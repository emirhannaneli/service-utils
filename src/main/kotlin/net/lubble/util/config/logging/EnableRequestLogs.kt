package net.lubble.util.config.logging

import org.springframework.context.annotation.Import

@Import(RequestLoggingConfig::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnableRequestLogs
