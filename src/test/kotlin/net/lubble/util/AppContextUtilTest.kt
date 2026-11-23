package net.lubble.util

import net.lubble.util.support.TestContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import java.util.Locale

class AppContextUtilTest {

    @AfterEach
    fun reset() {
        TestContext.reset()
        LocaleContextHolder.resetLocaleContext()
    }

    @Test
    fun `fails with a clear message when no context was initialized`() {
        assertThatThrownBy { AppContextUtil.bean(MessageSource::class.java) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("not initialized")
    }

    @Test
    fun `looks beans up by type name and both`() {
        TestContext.install {
            it.beanFactory.registerSingleton("first", StringBuilder("one"))
            it.beanFactory.registerSingleton("second", StringBuilder("two"))
        }

        assertThat(AppContextUtil.bean<StringBuilder>("first").toString()).isEqualTo("one")
        assertThat(AppContextUtil.bean("second", StringBuilder::class.java).toString()).isEqualTo("two")
        assertThat(AppContextUtil.beans(StringBuilder::class.java).keys).containsExactlyInAnyOrder("first", "second")
        assertThat(AppContextUtil.bean(MessageSource::class.java)).isNotNull()
    }

    @Test
    fun `wraps lookup failures and keeps the cause`() {
        TestContext.install()

        assertThatThrownBy { AppContextUtil.bean(Runnable::class.java) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("java.lang.Runnable")
            .hasCauseInstanceOf(NoSuchBeanDefinitionException::class.java)
        assertThatThrownBy { AppContextUtil.bean<Any>("missing") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("'missing'")
        assertThatThrownBy { AppContextUtil.bean("missing", Runnable::class.java) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `localize resolves known codes and echoes unknown ones`() {
        TestContext.install(mapOf("greeting" to "Merhaba"))

        assertThat(AppContextUtil.localize("greeting")).isEqualTo("Merhaba")
        assertThat(AppContextUtil.localize("some plain text")).isEqualTo("some plain text")
    }

    @Test
    fun `localize echoes the code when there is no context`() {
        assertThat(AppContextUtil.localize("greeting")).isEqualTo("greeting")
    }

    @Test
    fun `locale follows the locale context`() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr-TR"))

        assertThat(AppContextUtil.locale()).isEqualTo(Locale.forLanguageTag("tr-TR"))
    }

    @Test
    fun `spring is detected on the test classpath and detection can be reset`() {
        assertThat(SpringDetectionUtil.isSpringAvailable()).isTrue()
        assertThat(SpringDetectionUtil.isSpringSecurityAvailable()).isTrue()
        assertThat(SpringDetectionUtil.isSpringWebAvailable()).isTrue()
        assertThat(SpringDetectionUtil.isSpringDataAvailable()).isTrue()
        assertThat(SpringDetectionUtil.isSpringMessageSourceAvailable()).isTrue()
        assertThat(SpringDetectionUtil.isSpringLocaleContextHolderAvailable()).isTrue()

        SpringDetectionUtil.reset()

        assertThat(SpringDetectionUtil.isSpringAvailable()).isTrue()
    }
}
