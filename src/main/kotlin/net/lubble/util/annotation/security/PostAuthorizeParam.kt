package net.lubble.util.annotation.security

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class PostAuthorizeParam(
    /**
     * @return the Spring-EL expression to be evaluated before invoking the protected
     * method
     */
    val value: String
)