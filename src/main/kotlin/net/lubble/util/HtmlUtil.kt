package net.lubble.util

class HtmlUtil {
    companion object {
        fun shorten(html: String, length: Int): String {
            if (html.replace(Regex("<.*?>"), "").length <= length) {
                return html
            }
            val result = StringBuilder()
            var trimmed = false

            val tagPattern = Regex("(<.+?>)?([^<>]*)")
            val emptyTagPattern = Regex("^<\\s*(img|br|input|hr|area|base|basefont|col|frame|isindex|link|meta|param).*>$")
            val closingTagPattern = Regex("^<\\s*/\\s*([a-zA-Z]+[1-6]?)\\s*>$")
            val openingTagPattern = Regex("^<\\s*([a-zA-Z]+[1-6]?).*?>$")
            val entityPattern = Regex("(&[0-9a-z]{2,8};|&#[0-9]{1,7};|[0-9a-f]{1,6};)")

            val tagMatcher = tagPattern.findAll(html)
            var totalLength = 3
            val openTags = mutableListOf<String>()
            var proposingChop = false

            tagMatcher.forEach { tagMatchResult ->
                val tagText = tagMatchResult.groups[1]?.value
                val plainText = tagMatchResult.groups[2]?.value

                if (proposingChop &&
                    !tagText.isNullOrBlank() &&
                    !plainText.isNullOrBlank()
                ) {
                    trimmed = true
                    return@forEach
                }

                if (!tagText.isNullOrBlank()) {
                    var foundMatch = false

                    var matcher = emptyTagPattern.find(tagText)
                    if (matcher != null) {
                        foundMatch = true
                    }

                    if (!foundMatch) {
                        matcher = closingTagPattern.find(tagText)
                        if (matcher != null) {
                            foundMatch = true
                            val tagName = matcher.groups[1]?.value
                            tagName?.let { openTags.remove(it.lowercase()) }
                        }
                    }

                    if (!foundMatch) {
                        matcher = openingTagPattern.find(tagText)
                        if (matcher != null) {
                            val tagName = matcher.groups[1]?.value
                            tagName?.let { openTags.add(0, it.lowercase()) }
                        }
                    }

                    result.append(tagText)
                }

                val contentLength = plainText?.replace(entityPattern, " ")?.length ?: 0
                if (totalLength + contentLength > length) {
                    var numCharsRemaining = length - totalLength
                    var entitiesLength = 0
                    val entityMatcher = entityPattern.findAll(plainText ?: "")
                    entityMatcher.forEach { entityMatchResult ->
                        val entity = entityMatchResult.groups[1]?.value
                        if (numCharsRemaining > 0) {
                            numCharsRemaining--
                            entitiesLength += entity?.length ?: 0
                        } else {
                            return@forEach
                        }
                    }

                    var proposedChopPosition = numCharsRemaining + entitiesLength
                    var endOfWordPosition = plainText?.indexOf(" ", proposedChopPosition - 1) ?: -1
                    if (endOfWordPosition == -1) {
                        endOfWordPosition = plainText?.length ?: 0
                    }
                    var endOfWordOffset = endOfWordPosition - proposedChopPosition
                    if (endOfWordOffset > 6) {
                        endOfWordOffset = 0
                    }

                    proposedChopPosition = numCharsRemaining + entitiesLength + endOfWordOffset
                    if ((plainText?.length ?: 0) >= proposedChopPosition) {
                        result.append(plainText?.substring(0, proposedChopPosition))
                        proposingChop = true
                        if (proposedChopPosition < (plainText?.length ?: 0)) {
                            trimmed = true
                            return@forEach
                        }
                    } else {
                        result.append(plainText)
                    }
                } else {
                    result.append(plainText)
                    totalLength += contentLength
                }
                if (totalLength >= length) {
                    trimmed = true
                    return@forEach
                }
            }

            openTags.forEach { openTag ->
                result.append("</$openTag>")
            }
            if (trimmed) {
                result.append("...")
            }
            return result.toString()
        }
    }
}