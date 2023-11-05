package net.lubble.util.mapper

/**
 * BaseMapper interface for mapping between DTOs and Entities.
 * @param T Entity
 * @param R Read DTO
 * @param RB Read Basic DTO
 * @param U Update DTO
 *
 * @property map(source: U, destination: T) - maps the update DTO to the entity
 * */
interface BaseMapper<T, R, RB, U> {

    fun map(source: U, destination: T) {
        val sourceClass = source!!::class.java
        val destinationClass = destination!!::class.java

        val sourceFields = sourceClass.declaredFields
        val destinationFields = destinationClass.declaredFields

        for (sourceField in sourceFields) {
            for (destinationField in destinationFields) {
                if (sourceField.name == destinationField.name) {
                    sourceField.isAccessible = true
                    destinationField.isAccessible = true

                    val sourceValue = sourceField.get(source)
                    if (sourceValue != null) {
                        destinationField.set(destination, sourceValue)
                    }
                    break
                }
            }
        }
    }

    fun map(source: T): R

    fun rbMap(source: T): RB

    fun map(source: Collection<T>): List<R>

    fun rbMap(source: Collection<T>): List<RB>
}
