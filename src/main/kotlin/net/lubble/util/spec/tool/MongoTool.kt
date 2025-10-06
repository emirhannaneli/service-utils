package net.lubble.util.spec.tool

import net.lubble.util.LK
import net.lubble.util.model.BaseModel
import net.lubble.util.model.ParameterModel
import net.lubble.util.model.SortOrder
import net.lubble.util.spec.tool.SpecTool.IDType
import net.lubble.util.spec.tool.SpecTool.SearchType
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

/**
 * MongoTool interface defines the specifications for MongoDB models.
 */
interface MongoTool<T : BaseModel> {
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
    fun ofSearch(): Query

    /**
     * Returns the default query for a MongoDB model.
     *
     */
    fun defaultQuery(
        param: ParameterModel
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

        val fields = clazz.declaredFields
        when (param.sortOrder) {
            SortOrder.ASC -> param.sortBy?.let {
                if (fields.any { field ->
                        val columnValue = field.getAnnotation(Field::class.java)?.name
                        field.name == it || columnValue == it
                    }) query.with(Sort.by(Sort.Direction.ASC, it))
            }

            SortOrder.DESC -> param.sortBy?.let {
                if (fields.any { field ->
                        val columnValue = field.getAnnotation(Field::class.java)?.name
                        field.name == it || columnValue == it
                    }) query.with(Sort.by(Sort.Direction.DESC, it))
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
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return when (key) {
            IDType.PK -> query.addCriteria(Criteria.where(key.name.lowercase()).`is`(value))
            IDType.SK -> query.addCriteria(Criteria.where(key.name.lowercase()).`is`(value))
        }
    }

    /**
     * Returns the id query for a MongoDB model.
     *
     * @param query The query to be combined.
     * @param field The field to search in.
     * @param id The id of the entity.
     */
    fun idQuery(query: Query, field: String, id: String): Query {
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return when (key) {
            IDType.PK -> query.addCriteria(Criteria.where("$field.${key.name.lowercase()}").`is`(value))
            IDType.SK -> query.addCriteria(Criteria.where("$field.${key.name.lowercase()}").`is`(value))
        }
    }

    /**
     * Returns the type query for a MongoDB model.
     *
     * @param query The query to be combined.
     * @param type The type of the entity.
     */
    fun <K> typeQuery(query: Query, type: Class<K>): Query {
        query.addCriteria(Criteria.where("_class").`is`(type.name))
        return query
    }

    /**
     * Returns the type query for a MongoDB model.
     *
     * @param type The type of the entity.
     */
    fun <K> typeQuery(type: Class<K>): Query {
        val query = Query()
        query.addCriteria(Criteria.where("_class").`is`(type.name))
        return query
    }

    /**
     * Returns the search query for a MongoDB model.
     *
     * @param query The query to be combined.
     * @param search The search string.
     * @param fields The fields to search for.
     */
    fun searchQuery(
        query: Query,
        search: String,
        vararg fields: String,
        type: SearchType = SearchType.LIKE
    ): Query {
        val terms = search.split(" ").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.map { term ->
            Criteria().orOperator(
                *fields.map {
                    when (type) {
                        SearchType.EQUAL -> Criteria.where(it).`is`(term)
                        SearchType.STARTS_WITH -> Criteria.where(it).regex("^$term.*", "i")
                        SearchType.ENDS_WITH -> Criteria.where(it).regex(".*$term$", "i")
                        SearchType.LIKE -> Criteria.where(it).regex(".*$term.*", "i")
                    }
                }.toTypedArray()
            )
        }
        query.addCriteria(Criteria().orOperator(*terms.toTypedArray()))
        return query
    }
}