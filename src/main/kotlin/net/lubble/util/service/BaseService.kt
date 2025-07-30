package net.lubble.util.service

import org.springframework.data.domain.Page

/**
 * This interface defines the basic CRUD operations for a service.
 * It uses generic types for the entity (T), create (C), update (U), and specification (S).
 * @param T The entity type.
 * @param C The creation type.
 * @param U The update type.
 * @param S The specification type.
 */
interface BaseService<T, C, U, S> {

    /**
     * Create a new entity.
     * @param create The entity to create.
     * @return The created entity.
     */
    fun create(create: C): T

    /**
     * Save an entity.
     * @param base The entity to save.
     * @return The saved entity.
     */
    fun save(base: T): T

    /**
     * Find an entity by its specification.
     * @param spec The specification to use when finding the entity.
     * @return The found entity.
     */
    fun find(spec: S): T?

    /**
     * Check if an entity exists by its specification.
     * @param spec The specification to use when checking if the entity exists.
     * @return A boolean indicating if the entity exists.
     */
    fun exists(spec: S): Boolean

    /**
     * Find an entity by its ID and fields.
     * @param id The ID of the entity to find.
     * @param fields The fields to find.
     */
    fun findById(id: String, fields: Collection<String>? = null): T

    /**
     * Find all entities by their specification.
     * @param spec The specification to use when finding the entities.
     * @return A page of found entities.
     */
    fun findAll(spec: S): Page<T>

    /**
     * Update an entity.
     * @param base The entity to update.
     * @param update The updated entity.
     */
    fun update(base: T, update: U)

    /**
     * Delete an entity.
     * @param base The entity to delete.
     */
    fun delete(base: T)

    /**
     * Delete all entities matching the given specification.
     * @param spec The specification to use when deleting the entities.
     */
    fun delete(spec: S)
}