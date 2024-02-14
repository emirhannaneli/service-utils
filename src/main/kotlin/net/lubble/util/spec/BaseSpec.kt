package net.lubble.util.spec

import jakarta.persistence.criteria.*
import net.lubble.util.LID
import net.lubble.util.model.ParameterModel
import org.springframework.data.domain.Example
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

open class BaseSpec(private val base: ParameterModel) {

    fun ofPageable() = base.ofPageable()

    interface JPAModel<T> {
        fun ofSearch(): Specification<T>

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
                val value = id.toLongOrNull() ?: LID.fromKey(id)
                if (value is Long) {
                    return builder.and(predicate, builder.equal(root.get<Long>("id"), value))
                }
                return builder.and(predicate, builder.equal(root.get<ByteArray>("sk"), value))
            }

            return predicate
        }

        fun <Z, X> idPredicate(builder: CriteriaBuilder, join: Join<Z, X>, id: String): Predicate {
            val value = id.toLongOrNull() ?: LID.fromKey(id)
            if (value is Long) {
                return builder.equal(join.get<Long>("id"), value)
            }
            return builder.equal(join.get<ByteArray>("sk"), value)
        }
    }

    interface MongoModel<T> {
        fun ofSearch(): Query

        fun ofExample(): Example<T>

        fun defaultQuery(id: String? = null): Query {
            val query = Query()
            query.addCriteria(Criteria.where("deleted").`is`(false))
            query.addCriteria(Criteria.where("archived").`is`(false))

            id?.let {
                val value = id.toLongOrNull() ?: LID.fromKey(id)
                if (value is Long) {
                    query.addCriteria(Criteria.where("id").`is`(value))
                } else {
                    query.addCriteria(Criteria.where("sk").`is`(value))
                }
            }

            return query
        }
    }
}