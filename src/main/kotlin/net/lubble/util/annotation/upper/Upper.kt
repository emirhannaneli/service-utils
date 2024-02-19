package net.lubble.util.annotation.upper

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JacksonAnnotationsInside
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@JsonSerialize(converter = UpperProcessor::class)
@JsonDeserialize(converter = UpperProcessor::class)
annotation class Upper
