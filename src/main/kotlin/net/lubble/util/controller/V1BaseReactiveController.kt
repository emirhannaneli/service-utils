package net.lubble.util.controller

import jakarta.validation.Valid
import net.lubble.util.AppContextUtil
import net.lubble.util.Response
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*

/**
 * Base reactive controller for all controllers.
 * @param C Create model
 * @param U Update model
 * @param ID ID type
 * @property create Create function
 * @property findById Find by ID function
 * @property findAll Find all function
 * @property update Update function
 * @property delete Delete function

 */
interface V1BaseReactiveController<C, U, P, ID> {
    @PostMapping
    fun create(@RequestBody @Valid create: C): Mono<ResponseEntity<Response>>

    @GetMapping("{id}")
    fun findById(@PathVariable id: ID): Mono<ResponseEntity<Response>>

    @GetMapping
    fun findAll(@Valid params: P): Mono<ResponseEntity<Response>>

    @PutMapping("{id}")
    fun update(@PathVariable id: ID, @RequestBody @Valid update: U): Mono<ResponseEntity<Response>>

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: ID): Mono<ResponseEntity<Response>>
}