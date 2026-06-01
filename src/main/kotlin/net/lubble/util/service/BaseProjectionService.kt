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
interface BaseService<T : BaseModel, C, U, S> : BaseProjectionService<T, T, C, U, S> {

    /**
     * Archives all entities matching [spec] (sets archived = true) and saves them.
     * IMPORTANT: [spec] must NOT set `fields` (projection mode returns detached
     * copies that cannot be persisted). Returns the processed entities.
     */
    fun archiveAll(spec: S): Collection<T> = fetchAll(spec).onEach { it.archived = true; save(it) }

    /** Unarchives all entities matching [spec] (sets archived = false) and saves them. */
    fun unarchiveAll(spec: S): Collection<T> = fetchAll(spec).onEach { it.archived = false; save(it) }

    /** Soft-deletes all entities matching [spec] (sets deleted = true) and saves them. */
    fun softDeleteAll(spec: S): Collection<T> = fetchAll(spec).onEach { it.deleted = true; save(it) }

    /** Restores all entities matching [spec] (sets deleted = false) and saves them. */
    fun restoreAll(spec: S): Collection<T> = fetchAll(spec).onEach { it.deleted = false; save(it) }

    /** Permanently (hard) deletes all entities matching [spec]. Returns the deleted entities. */
    fun deletePermanentlyAll(spec: S): Collection<T> = fetchAll(spec).onEach { delete(it) }
}

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

