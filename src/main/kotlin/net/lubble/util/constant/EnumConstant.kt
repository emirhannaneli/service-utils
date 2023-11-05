package net.lubble.util.constant

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * EnumConstant is a data class that represents a constant in an enum.
 * It is used to represent the enum constants in the frontend.
 * @param label The label of the enum constant.
 * @param value The value of the enum constant.
 * @param color The color of the enum constant.
 * @param icon The icon of the enum constant.
 * @constructor Creates an EnumConstant.
 * */
data class EnumConstant(
    @JsonProperty("label")
    val label: String,
    @JsonProperty("value")
    val value: String,
    @JsonProperty("color")
    val color: String? = null,
    @JsonProperty("icon")
    val icon: String? = null
)
