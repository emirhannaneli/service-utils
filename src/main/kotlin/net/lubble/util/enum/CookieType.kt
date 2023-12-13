package net.lubble.util.enum

import net.lubble.util.AppContextUtil
import net.lubble.util.config.lubble.LubbleConfig

enum class CookieType {
    AUTHENTICATION {
        override val value: String
            get() = prefix() + "auth"

    },
    REFRESH {
        override val value: String
            get() = prefix() + "refresh"
    },
    PROFILE {
        override val value: String
            get() = prefix() + "profile"
    },
    CART {
        override val value: String
            get() = prefix() + "cart"
    },
    WISHLIST {
        override val value: String
            get() = prefix() + "wishlist"
    };

    abstract val value: String

    fun prefix(): String {
        return AppContextUtil.bean(LubbleConfig::class.java).security.cookie.prefix ?: default
    }
}

private const val default = "_lubble-"