package net.lubble.util.controller

import jakarta.validation.Valid
import net.lubble.util.PageResponse
import net.lubble.util.Response
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

/**
 * This interface defines the basic CRUD operations for a controller.
 * It uses generic types for create (C), update (U), response (R), and parameters (P).
 */
interface BaseController<C, U, R, P> {

    /**
     * Creates a new entity.
     *
     * @param create The entity to create.
     * @return The created entity wrapped in a ResponseEntity.
     */
    @PostMapping
    fun create(@RequestBody @Valid create: C): ResponseEntity<R>

    /**
     * Finds an entity by its ID.
     *
     * @param id The ID of the entity to find.
     * @return The found entity wrapped in a ResponseEntity.
     */
    @GetMapping("{id}")
    fun findById(@PathVariable id: String): ResponseEntity<R>

    /**
     * Finds all entities.
     *
     * @param params The parameters to use when finding entities.
     * @return A page of found entities wrapped in a ResponseEntity.
     */
    @GetMapping
    fun findAll(@Valid params: P): ResponseEntity<PageResponse>

    /**
     * Updates an existing entity.
     *
     * @param id The ID of the entity to update.
     * @param update The updated entity.
     * @return The updated entity wrapped in a ResponseEntity.
     */
    @PutMapping("{id}")
    fun update(@PathVariable id: String, @RequestBody @Valid update: U): ResponseEntity<R>

    /**
     * Deletes an entity by its ID.
     *
     * @param id The ID of the entity to delete.
     * @return A response wrapped in a ResponseEntity.
     */
    @DeleteMapping("{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Response>

    /**
     * Finds all archived entities.
     *
     * @param params The parameters to use when finding archived entities.
     * @return A page of found archived entities wrapped in a ResponseEntity.
     */
    @GetMapping("archive")
    fun findAllArchived(@Valid params: P): ResponseEntity<PageResponse> {
        throw UnsupportedOperationException()
    }

    /**
     * Archives an entity.
     *
     * @param id The ID of the entity to archive.
     * @return A response wrapped in a ResponseEntity.
     */
    @PutMapping("{id}/archive")
    fun archive(@PathVariable id: String): ResponseEntity<Response> {
        throw UnsupportedOperationException()
    }

    /**
     * Unarchives an entity.
     *
     * @param id The ID of the entity to unarchive.
     * @return A response wrapped in a ResponseEntity.
     */
    @PutMapping("{id}/unarchive")
    fun unarchive(@PathVariable id: String): ResponseEntity<Response> {
        throw UnsupportedOperationException()
    }

    /**
     * Finds all entities in the recycle bin.
     *
     * @param params The parameters to use when finding entities in the recycle bin.
     * @return A page of found entities in the recycle bin wrapped in a ResponseEntity.
     */
    @GetMapping("recycle-bin")
    fun recycleBin(@Valid params: P): ResponseEntity<PageResponse> {
        throw UnsupportedOperationException()
    }

    /**
     * Clears the recycle bin.
     *
     * @return A response wrapped in a ResponseEntity.
     */
    @DeleteMapping("recycle-bin/clear")
    fun clearRecycleBin(): ResponseEntity<Response> {
        throw UnsupportedOperationException()
    }

    /**
     * Restores an entity from the recycle bin.
     *
     * @param id The ID of the entity to restore.
     * @return A response wrapped in a ResponseEntity.
     */
    @PutMapping("{id}/restore")
    fun restore(@PathVariable id: String): ResponseEntity<Response> {
        throw UnsupportedOperationException()
    }

    /**
     * Permanently delete an entity.
     *
     * @param id The ID of the entity to permanently delete.
     * @return A response wrapped in a ResponseEntity.
     */
    @DeleteMapping("{id}/permanently")
    fun deletePermanently(@PathVariable id: String): ResponseEntity<Response> {
        throw UnsupportedOperationException()
    }

    /**
     * Creates a ResponseEntity with a 201 Created status and a location header.
     *
     * @param read The entity to include in the response.
     * @param uri The URI to include in the location header.
     * */
    fun created(read: R, uri: String): ResponseEntity<R> = ResponseEntity.created(URI.create(uri)).body(read)

    /**
     * Creates a ResponseEntity with a 200 OK status.
     *
     * @param status The status to include in the response.
     */
    fun ok(status: Boolean): ResponseEntity<Boolean> = ResponseEntity.ok(status)

    /**
     * Creates a ResponseEntity with a 200 OK status.
     *
     * @param read The object to include in the response.
     */
    fun ok(read: R): ResponseEntity<R> = ResponseEntity.ok(read)

    /**
     * Creates a ResponseEntity with a 200 OK status and a message.
     *
     * @param message The message to include in the response.
     */
    fun message(message: String): ResponseEntity<Response> = Response(message).build()

    /**
     * Creates a PageResponse from a Page and a list of data.
     *
     * @param page The Page to construct the PageResponse from.
     * @param data The data to include in the PageResponse.
     */
    fun paged(page: Page<*>, data: Collection<*>): ResponseEntity<PageResponse> = Response.of(page, data)
}