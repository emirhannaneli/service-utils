package net.lubble.util

import net.lubble.util.annotation.reformat.ReFormatProcessor
import net.lubble.util.codec.LKCodec
import net.lubble.util.constant.EnumConstant
import net.lubble.util.converter.ElasticConverter
import net.lubble.util.converter.LKToStringConverter
import net.lubble.util.dto.RBase
import net.lubble.util.exception.InvalidParamException
import net.lubble.util.filter.MapperRegistryCleanupFilter
import net.lubble.util.helper.EnumHelper
import net.lubble.util.model.BaseModel
import net.lubble.util.model.LID
import net.lubble.util.support.TestContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.BsonDocumentWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.Locale
import net.lubble.util.annotation.reformat.ReFormat as ReFormatAnnotation

class MiscTest {
    enum class Color(override val label: String, override val value: String) : EnumHelper {
        RED("color.red", "r"), BLUE("color.blue", "b")
    }

    @Suppress("unused")
    private class Annotated {
        @ReFormatAnnotation(upper = true, trim = true) val shout = ""
        @ReFormatAnnotation(lower = true, capitalize = true, locale = "tr") val title = ""
        @ReFormatAnnotation(upper = true, useContextLocale = true) val contextual = ""
        @ReFormatAnnotation(decapitate = true) val decapitated = ""
        @ReFormatAnnotation val untouched = ""
    }

    private fun annotation(field: String) = Annotated::class.java.getDeclaredField(field).getAnnotation(ReFormatAnnotation::class.java)

    @AfterEach
    fun reset() {
        TestContext.reset()
        LocaleContextHolder.resetLocaleContext()
        MapperRegistryHolder.clear()
    }

    // region ReFormat

    @Test
    fun `fluent reformat chains transformations`() {
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        assertThat(ReFormat("  hello World ").trim().upper().format()).isEqualTo("HELLO WORLD")
        assertThat(ReFormat("HELLO").lower().capitalize().format()).isEqualTo("Hello")
        assertThat(ReFormat("Hello").decapitate().format()).isEqualTo("hello")
    }

    @Test
    fun `fluent reformat honours explicit and context locales`() {
        val turkish = Locale.forLanguageTag("tr")

        assertThat(ReFormat("istanbul").upper(turkish).format()).isEqualTo("İSTANBUL")
        assertThat(ReFormat("ISPARTA").lower(turkish).format()).isEqualTo("ısparta")
        assertThat(ReFormat("istanbul").capitalize(turkish).format()).isEqualTo("İstanbul")
        assertThat(ReFormat("Istanbul").decapitate(turkish).format()).isEqualTo("ıstanbul")

        LocaleContextHolder.setLocale(turkish)
        assertThat(ReFormat("istanbul").upper().format()).isEqualTo("İSTANBUL")
    }

    @Test
    fun `annotation driven reformat applies every requested step in order`() {
        val processor = ReFormatProcessor()

        assertThat(processor.apply("  hello ", annotation("shout"))).isEqualTo("HELLO")
        assertThat(processor.apply("ISPARTA", annotation("title"))).isEqualTo("Isparta")
        assertThat(processor.apply("Hello", annotation("decapitated"))).isEqualTo("hello")
        assertThat(processor.apply(" As Is ", annotation("untouched"))).isEqualTo(" As Is ")

        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"))
        assertThat(processor.apply("istanbul", annotation("contextual"))).isEqualTo("İSTANBUL")
    }

    // endregion

    // region EnumHelper

    @Test
    fun `enum helper finds constants by name and value`() {
        assertThat(EnumHelper.findByName<Color>("RED")).isEqualTo(Color.RED)
        assertThat(EnumHelper.findByValue<Color>("b")).isEqualTo(Color.BLUE)
    }

    @Test
    fun `enum helper reports the accepted values for unknown input`() {
        TestContext.install(mapOf("global.exception.invalid.param" to "Invalid parameter"))

        assertThatThrownBy { EnumHelper.findByValue<Color>("x") }
            .isInstanceOfSatisfying(InvalidParamException::class.java) {
                assertThat(it.details()).isEqualTo(mapOf("param" to "Invalid enum value (Color)", "value" to "x", "desired" to listOf("r", "b")))
            }
        assertThatThrownBy { EnumHelper.findByName<Color>("red") }.isInstanceOf(InvalidParamException::class.java)
    }

    @Test
    fun `enum labels are localized when a message exists`() {
        assertThat(Color.RED.labelLocalized()).isEqualTo("color.red")

        TestContext.install(mapOf("color.red" to "Kırmızı"))

        assertThat(Color.RED.labelLocalized()).isEqualTo("Kırmızı")
        assertThat(Color.RED.toConstant()).isEqualTo(EnumConstant("Kırmızı", "r"))
        assertThat(Color.BLUE.toConstant()).isEqualTo(EnumConstant("color.blue", "b"))
    }

    // endregion

    // region LK / LID / BaseModel

    @Test
    fun `LK has value semantics`() {
        val a = LK("abcde-12345-fghij")

        assertThat(a).isEqualTo(LK("abcde-12345-fghij")).hasSameHashCodeAs(LK("abcde-12345-fghij"))
        assertThat(a).isNotEqualTo(LK("zzzzz-12345-fghij")).isNotEqualTo("abcde-12345-fghij")
        assertThat(a.toString()).isEqualTo("abcde-12345-fghij")
        assertThat(listOf(LK("b"), LK("a")).sorted()).containsExactly(LK("a"), LK("b"))
    }

    @Test
    fun `LK serializes as a plain json string`() {
        val mapper = JsonMapper.builder().build()

        assertThat(mapper.writeValueAsString(LK("abcde-12345-fghij"))).isEqualTo("\"abcde-12345-fghij\"")
        assertThat(mapper.writeValueAsString(RBase().apply { pk = 5; sk = LK("abcde-12345-fghij") }))
            .contains("\"sk\":\"abcde-12345-fghij\"").doesNotContain("archived").doesNotContain("deleted")
        assertThat(mapper.readValue("""{"pk":5,"sk":"abcde-12345-fghij"}""", RBase::class.java).sk).isEqualTo(LK("abcde-12345-fghij"))
    }

    @Test
    fun `LK converters round trip`() {
        val lk = LK("abcde-12345-fghij")
        val converter = LKToStringConverter()

        assertThat(converter.convertToDatabaseColumn(lk)).isEqualTo("abcde-12345-fghij")
        assertThat(converter.convertToEntityAttribute("abcde-12345-fghij")).isEqualTo(lk)
        assertThat(converter.write(lk)).isEqualTo("abcde-12345-fghij")
        assertThat(converter.write(7)).isEqualTo(7)
        assertThat(converter.read("abcde-12345-fghij")).isEqualTo(lk)
        assertThat(converter.read(7)).isEqualTo(7)
        assertThat(LKToStringConverter.Serializer().convert(lk)).isEqualTo("abcde-12345-fghij")
        assertThat(LKToStringConverter.Deserializer().convert("abcde-12345-fghij")).isEqualTo(lk)

        assertThat(ElasticConverter.LKToString().convert(lk)).isEqualTo("abcde-12345-fghij")
        assertThat(ElasticConverter.StringToLK().convert("abcde-12345-fghij")).isEqualTo(lk)
        assertThat(ElasticConverter.LongToInstantConverter().convert(1_000L)).isEqualTo(Instant.ofEpochMilli(1_000))
        assertThat(ElasticConverter.InstantToLongConverter().convert(Instant.ofEpochMilli(1_000))).isEqualTo(1_000L)
    }

    @Test
    fun `LK bson codec round trips`() {
        val codec = LKCodec()
        val document = BsonDocument()
        BsonDocumentWriter(document).apply {
            writeStartDocument()
            writeName("sk")
            codec.encode(this, LK("abcde-12345-fghij"), EncoderContext.builder().build())
            writeEndDocument()
        }

        val reader = BsonDocumentReader(document).apply { readStartDocument(); readName() }

        assertThat(codec.decode(reader, DecoderContext.builder().build())).isEqualTo(LK("abcde-12345-fghij"))
        assertThat(codec.encoderClass).isEqualTo(LK::class.java)
        assertThat(codec.lkConversions()).isNotNull()
    }

    @Test
    fun `LID matches by pk sk or their string forms`() {
        val lid = LID(42L, LK("abcde-12345-fghij"))

        assertThat(lid.getId()).hasSize(26)
        assertThat(lid.matches(42L)).isTrue()
        assertThat(lid.matches(43L)).isFalse()
        assertThat(lid.matches(LK("abcde-12345-fghij"))).isTrue()
        assertThat(lid.matches("42")).isTrue()
        assertThat(lid.matches("abcde-12345-fghij")).isTrue()
        assertThat(lid.matches("nope")).isFalse()
        assertThat(lid.matches(LID(42L, LK("abcde-12345-fghij")))).isTrue()
        assertThat(lid.matches(LID(42L, LK("zzzzz-12345-fghij")))).isFalse()
        assertThat(lid.matches(null as LID?)).isFalse()
    }

    @Test
    fun `LID equality includes the id`() {
        val model = BaseModel()
        val copy = LID(model)

        assertThat(copy.getId()).isEqualTo(model.getId())
        assertThat(copy).isEqualTo(LID.lid(model)).hasSameHashCodeAs(LID.lid(model))
        assertThat(copy).isEqualTo(BaseModel.from(model))
        assertThat(BaseModel.from(null)).isNull()
        assertThat(copy).isNotEqualTo(LID(model.pk, model.sk))

        copy.setId("changed")
        assertThat(copy.getId()).isEqualTo("changed")
    }

    @Test
    fun `new models get unique identifiers and sane defaults`() {
        val a = BaseModel()
        val b = BaseModel()

        assertThat(a.getId()).isNotEqualTo(b.getId())
        assertThat(a.pk).isNotEqualTo(b.pk)
        assertThat(a.sk).isNotEqualTo(b.sk)
        assertThat(a.deleted).isFalse()
        assertThat(a.archived).isFalse()
        assertThat(a).isNotEqualTo(b)

        val same = BaseModel(a.pk, a.sk)
        assertThat(same).isEqualTo(a).hasSameHashCodeAs(a)
        same.deleted = true
        assertThat(same).isNotEqualTo(a)
    }

    // endregion

    // region mapper registry

    @Test
    fun `mapper registry is identity based and can be invalidated`() {
        val key = BaseModel()
        val dto = RBase()

        MapperRegistryHolder.put(key, dto)

        assertThat(MapperRegistryHolder.get<RBase>(key)).isSameAs(dto)
        // An equal but different instance is a different key.
        assertThat(MapperRegistryHolder.get<RBase>(BaseModel(key.pk, key.sk))).isNull()

        MapperRegistryHolder.invalidate(key)
        assertThat(MapperRegistryHolder.get<RBase>(key)).isNull()
    }

    @Test
    fun `mapper registry is confined to its thread`() {
        val key = BaseModel()
        MapperRegistryHolder.put(key, RBase())

        var seenElsewhere: RBase? = RBase()
        Thread { seenElsewhere = MapperRegistryHolder.get(key) }.apply { start(); join() }

        assertThat(seenElsewhere).isNull()
    }

    @Test
    fun `cleanup filter clears the registry before and after the request even on failure`() {
        val key = BaseModel()
        MapperRegistryHolder.put(key, RBase())
        var seenInside: RBase? = RBase()
        val chain = MockFilterChain(object : jakarta.servlet.http.HttpServlet() {
            override fun service(req: jakarta.servlet.ServletRequest, res: jakarta.servlet.ServletResponse) {
                seenInside = MapperRegistryHolder.get(key)
                MapperRegistryHolder.put(key, RBase())
                throw IllegalStateException("boom")
            }
        })

        assertThatThrownBy { MapperRegistryCleanupFilter().doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain) }
            .isInstanceOf(IllegalStateException::class.java)

        assertThat(seenInside).isNull()
        assertThat(MapperRegistryHolder.get<RBase>(key)).isNull()
    }

    // endregion
}
