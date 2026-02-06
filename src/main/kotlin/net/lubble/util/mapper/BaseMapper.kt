package net.lubble.util.mapper

import net.lubble.util.MapperRegistryHolder
import net.lubble.util.dto.RBase
import net.lubble.util.model.BaseDocumented
import net.lubble.util.model.BaseModel
import org.springframework.data.domain.Page
import tools.jackson.databind.ObjectMapper

abstract class BaseProjectionMapper<T : BaseModel, V : BaseModel, R : RBase, U : Any> : BaseMapper<T, R, U> {

    abstract val mapper: ObjectMapper

    abstract fun pmapping(source: V): R

    fun pmap(source: V): R {
        MapperRegistryHolder.get<R>(source)?.let {
            return it
        }
        val dto = pmapping(source)
        apply(source, dto)
        return dto.also {
            MapperRegistryHolder.put(source, it)
        }
    }

    fun pmap(source: Collection<V>): List<R> = source.map(this::pmap)

    fun pmap(source: Page<V>): List<R> = source.content.map(this::pmap)
}

/**
 * BaseMapper interface is used for mapping between DTOs and Entities.
 * @param T Entity type
 * @param R Read DTO type
 * @param U Update DTO type
 */
interface BaseMapper<T : BaseModel, R : RBase, U : Any> {
    /**
     * Maps the properties of the update DTO (type U) to the Entity (type T).
     * @param source Source object
     * @param destination Destination object
     */
    fun map(source: U, destination: T) = objectMap(source, destination)

    /**
     * Custom function that maps the properties of the source object to the destination object.
     * Handles List, Map, and other class types.
     * @param source Source object
     * @param destination Destination object
     */
    @Suppress("UNCHECKED_CAST")
    private fun objectMap(source: Any, destination: Any) {
        source::class.java.declaredFields.forEach { sourceField ->
            destination::class.java.declaredFields.firstOrNull { it.name == sourceField.name }
                ?.let { destinationField ->
                    sourceField.isAccessible = true
                    destinationField.isAccessible = true

                    val sourceValue = sourceField.get(source)
                    val destinationValue = destinationField.get(destination)

                    if (sourceValue != null && destinationValue != null) {
                        when (sourceField.type) {
                            destinationField.type if sourceField.type.isAssignableFrom(List::class.java) -> {
                                // If the field is a list
                                val sourceList = sourceValue as List<*>
                                val destinationList = destinationValue as MutableList<Any>
                                destinationList.clear()
                                destinationList.addAll(sourceList.filterNotNull().map { it })
                            }

                            destinationField.type if sourceField.type.isAssignableFrom(Map::class.java) -> {
                                // If the field is a map
                                val sourceMap = sourceValue as Map<Any, Any>
                                val destinationMap = destinationValue as MutableMap<Any, Any>
                                destinationMap.clear()
                                destinationMap.putAll(sourceMap)
                            }

                            destinationField.type if sourceField.type.isAssignableFrom(
                                sourceValue::class.java
                            )
                                -> {
                                // If the field is another class
                                objectMap(sourceValue, destinationValue)
                            }

                            else -> {
                                destinationField.set(destination, sourceValue)
                            }
                        }
                    }
                }
        }
    }

    /**
     * Maps the properties of the Entity (type T) to a new DTO (type R).
     * @param source Source object
     * @return Newly created DTO
     */
    fun map(source: T): R {
        MapperRegistryHolder.get<R>(source)?.let {
            return it
        }
        val dto = mapping(source)
        apply(source, dto)
        return dto.also {
            MapperRegistryHolder.put(source, it)
        }
    }

    /**
     * Performs the conversion from Entity (type T) to DTO (type R).
     * @param source Source object
     * @return DTO object
     */
    fun mapping(source: T): R

    /**
     * Maps each object in the Entity collection (type T) to a DTO (type R).
     * @param source Source collection
     * @return List of DTO objects
     */
    fun map(source: Collection<T>): List<R> = source.map(this::map)

    /**
     * Maps each object in the Entity page (type T) to a DTO (type R).
     * @param source Source page
     * @return List of DTO objects
     */
    fun map(source: Page<T>): List<R> = source.content.map(this::map)

    /**
     * Maps the properties of the Documented Entity (type T) to a new Documented DTO (type R).
     * @param source Source object
     * @return Newly created Documented DTO
     */
    fun <D : BaseDocumented<T>> map(source: D): R {
        MapperRegistryHolder.get<R>(source)?.let {
            return it
        }
        val doc = mapping(source)
        apply(source, doc)
        return doc.also {
            MapperRegistryHolder.put(source, it)
        }
    }

    /**
     * Performs the conversion from Documented Entity (type T) to Documented DTO (type R).
     * @param source Source object
     * @return Documented DTO object
     */
    fun <D : BaseDocumented<T>> mapping(source: D): R {
        throw NotImplementedError("Always override dMapping function if you want to use dMap")
    }

    /**
     * Applies the basic properties of the Entity (type T) to the DTO (type R).
     * @param source Source object
     * @param target Target DTO
     */
    fun apply(source: BaseModel, target: R) {
        target.pk = source.pk
        target.sk = source.sk
        target.archived = source.archived
        target.deleted = source.deleted
        target.createdAt = source.createdAt
        target.updatedAt = source.updatedAt
    }
}

/**
 * Maps each object in the Documented Entity collection (type T) to a Documented DTO (type R).
 * @param source Source collection
 * @return List of Documented DTO objects
 */
fun <T : BaseModel, R : RBase, U : Any, D : BaseDocumented<T>>
        BaseMapper<T, R, U>.map(source: Collection<D>): List<R> =
    source.map { map(it) }

/**
 * Maps each object in the Documented Entity page (type T) to a Documented DTO (type R).
 * @param source Source page
 * @return List of Documented DTO objects
 */
fun <T : BaseModel, R : RBase, U : Any, D : BaseDocumented<T>>
        BaseMapper<T, R, U>.map(source: Page<D>): List<R> =
    source.content.map { map(it) }