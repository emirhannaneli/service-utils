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
    fun defaultQuery(
        param: ParameterModel
    ): CriteriaQuery {
        val criteria = Criteria()

        id?.let {
            criteria.and(idQuery(criteria, it))
        }

        deleted?.let {
            criteria.and(Criteria.where("deleted").`is`(it))
        }

        archived?.let {
            criteria.and(Criteria.where("archived").`is`(it))
        }

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
     * @param criteria The criteria to be combined.
     * @param id The id of the entity.
     */
    fun idQuery(criteria: Criteria, id: String): Criteria {
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return when (key) {
            IDType.PK -> Criteria.where(key.name.lowercase()).`is`(value)
            IDType.SK -> Criteria.where(key.name.lowercase()).`is`(value)
        }
    }

    /**
     * Returns the id query for an ElasticSearch model.
     *
     * @param criteria The criteria to be combined.
     * @param field The field to search in.
     * @param id The id of the entity.
     */
    fun idQuery(criteria: Criteria, field: String, id: String): Criteria {
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return when (key) {
            IDType.PK -> Criteria.where("$field.${key.name.lowercase()}").`is`(value)
            IDType.SK -> Criteria.where("$field.${key.name.lowercase()}").`is`(value)
        }
    }
}