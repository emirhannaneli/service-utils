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

        query.distinct(true)

        return predicate
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
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return if (key == IDType.PK) builder.and(
            predicate,
            builder.equal(root.get<Long>(key.name.lowercase()), value)
        )
        else builder.and(predicate, builder.equal(root.get<String>(key.name.lowercase()), value))
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
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return if (key == IDType.PK) builder.and(
            predicate,
            builder.equal(join.get<Long>(key.name.lowercase()), value)
        )
        else builder.and(predicate, builder.equal(join.get<String>(key.name.lowercase()), value))
    }

    /**
     * Returns the id predicate for a JPA model.
     *
     * @param builder Used to construct criteria queries.
     * @param join The join type.
     * @param id The id of the entity.
     */
    fun <Z, X> idPredicate(builder: CriteriaBuilder, join: Join<Z, X>, id: String): Predicate {
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return if (key == IDType.PK) builder.equal(join.get<Long>(key.name.lowercase()), value)
        else builder.equal(join.get<String>(key.name.lowercase()), value)
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
        val terms = search.split(" ").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.map { term ->
            builder.or(
                *fields.map { field ->
                    when (type) {
                        SearchType.EQUAL -> builder.equal(builder.lower(root.get(field)), term)
                        SearchType.STARTS_WITH -> builder.like(builder.lower(root.get(field)), "$term%")
                        SearchType.ENDS_WITH -> builder.like(builder.lower(root.get(field)), "%$term")
                        SearchType.LIKE -> builder.like(builder.lower(root.get(field)), "%$term%")
                    }
                }.toTypedArray()
            )
        }

        return builder.or(*terms.toTypedArray())
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
     * @param predicate The predicate to be combined.
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
        val terms = search.split(" ").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.map { term ->
            builder.or(
                *fields.map { field ->
                    when (type) {
                        SearchType.EQUAL -> builder.equal(builder.lower(join.get(field)), term)
                        SearchType.STARTS_WITH -> builder.like(builder.lower(join.get(field)), "$term%")
                        SearchType.ENDS_WITH -> builder.like(builder.lower(join.get(field)), "%$term")
                        SearchType.LIKE -> builder.like(builder.lower(join.get(field)), "%$term%")
                    }
                }.toTypedArray()
            )
        }

        return builder.or(*terms.toTypedArray())
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