package net.lubble.util.support

import net.lubble.util.AppContextUtil
import org.springframework.context.support.StaticApplicationContext
import java.util.Locale

/** Seeds the static [AppContextUtil] with a small real context so message lookups work in unit tests. */
object TestContext {
    fun install(messages: Map<String, String> = emptyMap(), configure: (StaticApplicationContext) -> Unit = {}): StaticApplicationContext {
        val context = StaticApplicationContext()
        messages.forEach { (code, text) ->
            Locale.getAvailableLocales().forEach { context.staticMessageSource.addMessage(code, it, text) }
            context.staticMessageSource.addMessage(code, Locale.ROOT, text)
        }
        configure(context)
        context.refresh()
        AppContextUtil.initialize(context)
        return context
    }

    fun reset() {
        AppContextUtil.instance = null
    }
}
