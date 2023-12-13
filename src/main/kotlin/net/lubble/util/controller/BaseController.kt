package net.lubble.util.controller

import jakarta.validation.Valid
import net.lubble.util.PageResponse
import net.lubble.util.Response
import net.lubble.util.model.ParameterModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

interface BaseController<C, U, R, ID> {
    @PostMapping
    fun create(@RequestBody @Valid create: C): R

    @GetMapping("{id}")
    fun findById(@PathVariable id: ID): R

    @GetMapping
    fun findAll(@Valid params: ParameterModel): ResponseEntity<PageResponse>

    @PutMapping("{id}")
    fun update(@PathVariable id: ID, @RequestBody @Valid update: U): ResponseEntity<Response>

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: ID): ResponseEntity<Response>
}