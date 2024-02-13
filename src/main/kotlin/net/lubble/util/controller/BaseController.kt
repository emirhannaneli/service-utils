package net.lubble.util.controller

import jakarta.validation.Valid
import net.lubble.util.PageResponse
import net.lubble.util.Response
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

interface BaseController<C, U, R, P> {
    @PostMapping
    fun create(@RequestBody @Valid create: C): ResponseEntity<R>

    @GetMapping("{id}")
    fun findById(@PathVariable id: String): ResponseEntity<R>

    @GetMapping
    fun findAll(@Valid params: P): ResponseEntity<PageResponse>

    @PutMapping("{id}")
    fun update(@PathVariable id: String, @RequestBody @Valid update: U): ResponseEntity<R>

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Response>

    @GetMapping("recycle-bin")
    fun recycleBin(@Valid params: P): ResponseEntity<PageResponse> {
        throw UnsupportedOperationException()
    }

    @PutMapping("{id}/restore")
    fun restore(@PathVariable id: String): ResponseEntity<Response> {
        throw UnsupportedOperationException()
    }

    @DeleteMapping("{id}/permanently")
    fun deletePermanently(@PathVariable id: String): ResponseEntity<Response> {
        throw UnsupportedOperationException()
    }
}