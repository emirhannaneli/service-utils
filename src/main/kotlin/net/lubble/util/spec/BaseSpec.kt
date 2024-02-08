package net.lubble.util.spec

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import net.lubble.util.LID
import net.lubble.util.model.ParameterModel
import org.springframework.data.domain.Example
import org.springframework.data.jpa.domain.Specification
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

            id?.let { value ->
                value.toLongOrNull()?.let {
                    return builder.and(predicate, builder.equal(root.get<Long>("id"), it))
                }
                return builder.and(predicate, builder.equal(root.get<ByteArray>("sk"), LID.fromKey(value)))
            }

            return predicate
        }
    }

    interface MongoModel<T> {
        fun ofSearch(): Query

        fun ofExample(): Example<T>
    }
}