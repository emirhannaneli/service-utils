package net.lubble.util.dto

import java.util.*

/**
 * This is a base class for DTOs (Data Transfer Objects).
 * It includes common fields that are present in most DTOs.
 *
 * The fields include:
 * - id: A unique identifier for the DTO.
 * - sk: A secondary key for the DTO.
 * - updatedAt: The date and time when the DTO was last updated.
 * - createdAt: The date and time when the DTO was created.
 */
open class RBase {
    /**
     * The unique identifier for the DTO.
     */
    open lateinit var id: String

    /**
     * A secondary key for the DTO.
     */
    open lateinit var sk: String

    /**
     * The date and time when the DTO was last updated.
     */
    open lateinit var updatedAt: Date

    /**
     * The date and time when the DTO was created.
     */
    open lateinit var createdAt: Date
}