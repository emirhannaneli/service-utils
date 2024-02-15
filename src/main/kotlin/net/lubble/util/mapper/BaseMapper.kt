package net.lubble.util.mapper

/**
 * BaseMapper interface for mapping between DTOs and Entities.
 * @param T Entity
 * @param R Read DTO
 * @param RB Read Basic DTO
 * @param U Update DTO
 *
 * @property map(source: U, destination: T) - maps the update DTO to the entity
 */
interface BaseMapper<T, R, RB, U> {

    /**
     * Maps the properties of the source object (of type U) to the destination object (of type T).
     * Only the properties with the same name and compatible types in both source and destination are mapped.
     *
     * @param source The source object to map from.
     * @param destination The destination object to map to.
     */
    fun map(source: U, destination: T) {
        source!!::class.java.declaredFields.forEach { sourceField ->
            destination!!::class.java.declaredFields.firstOrNull { it.name == sourceField.name }?.let { destinationField ->
                sourceField.isAccessible = true
                destinationField.isAccessible = true
                sourceField.get(source)?.let { sourceValue ->
                    destinationField.set(destination, sourceValue)
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
     * Maps the properties of each object in the source collection (of type T) to a new object of type RB.
     * Returns a list of these new objects.
     *
     * @param source The collection of source objects to map from.
     * @return The list of new objects of type RB.
     */
    fun rbMap(source: Collection<T>): List<RB> {
        return source.map { rbMap(it) }
    }
}