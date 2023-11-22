package net.lubble.util.controller

import jakarta.validation.Valid
import net.lubble.util.Response
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.web.bind.annotation.*

/**
 * Base controller for all controllers.
 * @param C Create model
 * @param U Update model
 * @param ID ID type
 * @property create Create function
 * @property findById Find by ID function
 * @property findAll Find all function
 * @property update Update function
 * @property delete Delete function

 */
interface V1BaseController<C, U, R, P, ID> {
    @PostMapping
    fun create(@RequestBody @Valid create: C): ResponseEntity<Response>

    @MessageMapping("create")
    fun createRSocket(@Payload create: C): R {
        throw UnsupportedOperationException()
    }

    @GetMapping("{id}")
    fun findById(@PathVariable id: ID): ResponseEntity<Response>

    @MessageMapping("find.{id}")
    fun findRSocket(@DestinationVariable id: ID): R {
        throw UnsupportedOperationException()
    }

    @GetMapping
    fun findAll(@Valid params: P): ResponseEntity<Response>

    @PutMapping("{id}")
    fun update(@PathVariable id: ID, @RequestBody @Valid update: U): ResponseEntity<Response>

    @MessageMapping("update.{id}")
    fun updateRSocket(@DestinationVariable id: ID, @Payload update: U): R {
        throw UnsupportedOperationException()
    }

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: ID): ResponseEntity<Response>

    @MessageMapping("delete.{id}")
    fun deleteRSocket(@DestinationVariable id: ID): R {
        throw UnsupportedOperationException()
    }
}