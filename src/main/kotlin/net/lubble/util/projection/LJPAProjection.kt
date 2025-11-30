package net.lubble.util.projection

import jakarta.persistence.*
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Selection
import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.spec.BaseSpec
import org.apache.commons.lang3.reflect.FieldUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.util.*

interface LJPAProjection<T : BaseModel> {
    private val manager: EntityManager
        get() = AppContextUtil.bean(EntityManager::class.java)

    fun findOne(spec: BaseSpec.JPA<T>): Optional<T> {
        val clazz = spec.clazz
        val builder = manager.criteriaBuilder
        val query = builder.createQuery(clazz)
        val root = query.from(clazz)

        // Apply Predicates (Search)
        val predicate = spec.ofSearch().toPredicate(root, query, builder)
        query.where(predicate)

        // Entity Graph (Fetch Strategy)
        val entityGraph = createDynamicEntityGraph(spec, clazz)

        val typedQuery = manager.createQuery(query)
        typedQuery.maxResults = 1

        if (entityGraph.attributeNodes.isNotEmpty()) {
            typedQuery.setHint("jakarta.persistence.fetchgraph", entityGraph)
        }

        return typedQuery.resultList.firstOrNull()
            ?.let { Optional.of(it) }
            ?: Optional.empty()
    }

    fun findAll(spec: BaseSpec.JPA<T>, pagination: Boolean = true): Page<T> {
        val clazz = spec.clazz

        // 1. Count Query
        var totalCount: Long = 0
        if (pagination) {
            val countQuery = manager.createQuery(count(spec, clazz))
            totalCount = countQuery.singleResult
            if (totalCount == 0L) return PageImpl(emptyList(), spec.ofSortedPageable(), 0L)
        }

        // 2. ID Fetching (Pagination in Memory Prevention)
        val ids = if (pagination) {
            val cb = manager.criteriaBuilder
            
            // PostgreSQL "SELECT DISTINCT + ORDER BY" hatası için çözüm: Tuple Query kullanımı
            val idQuery = cb.createTupleQuery()
            val root = idQuery.from(clazz)
            
            // Search/Filter
            val predicate = spec.ofSearch().toPredicate(root, idQuery, cb)
            
            // JPATool.defaultPredicates içinde eklenen orderBy'ı temizle (Select listesinde olmadığı için hata verebilir)
            idQuery.orderBy(emptyList())
            
            val selections = mutableListOf<Selection<*>>(root.get<Any>("id").alias("id"))
            
            // Sort
            val pageable = spec.ofSortedPageable()
            if (pageable.sort.isSorted) {
                val orders = pageable.sort.map { order ->
                    val path = root.get<Any>(order.property)
                    selections.add(path) // Sort alanını da select'e ekle
                    if (order.isAscending) cb.asc(path) else cb.desc(path)
                }.toList()
                idQuery.orderBy(orders)
            }
            
            idQuery.where(predicate)
            idQuery.multiselect(selections)
            idQuery.distinct(true) // ID'ler için distinct önemli

            val typedIdQuery = manager.createQuery(idQuery)
            typedIdQuery.firstResult = pageable.pageNumber * pageable.pageSize
            typedIdQuery.maxResults = pageable.pageSize
            
            val fetchedTuples = typedIdQuery.resultList
            if (fetchedTuples.isEmpty()) return PageImpl(emptyList(), spec.ofSortedPageable(), 0L)
            
            // Tuple'dan sadece ID'yi al (ilk eleman)
            fetchedTuples.map { it.get(0) as String }
        } else {
            emptyList() // Pagination yoksa ID listesi boş, aşağıda kontrol edeceğiz
        }

        // 3. Entity Fetching (Data Loading)
        val cb = manager.criteriaBuilder
        val query = cb.createQuery(clazz)
        val root = query.from(clazz)

        if (pagination) {
            // ID listesine göre filtrele
            query.where(root.get<String>("id").`in`(ids))
            // Sıralama: IN clause sıralamayı garanti etmez, bu yüzden memory'de sıralayacağız veya
            // veritabanı spesifik FIELD() fonksiyonu gerekebilir ki bu standart JPA değil.
            // En temizi memory'de ID listesine göre sıralamak.
        } else {
            // Pagination yoksa tüm filtreleri uygula
            val predicate = spec.ofSearch().toPredicate(root, query, cb)
            query.where(predicate)
            query.distinct(true) // Join varsa distinct entity için

            // Sort (Pagination olmadığında da sıralamayı uygula)
            val pageable = spec.ofSortedPageable()
            if (pageable.sort.isSorted) {
                val orders = pageable.sort.map { order ->
                    if (order.isAscending) cb.asc(root.get<Any>(order.property))
                    else cb.desc(root.get<Any>(order.property))
                }.toList()
                query.orderBy(orders)
            }
        }

        val typedQuery = manager.createQuery(query)

        // Entity Graph ile N+1 önleme
        val entityGraph = createDynamicEntityGraph(spec, clazz)
        if (entityGraph.attributeNodes.isNotEmpty()) {
            typedQuery.setHint("jakarta.persistence.fetchgraph", entityGraph)
        }

        var results = typedQuery.resultList

        // Eğer pagination varsa ve ID listesi ile çekildiyse, sıralamayı koru
        if (pagination && ids.isNotEmpty()) {
            val entityMap = results.associateBy { it.getId() }
            results = ids.mapNotNull { entityMap[it] }
        }

        // Count işlemi (Eğer yukarıda yapılmadıysa - pagination=false durumu)
        if (!pagination) {
            totalCount = results.size.toLong()
        }

        return PageImpl(results, spec.ofSortedPageable(), totalCount)
    }

    private fun createDynamicEntityGraph(spec: BaseSpec.JPA<T>, clazz: Class<T>): EntityGraph<T> {
        val entityGraph = manager.createEntityGraph(clazz)
        val requestedFields =
            spec.fields?.toSet() ?: return entityGraph // Eğer field belirtilmediyse default graph (boş)

        // Sadece entity üzerinde tanımlı ve ilişki içeren alanları ekle
        // Field Exposure riski için: Sadece class'ta tanımlı alanlar kontrol ediliyor.
        // Hassas veriler @JsonIgnore ile korunmalı.

        requestedFields.forEach { fieldName ->
            // Field kontrolü (güvenlik ve varlık kontrolü)
            val field = FieldUtils.getField(clazz, fieldName, true) ?: return@forEach

            // Sadece ilişkisel alanları graph'a ekle (EAGER fetch için)
            // Basit alanlar zaten default olarak gelir.
            if (field.isAnnotationPresent(ManyToOne::class.java) ||
                field.isAnnotationPresent(OneToOne::class.java) ||
                field.isAnnotationPresent(OneToMany::class.java) ||
                field.isAnnotationPresent(ManyToMany::class.java)
            ) {

                // Nested path desteği yok, sadece direct attribute
                if (!entityGraph.attributeNodes.any { it.attributeName == fieldName }) {
                    entityGraph.addAttributeNodes(fieldName)
                }
            }
        }
        return entityGraph
    }

    private fun count(spec: BaseSpec.JPA<T>, clazz: Class<T>): CriteriaQuery<Long> {
        val builder = manager.criteriaBuilder
        val query = builder.createQuery(Long::class.java)
        val root = query.from(clazz)
        val search = spec.ofSearch().toPredicate(root, query, builder)
        // Distinct count - ID üzerinden sayma daha performanslı olabilir
        query.select(builder.countDistinct(root))
            .where(search)
        return query
    }
}