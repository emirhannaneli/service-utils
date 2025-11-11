package net.lubble.util.spec.tool

import jakarta.persistence.Column
import jakarta.persistence.criteria.*
import net.lubble.util.LK
import net.lubble.util.model.ParameterModel
import net.lubble.util.model.SortOrder
import net.lubble.util.spec.tool.SpecTool.IDType
import net.lubble.util.spec.tool.SpecTool.SearchType
import org.springframework.data.jpa.domain.Specification

/**
 * JPATool interface defines the specifications for JPA models.
 */
interface JPATool<T> {
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

        val fields = root.model.javaType.declaredFields
        when (param.sortOrder) {
            SortOrder.ASC -> param.sortBy?.let {
                if (fields.any { field ->
                        val columnValue = field.getAnnotation(Column::class.java)?.name
                        field.name == it || columnValue == it
                    }) query?.orderBy(builder.asc(root.get<Any>(it)))
            }

            SortOrder.DESC -> param.sortBy?.let {
                if (fields.any { field ->
                        val columnValue = field.getAnnotation(Column::class.java)?.name
                        field.name == it || columnValue == it
                    }) query?.orderBy(builder.desc(root.get<Any>(it)))
            }
        }

        query?.distinct(true)

        return predicate
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
        return if (key == IDType.PK) builder.equal(path.get<Long>(key.name.lowercase()), value as Long)
        else builder.equal(path.get<String>(key.name.lowercase()), value as LK)
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
        val pkValues = ids.mapNotNull { it.toLongOrNull() }
        val skValues = ids.mapNotNull { id -> if (id.toLongOrNull() == null) LK(id) else null }
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
            predicates.add(path.get<Long>(IDType.PK.name.lowercase()).`in`(pkValues))
        }
        if (skValues.isNotEmpty()) {
            predicates.add(path.get<String>(IDType.SK.name.lowercase()).`in`(skValues))
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
        val terms = search.split(" ")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .map { term ->
                builder.or(
                    *fields.map { field ->
                        val expr = getter(field)
                        when (type) {
                            SearchType.EQUAL -> builder.equal(builder.lower(expr), term)
                            SearchType.STARTS_WITH -> builder.like(builder.lower(expr), "$term%")
                            SearchType.ENDS_WITH -> builder.like(builder.lower(expr), "%$term")
                            SearchType.LIKE -> builder.like(builder.lower(expr), "%$term%")
                        }
                    }.toTypedArray()
                )
            }

        return builder.or(*terms.toTypedArray())
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