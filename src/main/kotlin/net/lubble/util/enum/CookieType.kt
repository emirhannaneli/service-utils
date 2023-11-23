package net.lubble.util.enum

enum class CookieType {
    AUTHENTICATION {
        override val value: String
            get() = prefix + "auth"

    },
    REFRESH {
        override val value: String
            get() = prefix + "refresh"
    },
    PROFILE {
        override val value: String
            get() = prefix + "profile"
    },
    CART {
        override val value: String
            get() = prefix + "cart"
    },
    WISHLIST {
        override val value: String
            get() = prefix + "wishlist"
    };

    abstract val value: String
}

private const val prefix = "_lubble-"