package net.lubble.util.projection

import jakarta.persistence.*
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Root
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
        validateEntity(clazz)
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
        validateEntity(clazz)

        // Eğer projection (field seçimi) istenmişse, özel işlem yap
        if (!spec.fields.isNullOrEmpty()) {
            return fetchWithProjection(spec, clazz, pagination)
        }

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
            
            // Sort - nested path desteği ile
            val pageable = spec.ofSortedPageable()
            if (pageable.sort.isSorted) {
                val joinMapForSort = mutableMapOf<String, Join<*, *>>()
                val orders = pageable.sort.map { order ->
                    val path = getOrCreatePath(root, order.property, joinMapForSort)
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

            // Sort (Pagination olmadığında da sıralamayı uygula) - nested path desteği ile
            val pageable = spec.ofSortedPageable()
            if (pageable.sort.isSorted) {
                val orders = pageable.sort.map { order ->
                    val path = getOrCreatePath(root, order.property)
                    if (order.isAscending) cb.asc(path) else cb.desc(path)
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

    private fun validateEntity(clazz: Class<*>): Unit {
        // Entity kontrolü: BaseModel bir @MappedSuperclass olduğu için entity değil
        // Eğer clazz BaseModel ise, bu bir hata durumudur
        if (clazz == BaseModel::class.java || !isEntity(clazz)) {
            throw IllegalArgumentException(
                "Not an entity: ${clazz.name}. " +
                "BaseModel is a @MappedSuperclass and cannot be used directly in queries. " +
                "Please ensure your specification uses a concrete entity class that extends BaseModel."
            )
        }
    }
    
    private fun count(spec: BaseSpec.JPA<T>, clazz: Class<T>): CriteriaQuery<Long> {
        validateEntity(clazz)
        val builder = manager.criteriaBuilder
        val query = builder.createQuery(Long::class.java)
        val root = query.from(clazz)
        val search = spec.ofSearch().toPredicate(root, query, builder)
        // Distinct count - ID üzerinden sayma daha performanslı olabilir
        query.select(builder.countDistinct(root))
            .where(search)
        return query
    }
    
    private fun isEntity(clazz: Class<*>): Boolean {
        // @Entity annotation'ı kontrolü
        if (clazz.isAnnotationPresent(Entity::class.java)) {
            return true
        }
        
        // EntityManager'ın Metamodel'ini kullanarak kontrol
        return try {
            manager.metamodel.managedType(clazz)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun fetchWithProjection(spec: BaseSpec.JPA<T>, clazz: Class<T>, pagination: Boolean): Page<T> {
        val cb = manager.criteriaBuilder

        // --- 1. COUNT QUERY (Değişiklik yok) ---
        var totalCount: Long = 0
        if (pagination) {
            val countQuery = manager.createQuery(count(spec, clazz))
            totalCount = countQuery.singleResult
            if (totalCount == 0L) return PageImpl(emptyList(), spec.ofSortedPageable(), 0L)
        }

        // --- 2. TUPLE QUERY HAZIRLIĞI ---
        val tupleQuery = cb.createTupleQuery()
        val root = tupleQuery.from(clazz)

        // Join'leri tekrar tekrar yapmamak için cache (Örn: hem user.name hem user.email istenirse user tablosuna 1 kere join atılır)
        val joinMap = mutableMapOf<String, Join<*, *>>()

        val requestedFields = spec.fields ?: emptyList()
        val selections = mutableListOf<Selection<*>>()

        // ID'yi her zaman ekle (Mapping için referans noktası)
        selections.add(root.get<Any>("id").alias("id"))

        requestedFields.forEach { fieldPath ->
            try {
                // "category.name" -> Path çözümlemesi ve Join işlemleri
                val path = getOrCreatePath(root, fieldPath, joinMap)
                selections.add(path.alias(fieldPath))
            } catch (e: Exception) {
                // Field bulunamazsa loglanabilir, şimdilik yutuyoruz.
            }
        }

        tupleQuery.multiselect(selections)

        // Search Filters
        val predicate = spec.ofSearch().toPredicate(root, tupleQuery, cb)
        tupleQuery.where(predicate)

        // Sorting
        val pageable = spec.ofSortedPageable()
        if (pageable.sort.isSorted) {
            val orders = pageable.sort.map { order ->
                // Sort alanları için de path çözümlemesi gerekebilir
                val path = getOrCreatePath(root, order.property, joinMap)
                if (order.isAscending) cb.asc(path) else cb.desc(path)
            }.toList()
            tupleQuery.orderBy(orders)
        }

        val typedQuery = manager.createQuery(tupleQuery)

        if (pagination) {
            typedQuery.firstResult = pageable.pageNumber * pageable.pageSize
            typedQuery.maxResults = pageable.pageSize
        }

        val tuples = typedQuery.resultList

        // --- 3. MAPPING (Flat Tuple -> Nested Object) ---
        val results = tuples.map { tuple ->
            val entity = clazz.getDeclaredConstructor().newInstance()

            tuple.elements.forEach { tupleElement ->
                val fieldPath = tupleElement.alias // örn: "category.name"
                val value = tuple.get(fieldPath)

                if (fieldPath != null && value != null) {
                    try {
                        writeNestedField(entity, fieldPath, value)
                    } catch (e: Exception) {
                        e.printStackTrace() // Detaylı hata takibi için
                    }
                }
            }
            entity
        }

        if (!pagination) totalCount = results.size.toLong()

        return PageImpl(results, pageable, totalCount)
    }

    /**
     * Verilen field path (örn: "category.subCategory.name") için gerekli Join'leri yapar
     * ve son path'i döner. JoinMap kullanarak mükerrer join'leri engeller.
     */
    private fun getOrCreatePath(
        root: Root<*>,
        fieldPath: String,
        joinMap: MutableMap<String, Join<*, *>>
    ): jakarta.persistence.criteria.Path<Any> {
        if (!fieldPath.contains(".")) {
            return root.get(fieldPath)
        }

        val parts = fieldPath.split(".")
        var currentFrom: From<*, *> = root
        var currentPathKey = ""

        // Son parça hariç (attribute) diğerleri ilişkidir (relation)
        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            currentPathKey = if (currentPathKey.isEmpty()) part else "$currentPathKey.$part"

            // Join varsa kullan, yoksa oluştur ve map'e at
            if (joinMap.containsKey(currentPathKey)) {
                currentFrom = joinMap[currentPathKey] as From<*, *>
            } else {
                // LEFT JOIN önemli: İlişki null ise (örn: category yoksa) ana kayıt gelmeye devam etsin.
                val join = currentFrom.join<Any, Any>(part, JoinType.LEFT)
                joinMap[currentPathKey] = join
                currentFrom = join
            }
        }

        // Son parça attribute'un kendisidir
        return currentFrom.get(parts.last())
    }

    /**
     * Verilen field path için gerekli Join'leri yapar (joinMap olmadan).
     * Mevcut join'leri kontrol ederek mükerrer join'leri engeller.
     */
    private fun getOrCreatePath(
        root: Root<*>,
        fieldPath: String
    ): jakarta.persistence.criteria.Path<Any> {
        if (!fieldPath.contains(".")) {
            return root.get(fieldPath)
        }

        val parts = fieldPath.split(".")
        var currentFrom: From<*, *> = root

        // Son parça hariç (attribute) diğerleri ilişkidir (relation)
        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            
            // Önce bu ilişki için zaten bir join var mı diye bakıyoruz
            val existingJoin = currentFrom.joins.firstOrNull { join ->
                join.attribute.name == part && join.joinType == JoinType.LEFT
            }

            if (existingJoin != null) {
                // Varsa onu kullan (Reuse) - mükerrer join'leri engelle
                @Suppress("UNCHECKED_CAST")
                currentFrom = existingJoin as From<*, *>
            } else {
                // Yoksa yeni LEFT JOIN oluştur
                // LEFT JOIN önemli: İlişki null ise (örn: product yoksa) ana kayıt gelmeye devam etsin
                currentFrom = currentFrom.join<Any, Any>(part, JoinType.LEFT)
            }
        }

        // Son parça attribute'un kendisidir
        return currentFrom.get(parts.last())
    }

    /**
     * Reflection ile iç içe objeleri oluşturur ve değeri set eder.
     * Örn: target=Product, path="category.name", value="Electronics"
     * Bu metod Product içinde Category instance'ı yoksa oluşturur, sonra name'i set eder.
     */
    private fun writeNestedField(target: Any, path: String, value: Any) {
        if (!path.contains(".")) {
            FieldUtils.writeField(target, path, value, true)
            return
        }

        val parts = path.split(".")
        var currentObject = target

        for (i in 0 until parts.size - 1) {
            val fieldName = parts[i]
            val field = FieldUtils.getField(currentObject.javaClass, fieldName, true)

            // Mevcut değeri oku
            var nestedObject = FieldUtils.readField(field, currentObject, true)

            // Eğer null ise yeni instance oluştur
            if (nestedObject == null) {
                val fieldType = field.type
                // Parametresiz constructor ile instance yarat (Hibernate entityleri buna sahip olmalı)
                nestedObject = fieldType.getDeclaredConstructor().newInstance()
                FieldUtils.writeField(field, currentObject, nestedObject, true)
            }
            currentObject = nestedObject
        }

        // Son alana değeri yaz
        FieldUtils.writeField(currentObject, parts.last(), value, true)
    }
}