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
    fun ofSearch(): CriteriaQuery


    /**
     * Returns the default query for an ElasticSearch model.
     *
     */
    fun defaultCriteria(
        param: ParameterModel
    ): Criteria {
        var criteria = Criteria()

        id?.let {
            criteria = idQuery(criteria, it)
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

    /**
     * Returns the id query for an ElasticSearch model.
     *
     * @param id The id of the entity.
     */
    fun idQuery(id: String): Criteria {
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return Criteria.where(key.name.lowercase()).`is`(value)
    }

    /**
     * Returns the id query for an ElasticSearch model.
     *
     * @param criteria The existing criteria to combine with.
     * @param id The id of the entity.
     */
    fun idQuery(criteria: Criteria, id: String): Criteria {
        return criteria.and(idQuery(id))
    }

    /**
     * Returns the id query for an ElasticSearch model.
     *
     * @param field The field to search in.
     * @param id The id of the entity.
     */
    fun idQuery(field: String, id: String): Criteria {
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return Criteria.where("$field.${key.name.lowercase()}").`is`(value)
    }

    /**
     * Returns the id query for an ElasticSearch model.
     *
     * @param criteria The existing criteria to combine with.
     * @param field The field to search in.
     * @param id The id of the entity.
     */
    fun idQuery(criteria: Criteria, field: String, id: String): Criteria {
        return criteria.and(idQuery(field, id))
    }
}