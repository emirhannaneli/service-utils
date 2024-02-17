package net.lubble.util.dto

import java.util.*

open class RBase {
    open var id: Long = 0
    open lateinit var sk: String
    open lateinit var updatedAt: Date
    open lateinit var createdAt: Date
}