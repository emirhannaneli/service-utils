package net.lubble.util.annotation.trim

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JacksonAnnotationsInside
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@JsonSerialize(converter = TrimProcessor::class)
@JsonDeserialize(converter = TrimProcessor::class)
annotation class Trim()
