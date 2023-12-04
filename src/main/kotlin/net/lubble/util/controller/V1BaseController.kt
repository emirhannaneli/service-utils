package net.lubble.util.controller

import jakarta.validation.Valid
import net.lubble.util.Response
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Base controller for all controllers.
 * @param C Create model
 * @param U Update model
 * @param P Params model
 * @param ID ID type
 * @property create Create function
 * @property findById Find by ID function
 * @property findAll Find all function
 * @property update Update function
 * @property delete Delete function
 */
interface V1BaseController<C, U, P, ID> {
    @PostMapping
    fun create(@RequestBody @Valid create: C): ResponseEntity<Response>

    @GetMapping("{id}")
    fun findById(@PathVariable id: ID): ResponseEntity<Response>

    @GetMapping
    fun findAll(@Valid params: P): ResponseEntity<Response>

    @PutMapping("{id}")
    fun update(@PathVariable id: ID, @RequestBody @Valid update: U): ResponseEntity<Response>

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: ID): ResponseEntity<Response>

}