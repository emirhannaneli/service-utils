package net.lubble.util.exception

import net.lubble.util.model.ExceptionModel
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND

/**
 * Exception thrown when a resource is not found.
 * @property desiredName The desired ID of the resource. ("{id}" in the message)
 * @property desiredValue The desired type of the resource. ("{type}" in the message)
 */
open class NotFoundException() : RuntimeException(), ExceptionModel {
    private var desiredName: String = "UNKNOWN"
    private var desiredValue: Any = "UNKNOWN"

    constructor(desiredName: String?, desiredValue: Any?) : this() {
        this.desiredName = desiredName ?: "UNKNOWN"
        this.desiredValue = desiredValue ?: "UNKNOWN"
    }

    override fun message(): String {
        return source().getMessage("global.exception.not.found", arrayOf(desiredName, desiredValue), locale())
    }

    override fun status(): HttpStatus {
        return NOT_FOUND
    }

    override fun code(): String {
        return "0x0LB404"
    }

    fun details(): Map<String, Any> {
        return mapOf(
            "name" to desiredName,
            "value" to desiredValue
        )
    }
}