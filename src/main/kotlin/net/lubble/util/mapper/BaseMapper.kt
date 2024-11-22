package net.lubble.util.mapper

import org.springframework.data.domain.Page

/**
 * BaseMapper interface for mapping between DTOs and Entities.
 * @param T Entity
 * @param R Read DTO
 * @param RB Read Basic DTO
 * @param U Update DTO
 *
 * @property map(source: U, destination: T) - maps the update DTO to the entity
 */
interface BaseMapper<T : Any, R, RB, U : Any> {

    /**
     * Maps the properties of the source object (of type U) to the destination object (of type T).
     * Only the properties with the same name and compatible types in both source and destination are mapped.
     *
     * @param source The source object to map from.
     * @param destination The destination object to map to.
     */
    fun map(source: U, destination: T) {
        objectMap(source, destination)
    }

    /**
     * Private function to map the properties of the source object to the destination object.
     * This function is used internally by the public map functions.
     * It handles the mapping of fields of type List, Map and other classes.
     *
     * @param source The source object to map from.
     * @param destination The destination object to map to.
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
     * Maps the properties of the source object (of type T) to a new object of type R.
     *
     * @param source The source object to map from.
     * @return The new object of type R.
     */
    fun map(source: T): R

    /**
     * Maps the properties of the source object (of type T) to a new object of type RB.
     *
     * @param source The source object to map from.
     * @return The new object of type RB.
     */
    fun rbMap(source: T): RB

    /**
     * Maps the properties of each object in the source collection (of type T) to a new object of type R.
     * Returns a list of these new objects.
     *
     * @param source The collection of source objects to map from.
     * @return The list of new objects of type R.
     */
    fun map(source: Collection<T>): List<R> {
        return source.map { map(it) }
    }

    /**
     * Maps the properties of each object in the source page (of type T) to a new object of type R.
     * Returns a list of these new objects.
     *
     * @param source The page of source objects to map from.
     * @return The list of new objects of type R.
     */
    fun map(source: Page<T>): List<R> {
        return source.content.map { map(it) }
    }

    /**
     * Maps the properties of each object in the source collection (of type T) to a new object of type RB.
     * Returns a list of these new objects.
     *
     * @param source The collection of source objects to map from.
     * @return The list of new objects of type RB.
     */
    fun rbMap(source: Collection<T>): List<RB> {
        return source.map { rbMap(it) }
    }

    /**
     * Maps the properties of each object in the source page (of type T) to a new object of type RB.
     * Returns a list of these new objects.
     *
     * @param source The page of source objects to map from.
     * @return The list of new objects of type RB.
     */
    fun rbMap(source: Page<T>): List<RB> {
        return source.content.map { rbMap(it) }
    }
}