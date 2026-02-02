package net.lubble.util.spec.tool

import net.lubble.util.LK
import net.lubble.util.model.BaseModel
import net.lubble.util.model.ParameterModel
import net.lubble.util.spec.tool.SpecTool.IDType
import net.lubble.util.spec.tool.SpecTool.SearchType
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.util.concurrent.ConcurrentHashMap
// Reflection Field ile Mongo Field karışmaması için alias kullanıyoruz
import java.lang.reflect.Field as ReflectField

/**
 * MongoTool interface defines the specifications for MongoDB models.
 */
interface MongoTool<T : BaseModel> {

    companion object {
        private val fieldCache = ConcurrentHashMap<Class<*>, Array<ReflectField>>()

        private fun getCachedFields(clazz: Class<*>): Array<ReflectField> {
            return fieldCache.getOrPut(clazz) { clazz.declaredFields }
        }
    }

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
    fun ofSearch(): Query

    /**
     * Returns the default query for a MongoDB model.
     * Updated: Defaults to sorting by 'pk' DESC if no valid sort parameters are provided.
     */
    fun defaultQuery(
        param: ParameterModel
    ): Query {
        val query = Query()

        id?.let {
            idQuery(query, it)
        }

        ids?.let {
            idsQuery(query, it)
        }

        deleted?.let {
            query.addCriteria(Criteria.where("deleted").`is`(it))
        }

        archived?.let {
            query.addCriteria(Criteria.where("archived").`is`(it))
        }

        val fields = getCachedFields(clazz)
        val orders = mutableListOf<Sort.Order>()

        param.sortBy?.takeIf { it.isNotBlank() }?.let { sortByValue ->
            val sortFields = sortByValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val sortOrderValue = param.getSortOrderValue()
            val sortOrders = sortOrderValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            if (sortFields.isNotEmpty()) {
                sortFields.forEachIndexed { index, sortField ->
                    val isValid = fields.any { field ->
                        val columnValue = field.getAnnotation(Field::class.java)?.name
                        field.name == sortField || columnValue == sortField
                    }

                    if (isValid) {
                        val directionStr = if (index < sortOrders.size) sortOrders[index] else sortOrderValue

                        val direction = try {
                            Sort.Direction.valueOf(directionStr.uppercase())
                        } catch (e: IllegalArgumentException) {
                            Sort.Direction.ASC
                        }

                        orders.add(Sort.Order(direction, sortField))
                    }
                }
            }
        }

        if (orders.isNotEmpty()) {
            query.with(Sort.by(orders))
        } else {
            query.with(Sort.by(Sort.Direction.DESC, "pk"))
        }

        return query
    }

    private fun idKeyAndValue(id: String): Pair<IDType, Any> {
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return key to value
    }

    private fun applyIdCriteria(fieldPrefix: String?, id: String): Criteria {
        val (key, value) = idKeyAndValue(id)
        val field = if (fieldPrefix != null) "$fieldPrefix.${key.name.lowercase()}" else key.name.lowercase()
        return Criteria.where(field).`is`(value)
    }

    private fun mapIds(ids: List<String>): Pair<List<Long>, List<LK>> {
        val values = ids.map { id -> id.toLongOrNull() ?: LK(id) }
        val pkValues = values.filterIsInstance<Long>()
        val skValues = values.filterIsInstance<LK>()
        return pkValues to skValues
    }

    private fun buildIdsOrCriteria(fieldPrefix: String?, ids: List<String>): Criteria? {
        val (pkValues, skValues) = mapIds(ids)
        val pkField = if (fieldPrefix != null) "$fieldPrefix.pk" else "pk"
        val skField = if (fieldPrefix != null) "$fieldPrefix.sk" else "sk"
        val criteriaList = mutableListOf<Criteria>()
        if (pkValues.isNotEmpty()) criteriaList.add(Criteria.where(pkField).`in`(pkValues))
        if (skValues.isNotEmpty()) criteriaList.add(Criteria.where(skField).`in`(skValues))
        return if (criteriaList.isNotEmpty()) Criteria().orOperator(*criteriaList.toTypedArray()) else null
    }

    private fun buildSearchOrCriteria(search: String, type: SearchType, vararg fields: String): Criteria {
        val terms = search.split(" ")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (terms.isEmpty()) return Criteria()

        val termCriterias = terms.map { term ->
            val fieldCriterias = fields.map { field ->
                when (type) {
                    SearchType.EQUAL -> Criteria.where(field).`is`(term)
                    // Mongo regex aramalarında performans için dikkatli olunmalıdır.
                    SearchType.STARTS_WITH -> Criteria.where(field).regex("^$term.*", "i")
                    SearchType.ENDS_WITH -> Criteria.where(field).regex(".*$term$", "i")
                    SearchType.LIKE -> Criteria.where(field).regex(".*$term.*", "i")
                }
            }
            Criteria().orOperator(*fieldCriterias.toTypedArray())
        }

        return Criteria().andOperator(*termCriterias.toTypedArray())
    }
    // endregion

    // region Public Queries
    /**
     * Returns the id query for a MongoDB model.
     *
     * @param query The query to be combined.
     * @param id The id of the entity.
     */
    fun idQuery(query: Query, id: String): Query {
        return query.addCriteria(applyIdCriteria(null, id))
    }

    /**
     * Returns the id query for a MongoDB model.
     *
     * @param query The query to be combined.
     * @param field The field to search in.
     * @param id The id of the entity.
     */
    fun idQuery(query: Query, field: String, id: String): Query {
        return query.addCriteria(applyIdCriteria(field, id))
    }


    /**
     * Returns the ids query for a MongoDB model.
     *
     * @param query The query to be combined.
     * @param ids The list of ids of the entity.
     */
    fun idsQuery(query: Query, ids: List<String>): Query {
        val orCriteria = buildIdsOrCriteria(null, ids) ?: return query
        return query.addCriteria(orCriteria)
    }


    /**
     * Returns the ids query for a MongoDB model.
     *
     * @param query The query to be combined.
     * @param field The field to search in.
     * @param ids The list of ids of the entity.
     */
    fun idsQuery(query: Query, field: String, ids: List<String>): Query {
        val orCriteria = buildIdsOrCriteria(field, ids) ?: return query
        return query.addCriteria(orCriteria)
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
        val criteria = buildSearchOrCriteria(search, type, *fields)
        if (criteria != Criteria()) {
            query.addCriteria(criteria)
        }
        return query
    }
}