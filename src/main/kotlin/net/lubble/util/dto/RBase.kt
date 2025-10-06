package net.lubble.util.dto

import net.lubble.util.model.BaseModel

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
open class RBase : BaseModel()