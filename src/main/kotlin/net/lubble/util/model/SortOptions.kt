package net.lubble.util.model

open class SortOptions {
    var allowedSortFields: HashSet<String>? = null
    var disallowedSortFields: HashSet<String>? = null

    /**
     * Filters the given sort field names by allowedSortFields (whitelist) or disallowedSortFields (blacklist).
     * If allowedSortFields is non-null, only fields in that list are returned.
     * Else if disallowedSortFields is non-null, fields in that list are excluded.
     * If both are null, returns the original list unchanged.
     */
    fun filterAllowedFields(fields: HashSet<String>): HashSet<String> {
        return when {
            allowedSortFields != null -> fields.intersect((allowedSortFields ?: emptySet()).toSet())
            disallowedSortFields != null -> fields.subtract((disallowedSortFields ?: emptySet()).toSet())
            else -> fields
        } as HashSet<String>
    }
}