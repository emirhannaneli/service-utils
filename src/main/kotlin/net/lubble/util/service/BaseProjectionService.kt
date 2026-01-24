package net.lubble.util.service

import net.lubble.util.model.BaseModel
import org.springframework.data.domain.Page

/**
 * This interface defines the basic CRUD operations for a service.
 * It uses generic types for the entity (T), create (C), update (U), and specification (S).
 * This interface assumes that the projection type is the same as the entity type.
 *
 * @param T The entity type.
 * @param C The creation type.
 * @param U The update type.
 * @param S The specification type.
 */
interface BaseService<T : BaseModel, C, U, S> : BaseProjectionService<T, T, C, U, S>

/**
 * This interface defines the basic CRUD operations for a service with projections.
 * It uses generic types for the entity (T), projection (V), create (C),
 * update (U), and specification (S).
 *
 * @param T The entity type.
 * @param V The projection type.
 * @param C The creation type.
 * @param U The update type.
 * @param S The specification type.
 */
interface BaseProjectionService<T : BaseModel, V : BaseModel, C, U, S> {

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
     * Find a projection by its specification.
     * @param spec The specification to use when finding the projection.
     * @return The found projection.
     */
    fun findp(spec: S): V? = throw NotImplementedError()

    /**
     * Check if an entity exists by its specification.
     * @param spec The specification to use when checking if the entity exists.
     * @return A boolean indicating if the entity exists.
     */
    fun exists(spec: S): Boolean

    /**
     * Find all entities by their specification.
     * @param spec The specification to use when finding the entities.
     * @return A page of found entities.
     */
    fun findAll(spec: S): Page<V>

    /**
     * Fetch all entities by their specification.
     * @param spec The specification to use when fetching the entities.
     * @return A collection of found entities.
     */
    fun fetchAll(spec: S): Collection<V>

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
     * Delete all entities matching the specification.
     * @param spec The specification to use when deleting the entities.
     */
    fun deleteAll(spec: S)
}

