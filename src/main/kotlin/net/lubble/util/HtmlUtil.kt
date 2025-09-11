package net.lubble.util

/**
 * HtmlUtil is a utility class that provides methods for manipulating HTML strings.
 */
class HtmlUtil {
    companion object {
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
            // Remove all HTML tags from the input string
            val htmlWithoutTags = html.replace(Regex("<.*?>"), "")
            // If the length of the text content is less than or equal to the specified length, return the original HTML string
            if (htmlWithoutTags.length <= length) {
                return html
            }

            // Define regular expressions for matching different types of HTML tags
            val tagPattern = Regex("(<.+?>)?([^<>]*)")
            val emptyTagPattern = Regex("^<\\s*(img|br|input|hr|area|base|basefont|col|frame|isindex|link|meta|param).*>$")
            val closingTagPattern = Regex("^<\\s*/\\s*([a-zA-Z]+[1-6]?)\\s*>$")
            val openingTagPattern = Regex("^<\\s*([a-zA-Z]+[1-6]?).*?>$")
            val entityPattern = Regex("(&[0-9a-z]{2,8};|&#[0-9]{1,7};|[0-9a-f]{1,6};)")

            // Find all tags and text content in the HTML string
            val tagMatcher = tagPattern.findAll(html)
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
                    when {
                        emptyTagPattern.matches(tagText) -> {}
                        closingTagPattern.matches(tagText) -> openTags.remove(tagText.replace(Regex("[^a-zA-Z1-6]"), "").lowercase())
                        openingTagPattern.matches(tagText) -> openTags.add(0, tagText.replace(Regex("[^a-zA-Z1-6]"), "").lowercase())
                    }
                    // Append the tag to the result
                    result.append(tagText)
                }

                // Calculate the length of the text content, replacing entities with a single space
                val contentLength = plainText?.replace(entityPattern, " ")?.length ?: 0
                // If appending the full text content would exceed the specified length, append only a part of it
                // Otherwise, append the full text content
                if (result.length + contentLength > length) {
                    val proposedChopPosition = length - result.length
                    result.append(plainText?.substring(0, proposedChopPosition))
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