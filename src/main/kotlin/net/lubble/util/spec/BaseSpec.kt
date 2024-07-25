package net.lubble.util.spec

import jakarta.persistence.Column
import jakarta.persistence.criteria.*
import net.lubble.util.LID
import net.lubble.util.model.BaseJPAModel
import net.lubble.util.model.BaseMongoModel
import net.lubble.util.model.ParameterModel
import net.lubble.util.model.SortOrder
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

/**
 * BaseSpec class is used to define common specifications for JPA and MongoDB models.
 *
 * @property base The base parameter model.
 */
open class BaseSpec(private val base: ParameterModel) {
    /**
     * Returns the pageable object from the base parameter model.
     */
    fun ofPageable() = base.ofPageable()

    fun ofPageable(sort: Sort) = base.ofPageable(sort)

    fun ofSortedPageable(): Pageable {
        val sort = base.sortBy?.let { Sort.by(Sort.Direction.valueOf(base.sortOrder.value), it) } ?: Sort.unsorted()
        return base.ofPageable(sort)
    }

    /**
     * JPAModel interface defines the specifications for JPA models.
     */
    interface JPAModel<T> {
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
            query: CriteriaQuery<*>,
            builder: CriteriaBuilder,
            params: BaseJPAModel.SearchParams,
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
            when (params.sortOrder) {
                SortOrder.ASC -> params.sortBy?.let {
                    if (fields.any { field ->
                            val columnValue = field.getAnnotation(Column::class.java)?.name
                            field.name == it || columnValue == it
                        })
                        query.orderBy(builder.asc(root.get<Any>(it)))
                }

                SortOrder.DESC -> params.sortBy?.let {
                    if (fields.any { field ->
                            val columnValue = field.getAnnotation(Column::class.java)?.name
                            field.name == it || columnValue == it
                        })
                        query.orderBy(builder.desc(root.get<Any>(it)))
                }
            }

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
            val value = id.toLongOrNull() ?: LID.fromKey(id)
            val key = if (value is Long) IDType.PK else IDType.SK
            return if (key == IDType.PK) builder.and(predicate, builder.equal(root.get<Long>(key.name.lowercase()), value))
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
        fun <Z, X> idPredicate(predicate: Predicate, builder: CriteriaBuilder, join: Join<Z, X>, id: String): Predicate {
            val value = id.toLongOrNull() ?: LID.fromKey(id)
            val key = if (value is Long) IDType.PK else IDType.SK
            return if (key == IDType.PK) builder.and(predicate, builder.equal(join.get<Long>(key.name.lowercase()), value))
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
            val value = id.toLongOrNull() ?: LID.fromKey(id)
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
        fun <K> typePredicate(predicate: Predicate, builder: CriteriaBuilder, root: Root<T>, type: Class<K>): Predicate {
            return builder.and(predicate, typePredicate(builder, root, type))
        }

        /**
         * Returns the type predicate for a JPA model.
         *
         * @param builder Used to construct criteria queries.
         * @param root The root type in the from clause.
         * @param type The type of the entity.
         */
        fun <K> typePredicate(builder: CriteriaBuilder, root: Root<T>, type: Class<K>): Predicate {
            return builder.equal(root.type(), type)
        }

        /**
         * Returns the search predicate for a JPA model.
         *
         * @param predicate The predicate to be combined.
         * @param builder Used to construct criteria queries.
         * @param root The root type in the from clause.
         * @param search The search string.
         * @param fields The fields to search for.
         */
        fun searchPredicate(
            predicate: Predicate,
            builder: CriteriaBuilder,
            root: Root<T>,
            search: String,
            vararg fields: String
        ): Predicate {
            val terms = search.split(" ")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .map { term ->
                    builder.or(
                        *fields.map { field -> builder.like(builder.lower(root.get(field)), "%$term%") }.toTypedArray()
                    )
                }

            return builder.and(predicate, builder.or(*terms.toTypedArray()))
        }
    }

    /**
     * MongoModel interface defines the specifications for MongoDB models.
     */
    interface MongoModel<T> {
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
        fun ofSearch(): Query

        /**
         * Returns the default query for a MongoDB model.
         *
         */
        fun defaultQuery(
            params: BaseMongoModel.SearchParams
        ): Query {
            val query = Query()

            id?.let {
                idQuery(query, it)
            }

            deleted?.let {
                query.addCriteria(Criteria.where("deleted").`is`(it))
            }

            archived?.let {
                query.addCriteria(Criteria.where("archived").`is`(it))
            }

            when (params.sortOrder) {
                SortOrder.ASC -> params.sortBy?.let {
                    query.with(Sort.by(Sort.Order.asc(it)))
                }

                SortOrder.DESC -> params.sortBy?.let {
                    query.with(Sort.by(Sort.Order.desc(it)))
                }
            }

            return query
        }

        /**
         * Returns the id query for a MongoDB model.
         *
         * @param query The query to be combined.
         * @param id The id of the entity.
         */
        fun idQuery(query: Query, id: String): Query {
            val value = id.toLongOrNull() ?: LID.fromKey(id)
            val key = if (value is Long) IDType.PK else IDType.SK
            return when (key) {
                IDType.PK -> query.addCriteria(Criteria.where(key.name.lowercase()).`is`(value))
                IDType.SK -> query.addCriteria(Criteria.where(key.name.lowercase()).`is`(value))
            }
        }

        /**
         * Returns the type query for a MongoDB model.
         *
         * @param query The query to be combined.
         * @param type The type of the entity.
         */
        fun <K> typeQuery(query: Query, type: Class<K>): Query {
            query.addCriteria(Criteria.where("class").`is`(type.name))
            return query
        }

        /**
         * Returns the type query for a MongoDB model.
         *
         * @param type The type of the entity.
         */
        fun <K> typeQuery(type: Class<K>): Query {
            val query = Query()
            query.addCriteria(Criteria.where("class").`is`(type.name))
            return query
        }

        /**
         * Returns the search query for a MongoDB model.
         *
         * @param query The query to be combined.
         * @param search The search string.
         * @param fields The fields to search for.
         */
        fun searchQuery(query: Query, search: String, vararg fields: String): Query {
            val terms = search.split(" ")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .map { term ->
                    Criteria().orOperator(
                        *fields.map { Criteria.where(it).regex(".*$term.*", "i") }.toTypedArray()
                    )
                }
            query.addCriteria(Criteria().orOperator(*terms.toTypedArray()))
            return query
        }
    }

    /**
     * IDType enum class defines the types of ids.
     * */
    private enum class IDType {
        PK,
        SK
    }
}