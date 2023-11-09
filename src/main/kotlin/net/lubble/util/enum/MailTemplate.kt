package net.lubble.util.enum

import net.lubble.util.AppContextUtil
import net.lubble.util.helper.EnumHelper
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import java.util.*

enum class MailTemplate: EnumHelper {
    DIFFERENT_DEVICE_SIGN_IN_TR {
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
        override val label: String
            get() = "mail.template.different.device.sign.in.subject"
        override val value: String
            get() = "default-template-with-actions"
        override val header: String
            get() = localized("mail.template.different.device.sign.in.header")
        override val title: String
            get() = localized("mail.template.different.device.sign.in.title")
        override val subtitle: String
            get() = localized("mail.template.different.device.sign.in.subtitle")
        override val link: String
            get() = localized("mail.template.different.device.sign.in.link")
    },
    VERIFICATION_TR {
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
        override val label: String
            get() = "mail.template.verification.subject"
        override val value: String
            get() = "default-template-with-actions"
        override val header: String
            get() = localized("mail.template.verification.header")
        override val title: String
            get() = localized("mail.template.verification.title")
        override val subtitle: String
            get() = localized("mail.template.verification.subtitle")
        override val link: String
            get() = localized("mail.template.verification.link")
    },
    RESET_PASSWORD_TR {
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
        override val label: String
            get() = "mail.template.reset.password.subject"
        override val value: String
            get() = "default-template-with-actions"
        override val header: String
            get() = localized("mail.template.reset.password.header")
        override val title: String
            get() = localized("mail.template.reset.password.title")
        override val subtitle: String
            get() = localized("mail.template.reset.password.subtitle")
        override val link: String
            get() = localized("mail.template.reset.password.link")
    },
    RESET_PASSWORD_SUCCESS_TR {
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
        override val label: String
            get() = "mail.template.reset.password.success.subject"
        override val value: String
            get() = "default-template-with-actions"
        override val header: String
            get() = localized("mail.template.reset.password.success.header")
        override val title: String
            get() = localized("mail.template.reset.password.success.title")
        override val subtitle: String
            get() = localized("mail.template.reset.password.success.subtitle")
        override val link: String
            get() = localized("mail.template.reset.password.success.link")
    },
    PASSWORD_CHANGE_SUCCESS_TR {
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
        override val label: String
            get() = "mail.template.password.change.success.subject"
        override val value: String
            get() = "default-template-with-actions"
        override val header: String
            get() = localized("mail.template.password.change.success.header")
        override val title: String
            get() = localized("mail.template.password.change.success.title")
        override val subtitle: String
            get() = localized("mail.template.password.change.success.subtitle")
        override val link: String
            get() = localized("mail.template.password.change.success.link")
    },
    ORDER_CONFIRMATION_TR {
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
        override val label: String
            get() = "mail.template.order.confirmation.subject"
        override val value: String
            get() = "default-template-with-actions"
        override val header: String
            get() = localized("mail.template.order.confirmation.header")
        override val title: String
            get() = localized("mail.template.order.confirmation.title")
        override val subtitle: String
            get() = localized("mail.template.order.confirmation.subtitle")
        override val link: String
            get() = localized("mail.template.order.confirmation.link")
    },
    ORDER_SHIPPED_TR {
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
        override val label: String
            get() = "mail.template.order.shipped.subject"
        override val value: String
            get() = "default-template-with-actions"
        override val header: String
            get() = localized("mail.template.order.shipped.header")
        override val title: String
            get() = localized("mail.template.order.shipped.title")
        override val subtitle: String
            get() = localized("mail.template.order.shipped.subtitle")
        override val link: String
            get() = localized("mail.template.order.shipped.link")
    },
    ORDER_DELIVERED_TR {
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
        override val label: String
            get() = "mail.template.order.delivered.subject"
        override val value: String
            get() = "default-template-with-actions"
        override val header: String
            get() = localized("mail.template.order.delivered.header")
        override val title: String
            get() = localized("mail.template.order.delivered.title")
        override val subtitle: String
            get() = localized("mail.template.order.delivered.subtitle")
        override val link: String
            get() = localized("mail.template.order.delivered.link")
    },
    ORDER_CANCELLED_TR {
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
        override val label: String
            get() = "mail.template.order.cancelled.subject"
        override val value: String
            get() = "default-template-with-actions"
        override val header: String
            get() = localized("mail.template.order.cancelled.header")
        override val title: String
            get() = localized("mail.template.order.cancelled.title")
        override val subtitle: String
            get() = localized("mail.template.order.cancelled.subtitle")
        override val link: String
            get() = localized("mail.template.order.cancelled.link")
    },
    ORDER_REFUNDED_TR {
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
        override val label: String
            get() = "mail.template.order.refunded.subject"
        override val value: String
            get() = "default-template-with-actions"
        override val header: String
            get() = localized("mail.template.order.refunded.header")
        override val title: String
            get() = localized("mail.template.order.refunded.title")
        override val subtitle: String
            get() = localized("mail.template.order.refunded.subtitle")
        override val link: String
            get() = localized("mail.template.order.refunded.link")
    },
    ORDER_RETURNED_TR {
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
        override val label: String
            get() = "mail.template.order.returned.subject"
        override val value: String
            get() = localized("default-template-with-actions")
        override val header: String
            get() = localized("mail.template.order.returned.header")
        override val title: String
            get() = localized("mail.template.order.returned.title")
        override val subtitle: String
            get() = localized("mail.template.order.returned.subtitle")
        override val link: String
            get() = localized("mail.template.order.returned.link")
    };

    abstract override val color: String?
    abstract override val icon: String?
    abstract override val label: String
    abstract override val value: String
    abstract val header: String
    abstract val title: String
    abstract val subtitle: String
    abstract val link: String
}

fun localized(value: String): String {
    return source().getMessage(value, null, locale())
}

private fun locale(): Locale {
    return LocaleContextHolder.getLocale()
}

private fun source(): MessageSource {
    return AppContextUtil.bean(MessageSource::class.java)
}