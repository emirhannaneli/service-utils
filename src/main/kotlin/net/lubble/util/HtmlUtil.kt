package net.lubble.util

/**
 * HtmlUtil is a utility class that provides methods for manipulating HTML strings.
 */
class HtmlUtil {
    companion object {
        // Compiled once; these run on every call.
        private val ANY_TAG = Regex("<.*?>")
        private val TAG_AND_TEXT = Regex("(<.+?>)?([^<>]*)")
        private val EMPTY_TAG = Regex("^<\\s*(img|br|input|hr|area|base|basefont|col|frame|isindex|link|meta|param).*>$")
        private val CLOSING_TAG = Regex("^<\\s*/\\s*([a-zA-Z]+[1-6]?)\\s*>$")
        private val OPENING_TAG = Regex("^<\\s*([a-zA-Z]+[1-6]?).*?>$")
        private val ENTITY = Regex("(&[0-9a-z]{2,8};|&#[0-9]{1,7};|[0-9a-f]{1,6};)")

        /**
         * Shortens the given HTML string to the specified length, preserving HTML tags.
         * If the length of the text content (excluding tags) is less than or equal to the specified length, the original HTML string is returned.
         * Otherwise, the HTML string is shortened to the specified length, and any open tags are closed.
         * If the shortened HTML string is still longer than the specified length due to closing tags, an ellipsis is appended.
         *
         * @param html The HTML string to shorten.
         * @param length The maximum length of the text content in the shortened HTML string.
         * @return The shortened HTML string.
         */
        fun shorten(html: String, length: Int): String {
            // If the length of the text content is less than or equal to the specified length, return the original HTML string
            if (html.replace(ANY_TAG, "").length <= length) {
                return html
            }

            // Find all tags and text content in the HTML string
            val tagMatcher = TAG_AND_TEXT.findAll(html)
            // Keep track of open tags that need to be closed
            val openTags = mutableListOf<String>()
            // Build the shortened HTML string
            val result = tagMatcher.fold(StringBuilder()) { result, tagMatchResult ->
                // Extract the tag and text content from the current match
                val tagText = tagMatchResult.groups[1]?.value
                val plainText = tagMatchResult.groups[2]?.value

                // If the length of the result has reached the specified length, stop appending
                if (result.length >= length) {
                    return@fold result
                }

                // If the current match includes a tag, handle it
                if (!tagText.isNullOrBlank()) {
                    // If the tag is an empty tag, ignore it
                    // If the tag is a closing tag, remove the corresponding opening tag from the list of open tags
                    // If the tag is an opening tag, add it to the list of open tags
                    // The tag name is taken from the pattern's capture group: attributes must not leak into it.
                    when {
                        EMPTY_TAG.matches(tagText) -> {}
                        CLOSING_TAG.matches(tagText) ->
                            openTags.remove(CLOSING_TAG.find(tagText)!!.groupValues[1].lowercase())

                        OPENING_TAG.matches(tagText) ->
                            openTags.add(0, OPENING_TAG.find(tagText)!!.groupValues[1].lowercase())
                    }
                    // Append the tag to the result
                    result.append(tagText)
                }

                // Calculate the length of the text content, replacing entities with a single space
                val contentLength = plainText?.replace(ENTITY, " ")?.length ?: 0
                // If appending the full text content would exceed the specified length, append only a part of it
                // Otherwise, append the full text content
                if (result.length + contentLength > length) {
                    // Tags count towards result.length, so the remaining budget can already be used up.
                    val proposedChopPosition = (length - result.length).coerceAtLeast(0)
                    result.append(plainText?.take(proposedChopPosition))
                    return@fold result
                } else {
                    result.append(plainText)
                }

                // Return the result for the next iteration
                result
            }

            // Close any open tags
            openTags.forEach { openTag ->
                result.append("</$openTag>")
            }
            // If the result is still longer than the specified length due to closing tags, append an ellipsis
            if (result.length >= length) {
                result.append("...")
            }
            // Return the shortened HTML string
            return result.toString()
        }
    }
}