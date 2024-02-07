package net.lubble.util.annotation.lower

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JacksonAnnotationsInside
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@JsonSerialize(converter = LowerProcessor::class)
@JsonDeserialize(converter = LowerProcessor::class)
annotation class Lower()
