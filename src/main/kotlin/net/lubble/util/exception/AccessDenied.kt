package net.lubble.util.exception

import net.lubble.util.model.ExceptionModel
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN

class AccessDenied(private val reason: String?) : RuntimeException(), ExceptionModel {
    constructor() : this(null)

    override fun code(): String {
        return "0x000403-1"
    }

    override fun message(): String {
        return "global.exception.access.denied"
    }

    override fun status(): HttpStatus {
        return FORBIDDEN
    }

    fun details(): String? {
        return reason
    }
}