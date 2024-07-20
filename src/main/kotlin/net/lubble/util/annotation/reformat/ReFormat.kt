package net.lubble.util.annotation.reformat

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReFormat(
    val upper: Boolean = false,
    val lower: Boolean = false,
    val trim: Boolean = false,
    val capitalize: Boolean = false,
    val decapitate: Boolean = false,
    val locale: String = "en",
    val useContextLocale: Boolean = false
)
