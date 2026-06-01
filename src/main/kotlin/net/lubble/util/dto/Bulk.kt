package net.lubble.util.dto

/**
 * Request body for bulk operations. Each id may be a pk or sk string
 * (resolved via LID.matches). Empty list is a no-op.
 */
data class BulkIdRequest(val ids: List<String> = emptyList())

/**
 * A single id that could not be processed in a bulk operation.
 * @param id the requested id (pk or sk string)
 * @param reason short machine-readable reason, e.g. "not_found"
 */
data class BulkFailure(val id: String, val reason: String)

/**
 * Result of a bulk operation.
 * @param successIds requested ids that were processed
 * @param failed requested ids that were not processed, with reasons
 */
data class BulkResponse(
    val successIds: List<String>,
    val failed: List<BulkFailure>,
)
