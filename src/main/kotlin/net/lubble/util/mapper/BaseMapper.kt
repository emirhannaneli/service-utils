package net.lubble.util.mapper

import net.lubble.util.dto.RBase
import net.lubble.util.model.BaseModel
import org.springframework.data.domain.Page

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
    fun map(source: U, destination: T) {
        objectMap(source, destination)
    }

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
                        if (sourceField.type == destinationField.type && sourceField.type.isAssignableFrom(List::class.java)) {
                            // If the field is a list
                            val sourceList = sourceValue as List<*>
                            val destinationList = destinationValue as MutableList<Any>
                            destinationList.clear()
                            destinationList.addAll(sourceList.filterNotNull().map { it })
                        } else if (sourceField.type == destinationField.type && sourceField.type.isAssignableFrom(Map::class.java)) {
                            // If the field is a map
                            val sourceMap = sourceValue as Map<Any, Any>
                            val destinationMap = destinationValue as MutableMap<Any, Any>
                            destinationMap.clear()
                            destinationMap.putAll(sourceMap)
                        } else if (sourceField.type == destinationField.type && sourceField.type.isAssignableFrom(
                                sourceValue::class.java
                            )
                        ) {
                            // If the field is another class
                            objectMap(sourceValue, destinationValue)
                        } else {
                            destinationField.set(destination, sourceValue)
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
        val dto = mapping(source)
        apply(source, dto)
        return dto
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
    fun map(source: Collection<T>): List<R> {
        return source.map { map(it) }
    }

    /**
     * Maps each object in the Entity page (type T) to a DTO (type R).
     * @param source Source page
     * @return List of DTO objects
     */
    fun map(source: Page<T>): List<R> {
        return source.content.map { map(it) }
    }

    /**
     * Applies the basic properties of the Entity (type T) to the DTO (type R).
     * @param source Source object
     * @param target Target DTO
     */
    fun apply(source: BaseModel, target: R) {
        target.pk = source.pk
        target.sk = source.sk
        target.createdAt = source.createdAt
        target.updatedAt = source.updatedAt
    }
}