package net.lubble.util.exception

import net.lubble.util.model.ExceptionModel
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT

/**
 * Exception thrown when a resource already exists.
 * @property desiredName The desired ID of the resource. ("{id}" in the message)
 * @property desiredValue The desired type of the resource. ("{type}" in the message)
 */
open class AlreadyExistsException() : RuntimeException(), ExceptionModel {
    private var desiredName: String = "UNKNOWN"
    private var desiredValue: Any = "UNKNOWN"

    constructor(desiredName: String?, desiredValue: Any?) : this(){
        this.desiredName = desiredName ?: "UNKNOWN"
        this.desiredValue = desiredValue ?: "UNKNOWN"
    }

    override fun message(): String {
        val message = source().getMessage("global.exception.already.exists", null, locale())
        return message.replace("{name}", desiredName).replace("{value}", if (desiredValue is String) "'$desiredValue'" else "[object]")
    }

    override fun status(): HttpStatus {
        return CONFLICT
    }

    override fun code(): String {
        return "0x0LB409-0"
    }

    fun details(): Map<String, Any> {
        return mapOf(
            "name" to desiredName,
            "value" to desiredValue
        )
    }
}