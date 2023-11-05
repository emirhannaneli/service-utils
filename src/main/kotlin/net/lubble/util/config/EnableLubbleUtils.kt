package net.lubble.util.config

import org.springframework.context.annotation.Import

@Import(
    EnableLubbleUtilsConfig::class
)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class EnableLubbleUtils()
