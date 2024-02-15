package net.lubble.util.spec

import jakarta.persistence.criteria.*
import net.lubble.util.LID
import net.lubble.util.model.ParameterModel
import org.springframework.data.domain.Example
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

    /**
     * JPAModel interface defines the specifications for JPA models.
     */
    interface JPAModel<T> {
        /**
         * Returns the specification for search.
         */
        fun ofSearch(): Specification<T>

        /**
         * Returns the default predicates for a JPA model.
         *
         * @param root The root type in the from clause.
         * @param query The criteria query.
         * @param builder Used to construct criteria queries.
         * @param id The id of the entity.
         */
        fun defaultPredicates(
            root: Root<T>,
            query: CriteriaQuery<*>,
            builder: CriteriaBuilder,
            id: String? = null,
        ): Predicate {
            var predicate = builder.conjunction()

            predicate = builder.and(predicate, builder.isFalse(root.get("deleted")))
            predicate = builder.and(predicate, builder.isFalse(root.get("archived")))

            id?.let {
                val value = it.toLongOrNull() ?: LID.fromKey(it)
                val key = if (value is Long) "id" else "sk"
                return builder.and(predicate, builder.equal(root.get<Any>(key), value))
            }

            return predicate
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
            val key = if (value is Long) "id" else "sk"
            return builder.equal(join.get<Any>(key), value)
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
            return builder.and(predicate, builder.equal(root.type(), type))
        }

        /**
         * Returns the type predicate for a JPA model.
         *
         * @param builder Used to construct criteria queries.
         * @param root The root type in the from clause.
         * @param type The type of the entity.
         */
        fun <K> typePredicate(builder: CriteriaBuilder, root: Root<T>, type: Class<K>): Predicate {
            return builder.and(builder.equal(root.type(), type))
        }
    }

    /**
     * MongoModel interface defines the specifications for MongoDB models.
     */
    interface MongoModel<T> {
        /**
         * Returns the query for search.
         */
        fun ofSearch(): Query

        /**
         * Returns the example for a MongoDB model.
         */
        fun ofExample(): Example<T>

        /**
         * Returns the default query for a MongoDB model.
         *
         * @param id The id of the entity.
         */
        fun defaultQuery(id: String? = null): Query {
            val query = Query()
            query.addCriteria(Criteria.where("deleted").`is`(false))
            query.addCriteria(Criteria.where("archived").`is`(false))

            id?.let {
                val value = it.toLongOrNull() ?: LID.fromKey(it)
                val key = if (value is Long) "id" else "sk"
                query.addCriteria(Criteria.where(key).`is`(value))
            }

            return query
        }
    }
}