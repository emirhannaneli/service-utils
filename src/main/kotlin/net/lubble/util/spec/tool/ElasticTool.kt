package net.lubble.util.spec.tool

import net.lubble.util.LK
import net.lubble.util.model.BaseModel
import net.lubble.util.model.ParameterModel
import net.lubble.util.model.SortOrder
import net.lubble.util.spec.tool.SpecTool.IDType
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery

/**
 * ElasticTool interface defines the specifications for ElasticSearch models.
 */
interface ElasticTool<T : BaseModel> {
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
    fun ofSearch(): Criteria


    /**
     * Returns the default query for an ElasticSearch model.
     *
     */
    fun defaultCriteria(
        param: ParameterModel
    ): Criteria {
        var criteria = Criteria()

        id?.let {
            criteria = idCriteria(criteria, it)
        }

        ids?.let {
            criteria = idsCriteria(criteria, it)
        }

        deleted?.let {
            criteria = criteria.and(Criteria.where("deleted").`is`(it))
        }

        archived?.let {
            criteria = criteria.and(Criteria.where("archived").`is`(it))
        }

        return criteria
    }

    /**
     * Builds the query with sorting for an ElasticSearch model.
     *
     * @param param The parameter model containing sorting information.
     * @param criteria The criteria to be combined.
     */
    fun buildQuery(param: ParameterModel, criteria: Criteria): CriteriaQuery {
        var query = CriteriaQuery(criteria)

        val fields = clazz.declaredFields
        when (param.sortOrder) {
            SortOrder.ASC -> param.sortBy?.let {
                if (fields.any { field ->
                        val columnValue = field.getAnnotation(Field::class.java)?.name
                        field.name == it || columnValue == it
                    }) query = CriteriaQuery(criteria).addSort(Sort.by(Sort.Order.asc(it)))
            }

            SortOrder.DESC -> param.sortBy?.let {
                if (fields.any { field ->
                        val columnValue = field.getAnnotation(Field::class.java)?.name
                        field.name == it || columnValue == it
                    }) query = CriteriaQuery(criteria).addSort(Sort.by(Sort.Order.desc(it)))
            }
        }

        return query
    }

    // region private helpers to eliminate duplication
    private fun idKeyAndValue(id: String): Pair<IDType, Any> {
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return key to value
    }

    private fun applyIdCriteria(id: String): Criteria {
        val (key, value) = idKeyAndValue(id)
        return Criteria.where(key.name.lowercase()).`is`(value)
    }

    private fun applyIdCriteria(field: String, id: String): Criteria {
        val (key, value) = idKeyAndValue(id)
        return Criteria.where("$field.${key.name.lowercase()}").`is`(value)
    }

    private fun partitionIds(ids: List<String>): Pair<List<Long>, List<Any>> {
        val pkValues: List<Long> = ids.mapNotNull { it.toLongOrNull() }
        val skValues = mutableListOf<Any>()
        ids.forEach { id -> if (id.toLongOrNull() == null) skValues.add(LK(id)) }
        return pkValues to skValues
    }

    private fun buildIdsCriteria(fieldPrefix: String?, ids: List<String>): Criteria? {
        val (pkValues, skValues) = partitionIds(ids)
        val pkField = if (fieldPrefix != null) "$fieldPrefix.pk" else "pk"
        val skField = if (fieldPrefix != null) "$fieldPrefix.sk" else "sk"
        val hasPk = pkValues.isNotEmpty()
        val hasSk = skValues.isNotEmpty()
        if (!hasPk && !hasSk) return null
        if (hasPk && hasSk) {
            return Criteria().or(Criteria.where(pkField).`in`(pkValues)).or(Criteria.where(skField).`in`(skValues))
        }
        return if (hasPk) Criteria.where(pkField).`in`(pkValues) else Criteria.where(skField).`in`(skValues)
    }
    // endregion

    /**
     * Returns the id criteria for an ElasticSearch model.
     *
     * @param id The id of the entity.
     */
    fun idCriteria(id: String): Criteria {
        return applyIdCriteria(id)
    }

    /**
     * Returns the id criteria for an ElasticSearch model.
     *
     * @param criteria The existing criteria to combine with.
     * @param id The id of the entity.
     */
    fun idCriteria(criteria: Criteria, id: String): Criteria {
        return criteria.and(idCriteria(id))
    }

    /**
     * Returns the id criteria for an ElasticSearch model.
     *
     * @param field The field to search in.
     * @param id The id of the entity.
     */
    fun idCriteria(field: String, id: String): Criteria {
        return applyIdCriteria(field, id)
    }

    /**
     * Returns the id criteria for an ElasticSearch model.
     *
     * @param criteria The existing criteria to combine with.
     * @param field The field to search in.
     * @param id The id of the entity.
     */
    fun idCriteria(criteria: Criteria, field: String, id: String): Criteria {
        return criteria.and(idCriteria(field, id))
    }

    /**
     * Returns the ids criteria for an ElasticSearch model.
     *
     * @param criteria The existing criteria to combine with.
     * @param ids The list of ids of the entity.
     */
    fun idsCriteria(criteria: Criteria, ids: List<String>): Criteria {
        val orCriteria = buildIdsCriteria(null, ids) ?: return criteria
        return criteria.and(orCriteria)
    }

    /**
     * Returns the ids criteria for an ElasticSearch model.
     *
     * @param criteria The existing criteria to combine with.
     * @param field The nested field to search in.
     * @param ids The list of ids of the entity.
     */
    fun idsCriteria(criteria: Criteria, field: String, ids: List<String>): Criteria {
        val orCriteria = buildIdsCriteria(field, ids) ?: return criteria
        return criteria.and(orCriteria)
    }
}