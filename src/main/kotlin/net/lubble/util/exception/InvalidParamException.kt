package net.lubble.util.exception

import net.lubble.util.model.ExceptionModel
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST

/**
 * Exception thrown when a parameter is invalid.
 * @property param The name of the parameter. ("{param}" in the message)
 * @property value The value of the parameter. ("{value}" in the message)
 * @property desired The desired value of the parameter. ("{desired}" in the message)
 */
open class InvalidParamException() : RuntimeException(), ExceptionModel {
    private var param: String? = null
    private var value: Any? = null
    private var desired: Any? = null

    constructor(param: String, value: Any, desired: Any) : this() {
        this.param = param
        this.value = value
        this.desired = desired
    }

    override fun message(): String {
        return source().getMessage("global.exception.invalid.param", null, locale())
    }

    override fun status(): HttpStatus {
        return BAD_REQUEST
    }

    override fun code(): String {
        return "0x0LB400-0"
    }

    @Suppress("unused")
    fun details(): Map<String, Any?> {
        return mapOf(
            "param" to param,
            "value" to value,
            "desired" to desired
        )
    }
}