package net.lubble.util.annotation.reformat

//@Aspect
//@Component
class ReFormatProcessor {
    /*@Pointcut("@annotation(reFormat) || @within(reFormat)")
    fun reformat(reFormat: ReFormat) {
    }

    @AfterReturning(pointcut = "reformat(reFormat)", returning = "result")
    fun reformatAfterReturning(joinPoint: JoinPoint, reFormat: ReFormat, result: Any): Any {
        println("ReFormatProcessor reformatAfterReturning")
        println("joinPoint: $joinPoint")
        println("reFormat: $reFormat")
        println("result: $result")
        return result
    }*/

    /*val args = point.args
        for (i in args.indices) {
            val arg = args[i]
            if (arg is String) {
                val methodSignature = point.signature as MethodSignature
                val annotation = methodSignature.method.getAnnotation(ReFormat::class.java)
                val upper = annotation.upper
                val lower = annotation.lower
                val trim = annotation.trim
                val capitalize = annotation.capitalize
                val decapitate = annotation.decapitate
                val locale = annotation.locale
                val useContextLocale = annotation.useContextLocale

                var value = arg

                if (upper) {
                    value = if (useContextLocale) {
                        try {
                            val contextLocale = LocaleContextHolder.getLocale()
                            value.uppercase(contextLocale)
                        } catch (e: Exception) {
                            value.uppercase(Locale.forLanguageTag(locale))
                        }
                    } else {
                        value.uppercase(Locale.forLanguageTag(locale))
                    }
                }

                if (lower) {
                    value = if (useContextLocale) {
                        try {
                            val contextLocale = LocaleContextHolder.getLocale()
                            value.lowercase(contextLocale)
                        } catch (e: Exception) {
                            value.lowercase(Locale.forLanguageTag(locale))
                        }
                    } else {
                        value.lowercase(Locale.forLanguageTag(locale))
                    }
                }

                if (trim) {
                    value = value.trim()
                }

                if (capitalize) {
                    value = if (useContextLocale) {
                        try {
                            val contextLocale = LocaleContextHolder.getLocale()
                            value.replaceFirstChar { it.titlecase(contextLocale) }
                        } catch (e: Exception) {
                            value.replaceFirstChar { it.titlecase(Locale.forLanguageTag(locale)) }
                        }
                    } else {
                        value.replaceFirstChar { it.titlecase(Locale.forLanguageTag(locale)) }
                    }
                }

                if (decapitate) {
                    value = if (useContextLocale) {
                        try {
                            val contextLocale = LocaleContextHolder.getLocale()
                            value.replaceFirstChar { it.lowercase(contextLocale) }
                        } catch (e: Exception) {
                            value.replaceFirstChar { it.lowercase(Locale.forLanguageTag(locale)) }
                        }
                    } else {
                        value.replaceFirstChar { it.lowercase(Locale.forLanguageTag(locale)) }
                    }
                }

                args[i] = value
            }
        }
        return point.proceed(args)*/
}