package net.lubble.util.spec.tool

import jakarta.persistence.Column
import jakarta.persistence.criteria.*
import net.lubble.util.LK
import net.lubble.util.model.ParameterModel
import net.lubble.util.model.SortOrder
import net.lubble.util.spec.tool.SpecTool.IDType
import net.lubble.util.spec.tool.SpecTool.SearchType
import org.springframework.data.jpa.domain.Specification
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * JPATool interface defines the specifications for JPA models.
 */
interface JPATool<T> {
    companion object {
        // Reflection cache'leri - performans için
        private val fieldCache = ConcurrentHashMap<Class<*>, Array<Field>>()
        private val columnNameCache = ConcurrentHashMap<Field, String>()
        // lowercaseCache memory leak riski nedeniyle kaldırıldı.

        // Lowercase işlemi modern JVM'lerde yeterince hızlıdır.
        private fun normalizeString(str: String): String {
            return str.lowercase(Locale.ENGLISH)
        }
    }

    /**
     * The class of the entity.
     * */
    val clazz: Class<T>

    /**
     * The id of the entity.
     * */
    var id: String?

    /**
     * The list of ids of the entity.
     * */
    var ids: List<String>?

    /**
     * The deleted status of the entity.
     * */
    var deleted: Boolean?

    /**
     * The archived status of the entity.
     * */
    var archived: Boolean?

    /**
     * Returns the query for search.
     */
    fun ofSearch(): Specification<T>

    /**
     * Returns the default predicates for a JPA model.
     *
     * @param root The root type in the from clause.
     * @param query The criteria query.
     * @param builder Used to construct criteria queries.
     */
    fun defaultPredicates(
        root: Root<T>,
        query: CriteriaQuery<*>?,
        builder: CriteriaBuilder,
        param: ParameterModel,
    ): Predicate {
        var predicate = builder.conjunction()

        id?.let {
            predicate = idPredicate(predicate, root, builder, it)
        }

        ids?.let {
            predicate = idsPredicate(predicate, builder, root, it)
        }

        deleted?.let {
            predicate = builder.and(predicate, builder.equal(root.get<Any>("deleted"), deleted))
        }

        archived?.let {
            predicate = builder.and(predicate, builder.equal(root.get<Any>("archived"), archived))
        }

        // Field cache kullan - performans optimizasyonu
        val fields = getCachedFields(root.model.javaType)
        when (param.sortOrder) {
            SortOrder.ASC -> param.sortBy?.let { sortField ->
                if (isValidSortField(fields, sortField)) {
                    query?.orderBy(builder.asc(root.get<Any>(sortField)))
                }
            }

            SortOrder.DESC -> param.sortBy?.let { sortField ->
                if (isValidSortField(fields, sortField)) {
                    query?.orderBy(builder.desc(root.get<Any>(sortField)))
                }
            }
        }

        // query?.distinct(true) // Distinct performansı düşürebilir, gerektiğinde kullanılmalı

        return predicate
    }

    // Field cache - reflection performansı için
    private fun getCachedFields(clazz: Class<*>): Array<Field> {
        return Companion.fieldCache.getOrPut(clazz) {
            clazz.declaredFields
        }
    }

    // Column name cache - annotation performansı için
    private fun getCachedColumnName(field: Field): String? {
        val cachedName = columnNameCache.getOrPut(field) {
            field.getAnnotation(Column::class.java)?.name ?: ""
        }

        return cachedName.takeIf { it.isNotEmpty() }
    }

    // Sort field validation - optimize edilmiş
    private fun isValidSortField(fields: Array<Field>, sortField: String): Boolean {
        return fields.any { field ->
            field.name == sortField || getCachedColumnName(field) == sortField
        }
    }

    private fun idKeyAndValue(id: String): Pair<IDType, Any> {
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return key to value
    }

    private fun applyIdPredicate(
        builder: CriteriaBuilder,
        path: Path<*>,
        id: String,
    ): Predicate {
        val (key, value) = idKeyAndValue(id)
        val fieldName = Companion.normalizeString(key.name)
        return if (key == IDType.PK) {
            builder.equal(path.get<Long>(fieldName), value as Long)
        } else {
            builder.equal(path.get<String>(fieldName), value as LK)
        }
    }

    private fun applyIdPredicate(
        predicate: Predicate,
        builder: CriteriaBuilder,
        path: Path<*>,
        id: String,
    ): Predicate {
        return builder.and(predicate, applyIdPredicate(builder, path, id))
    }

    private fun partitionIds(ids: List<String>): Pair<List<Long>, List<LK>> {
        // Optimize edilmiş - her id için sadece bir kez toLongOrNull çağrılıyor
        val pkValues = mutableListOf<Long>()
        val skValues = mutableListOf<LK>()

        ids.forEach { id ->
            val longValue = id.toLongOrNull()
            if (longValue != null) {
                pkValues.add(longValue)
            } else {
                skValues.add(LK(id))
            }
        }

        return pkValues to skValues
    }

    private fun buildIdsOrPredicate(
        builder: CriteriaBuilder,
        path: Path<*>,
        ids: List<String>,
    ): Predicate? {
        val (pkValues, skValues) = partitionIds(ids)
        val predicates = mutableListOf<Predicate>()
        if (pkValues.isNotEmpty()) {
            predicates.add(path.get<Long>(Companion.normalizeString(IDType.PK.name)).`in`(pkValues))
        }
        if (skValues.isNotEmpty()) {
            predicates.add(path.get<String>(Companion.normalizeString(IDType.SK.name)).`in`(skValues))
        }
        return if (predicates.isNotEmpty()) builder.or(*predicates.toTypedArray()) else null
    }

    private fun applyIdsPredicate(
        predicate: Predicate,
        builder: CriteriaBuilder,
        path: Path<*>,
        ids: List<String>,
    ): Predicate {
        val orPredicate = buildIdsOrPredicate(builder, path, ids) ?: return predicate
        return builder.and(predicate, orPredicate)
    }

    private fun buildSearchPredicate(
        builder: CriteriaBuilder,
        getter: (String) -> Expression<String>,
        search: String,
        type: SearchType,
        vararg fields: String,
    ): Predicate {
        // Optimize edilmiş - string işlemleri önceden yapılıyor
        val terms = search.split(" ")
            .mapNotNull { it.trim().takeIf { it.isNotEmpty() } }
            .map { Companion.normalizeString(it) }
            .takeIf { it.isNotEmpty() }
            ?: return builder.conjunction() // Boş search için

        val termPredicates = terms.map { term ->
            val fieldPredicates = fields.map { field ->
                val expr = getter(field)
                when (type) {
                    SearchType.EQUAL -> builder.equal(builder.lower(expr), term)
                    SearchType.STARTS_WITH -> builder.like(builder.lower(expr), "$term%")
                    SearchType.ENDS_WITH -> builder.like(builder.lower(expr), "%$term")
                    SearchType.LIKE -> builder.like(builder.lower(expr), "%$term%")
                }
            }
            builder.or(*fieldPredicates.toTypedArray())
        }

        // Kelime grupları arasında AND kullanıyoruz (Ahmet AND Yılmaz)
        return builder.and(*termPredicates.toTypedArray())
    }

    /**
     * Returns the id predicate for a JPA model.
     *
     * @param predicate The predicate to be combined.
     * @param root The root type in the from clause.
     * @param builder Used to construct criteria queries.
     * @param id The id of the entity.
     */
    private fun idPredicate(
        predicate: Predicate,
        root: Root<T>,
        builder: CriteriaBuilder,
        id: String,
    ): Predicate {
        return applyIdPredicate(predicate, builder, root, id)
    }

    /**
     * Returns the id predicate for a JPA model.
     *
     * @param predicate The predicate to be combined.
     * @param builder Used to construct criteria queries.
     * @param join The join type.
     * @param id The id of the entity.
     */
    fun <Z, X> idPredicate(
        predicate: Predicate,
        builder: CriteriaBuilder,
        join: Join<Z, X>,
        id: String
    ): Predicate {
        return applyIdPredicate(predicate, builder, join, id)
    }

    /**
     * Returns the id predicate for a JPA model.
     *
     * @param builder Used to construct criteria queries.
     * @param join The join type.
     * @param id The id of the entity.
     */
    fun <Z, X> idPredicate(builder: CriteriaBuilder, join: Join<Z, X>, id: String): Predicate {
        return applyIdPredicate(builder, join, id)
    }

    /**
     * Returns the ids predicate for a JPA model.
     *
     * @param predicate The predicate to be combined.
     * @param builder Used to construct criteria queries.
     * @param join The join type.
     * @param ids The list of ids of the entity.
     */
    fun <Z, X> idsPredicate(
        predicate: Predicate,
        builder: CriteriaBuilder,
        join: Join<Z, X>,
        ids: List<String>
    ): Predicate {
        return applyIdsPredicate(predicate, builder, join, ids)
    }

    /**
     * Returns the ids predicate for a JPA model.
     *
     * @param predicate The predicate to be combined.
     * @param builder Used to construct criteria queries.
     * @param root The root type in the from clause.
     * @param ids The list of ids of the entity.
     */
    fun idsPredicate(
        predicate: Predicate,
        builder: CriteriaBuilder,
        root: Root<T>,
        ids: List<String>
    ): Predicate {
        return applyIdsPredicate(predicate, builder, root, ids)
    }

    /**
     * Returns the type predicate for a JPA model.
     *
     * @param predicate The predicate to be combined.
     * @param builder Used to construct criteria queries.
     * @param root The root type in the from clause.
     * @param type The type of the entity.
     */
    fun <K> typePredicate(
        predicate: Predicate,
        builder: CriteriaBuilder,
        root: Root<T>,
        type: Class<out K>
    ): Predicate {
        return builder.and(predicate, typePredicate(builder, root, type))
    }

    /**
     * Returns the type predicate for a JPA model.
     *
     * @param builder Used to construct criteria queries.
     * @param root The root type in the from clause.
     * @param type The type of the entity.
     */
    fun <K> typePredicate(builder: CriteriaBuilder, root: Root<T>, type: Class<out K>): Predicate {
        return builder.equal(root.type(), type)
    }

    /**
     * Returns the search predicate for a JPA model.
     *
     * @param builder Used to construct criteria queries.
     * @param root The root type in the from clause.
     * @param search The search string.
     * @param type The type of search to perform.
     * @param fields The fields to search for.
     */
    fun searchPredicate(
        builder: CriteriaBuilder,
        root: Root<T>,
        search: String,
        type: SearchType = SearchType.LIKE,
        vararg fields: String,
    ): Predicate {
        return buildSearchPredicate(builder, { f -> root.get<String>(f) }, search, type, *fields)
    }

    /**
     * Returns the search predicate for a JPA model.
     *
     * @param predicate The predicate to be combined.
     * @param builder Used to construct criteria queries.
     * @param root The root type in the from clause.
     * @param search The search string.
     * @param type The type of search to perform.
     * @param fields The fields to search for.
     */
    fun searchPredicate(
        predicate: Predicate,
        builder: CriteriaBuilder,
        root: Root<T>,
        search: String,
        type: SearchType = SearchType.LIKE,
        vararg fields: String,
    ): Predicate {
        return builder.and(predicate, searchPredicate(builder, root, search, type, *fields))
    }

    /**
     * Returns the search predicate for a JPA model.
     *
     * @param builder Used to construct criteria queries.
     * @param join The join type.
     * @param search The search string.
     * @param type The type of search to perform.
     * @param fields The fields to search for.
     */
    fun <Z, X> searchPredicate(
        builder: CriteriaBuilder,
        join: Join<Z, X>,
        search: String,
        type: SearchType = SearchType.LIKE,
        vararg fields: String,
    ): Predicate {
        return buildSearchPredicate(builder, { f -> join.get<String>(f) }, search, type, *fields)
    }

    /**
     * Returns the search predicate for a JPA model.
     *
     * @param predicate The predicate to be combined.
     * @param builder Used to construct criteria queries.
     * @param join The join type.
     * @param search The search string.
     * @param type The type of search to perform.
     * @param fields The fields to search for.
     */
    fun <Z, X> searchPredicate(
        predicate: Predicate,
        builder: CriteriaBuilder,
        join: Join<Z, X>,
        search: String,
        type: SearchType = SearchType.LIKE,
        vararg fields: String,
    ): Predicate {
        return builder.and(predicate, searchPredicate(builder, join, search, type, *fields))
    }
}