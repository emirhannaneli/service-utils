package net.lubble.util.controller

import jakarta.validation.Valid
import net.lubble.util.PageResponse
import net.lubble.util.Response
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * This interface defines the basic CRUD operations for a controller.
 * It uses generic types for create (C), update (U), response (R), and parameters (P).
 */
interface BaseController<C, U, R, P> {

    /**
     * Create a new entity.
     * @param create The entity to create.
     * @return The created entity wrapped in a ResponseEntity.
     */
    @PostMapping
    fun create(@RequestBody @Valid create: C): ResponseEntity<R>

    /**
     * Find an entity by its ID.
     * @param id The ID of the entity to find.
     * @return The found entity wrapped in a ResponseEntity.
     */
    @GetMapping("{id}")
    fun findById(@PathVariable id: String): ResponseEntity<R>

    /**
     * Find all entities.
     * @param params The parameters to use when finding entities.
     * @return A page of found entities wrapped in a ResponseEntity.
     */
    @GetMapping
    fun findAll(@Valid params: P): ResponseEntity<PageResponse>

    /**
     * Update an entity.
     * @param id The ID of the entity to update.
     * @param update The updated entity.
     * @return The updated entity wrapped in a ResponseEntity.
     */
    @PutMapping("{id}")
    fun update(@PathVariable id: String, @RequestBody @Valid update: U): ResponseEntity<R>

    /**
     * Delete an entity.
     * @param id The ID of the entity to delete.
     * @return A response wrapped in a ResponseEntity.
     */
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Response>

    /**
     * Find all archived entities.
     * @param params The parameters to use when finding archived entities.
     * @return A page of found archived entities wrapped in a ResponseEntity.
     */
    @GetMapping("archive")
    fun findAllArchived(@Valid params: P): ResponseEntity<PageResponse> {
        throw UnsupportedOperationException()
    }

    /**
     * Archive an entity.
     * @param id The ID of the entity to archive.
     * @return A response wrapped in a ResponseEntity.
     */
    @PutMapping("{id}/archive")
    fun archive(@PathVariable id: String): ResponseEntity<Response> {
        throw UnsupportedOperationException()
    }

    /**
     * Unarchive an entity.
     * @param id The ID of the entity to unarchive.
     * @return A response wrapped in a ResponseEntity.
     */
    @PutMapping("{id}/unarchive")
    fun unarchive(@PathVariable id: String): ResponseEntity<Response> {
        throw UnsupportedOperationException()
    }

    /**
     * Find all entities in the recycle bin.
     * @param params The parameters to use when finding entities in the recycle bin.
     * @return A page of found entities in the recycle bin wrapped in a ResponseEntity.
     */
    @GetMapping("recycle-bin")
    fun recycleBin(@Valid params: P): ResponseEntity<PageResponse> {
        throw UnsupportedOperationException()
    }

    /**
     * Clear the recycle bin.
     * @return A response wrapped in a ResponseEntity.
     */
    @DeleteMapping("recycle-bin/clear")
    fun clearRecycleBin(): ResponseEntity<Response> {
        throw UnsupportedOperationException()
    }

    /**
     * Restore an entity from the recycle bin.
     * @param id The ID of the entity to restore.
     * @return A response wrapped in a ResponseEntity.
     */
    @PutMapping("{id}/restore")
    fun restore(@PathVariable id: String): ResponseEntity<Response> {
        throw UnsupportedOperationException()
    }

    /**
     * Permanently delete an entity.
     * @param id The ID of the entity to permanently delete.
     * @return A response wrapped in a ResponseEntity.
     */
    @DeleteMapping("{id}/permanently")
    fun deletePermanently(@PathVariable id: String): ResponseEntity<Response> {
        throw UnsupportedOperationException()
    }
}