package net.lubble.util.config.utils

import org.springframework.context.annotation.Import

@Import(
    EnableLubbleUtilsConfig::class
)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class EnableLubbleUtils() {
}
