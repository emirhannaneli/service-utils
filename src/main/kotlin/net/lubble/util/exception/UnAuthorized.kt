package net.lubble.util.exception

import net.lubble.util.model.ExceptionModel
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.UNAUTHORIZED

class UnAuthorized(private val reason: String?) : RuntimeException(), ExceptionModel {
    constructor() : this(null)

    override fun code(): String {
        return "0x000401-1"
    }

    override fun message(): String {
        return "global.exception.unauthorized"
    }

    override fun status(): HttpStatus {
        return UNAUTHORIZED
    }

    fun details(): String? {
        return reason
    }
}