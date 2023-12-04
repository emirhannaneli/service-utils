package net.lubble.util.controller

import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload

/**
 * Base controller for all controllers.
 * @param C Create model
 * @param U Update model
 * @param R Read model
 * @param ID ID type
 * @property create Create function
 * @property findById Find by ID function
 * @property update Update function
 * @property delete Delete function
 */
interface RSocketController<C, U, R, ID> {
    @MessageMapping("create")
    fun createRSocket(@Payload create: C): R {
        throw UnsupportedOperationException()
    }

    @MessageMapping("find.{id}")
    fun findRSocket(@DestinationVariable id: ID): R {
        throw UnsupportedOperationException()
    }

    @MessageMapping("update.{id}")
    fun updateRSocket(@DestinationVariable id: ID, @Payload update: U): R {
        throw UnsupportedOperationException()
    }

    @MessageMapping("delete.{id}")
    fun deleteRSocket(@DestinationVariable id: ID): R {
        throw UnsupportedOperationException()
    }
}