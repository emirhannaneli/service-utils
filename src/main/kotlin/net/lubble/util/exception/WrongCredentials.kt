package net.lubble.util.exception

import net.lubble.util.model.ExceptionModel
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.UNAUTHORIZED

class WrongCredentials: RuntimeException(), ExceptionModel {
    override fun message(): String {
        return "global.exception.wrong.credentials"
    }

    override fun status(): HttpStatus {
        return UNAUTHORIZED
    }

    override fun code(): String {
        return "0x0LB401-0"
    }
}