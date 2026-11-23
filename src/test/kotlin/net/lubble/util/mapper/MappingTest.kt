package net.lubble.util.mapper

import net.lubble.util.DocumentRegistryHolder
import net.lubble.util.LK
import net.lubble.util.MapperRegistryHolder
import net.lubble.util.dto.RBase
import net.lubble.util.model.BaseDocumented
import net.lubble.util.model.BaseModel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl

class MappingTest {
    class Address {
        var city: String? = "Ankara"
        var zip: String? = "06000"
    }

    class User : BaseModel() {
        var name: String = "old"
        var age: Int? = 1
        var tags: MutableList<String> = mutableListOf("old")
        var attributes: MutableMap<String, String> = mutableMapOf("old" to "x")
        var address: Address = Address()
        var untouched: String = "keep"
        var owner: User? = null

        companion object {
            const val TABLE = "users"
        }
    }

    class AddressUpdate {
        var city: String? = null
        var zip: String? = null
    }

    class UserUpdate {
        var name: String? = null
        var age: Int? = null
        var tags: List<String>? = null
        var attributes: Map<String, String>? = null
        var address: AddressUpdate? = null
        var unknown: String? = "ignored"

        companion object {
            const val TABLE = "updates"
        }
    }

    class UserRead : RBase() {
        var name: String? = null
    }

    class UserDocument(ref: User? = null) : BaseDocumented<User>(ref) {
        var name: String? = null
        var owner: UserDocument? = null

        override fun mapping(source: User): BaseDocumented<User> {
            name = source.name
            owner = from(source.owner) { UserDocument() }
            return this
        }
    }

    private var mappings = 0
    private val mapper = object : BaseMapper<User, UserRead, UserUpdate> {
        override fun mapping(source: User): UserRead {
            mappings++
            return UserRead().apply { name = source.name }
        }
    }

    @AfterEach
    fun clear() = MapperRegistryHolder.clear()

    @Test
    fun `update dto is copied onto the entity field by field`() {
        val user = User()
        val update = UserUpdate().apply {
            name = "new"
            age = 30
            tags = listOf("a", "b")
            attributes = mapOf("k" to "v")
            address = AddressUpdate().apply { city = "İzmir" }
        }

        mapper.map(update, user)

        assertThat(user.name).isEqualTo("new")
        assertThat(user.age).isEqualTo(30)
        assertThat(user.tags).containsExactly("a", "b")
        assertThat(user.attributes).containsExactly(java.util.Map.entry("k", "v"))
        // Nested objects are merged: fields the update leaves null keep their value.
        assertThat(user.address.city).isEqualTo("İzmir")
        assertThat(user.address.zip).isEqualTo("06000")
        assertThat(user.untouched).isEqualTo("keep")
    }

    @Test
    fun `values of the same type are assigned rather than merged`() {
        class Stamped : BaseModel() {
            var at: java.time.Instant = java.time.Instant.EPOCH
            var kind: java.time.DayOfWeek = java.time.DayOfWeek.MONDAY
            var count: Long = 1
        }

        class StampedUpdate {
            var at: java.time.Instant? = java.time.Instant.ofEpochSecond(60)
            var kind: java.time.DayOfWeek? = java.time.DayOfWeek.FRIDAY
            var count: Long? = 5
        }

        val entity = Stamped()
        object : BaseMapper<Stamped, UserRead, StampedUpdate> {
            override fun mapping(source: Stamped) = UserRead()
        }.map(StampedUpdate(), entity)

        assertThat(entity.at).isEqualTo(java.time.Instant.ofEpochSecond(60))
        assertThat(entity.kind).isEqualTo(java.time.DayOfWeek.FRIDAY)
        assertThat(entity.count).isEqualTo(5)
    }

    @Test
    fun `null values in the update leave the entity untouched`() {
        val user = User()

        mapper.map(UserUpdate(), user)

        assertThat(user.name).isEqualTo("old")
        assertThat(user.age).isEqualTo(1)
        assertThat(user.tags).containsExactly("old")
        assertThat(user.address.city).isEqualTo("Ankara")
    }

    @Test
    fun `static members such as companions are never copied`() {
        // Both classes declare a companion: copying it would try to overwrite a static final field.
        mapper.map(UserUpdate().apply { name = "new" }, User())
    }

    @Test
    fun `mapping an entity fills the base fields and is cached per request`() {
        val user = User().apply { name = "ada"; archived = true }

        val first = mapper.map(user)
        val second = mapper.map(user)

        assertThat(first).isSameAs(second)
        assertThat(mappings).isEqualTo(1)
        assertThat(first.name).isEqualTo("ada")
        assertThat(first.pk).isEqualTo(user.pk)
        assertThat(first.sk).isEqualTo(user.sk)
        assertThat(first.archived).isTrue()
        assertThat(first.deleted).isFalse()
        assertThat(first.createdAt).isEqualTo(user.createdAt)
        assertThat(first.updatedAt).isEqualTo(user.updatedAt)
    }

    @Test
    fun `updating an entity invalidates its cached dto`() {
        val user = User().apply { name = "ada" }
        mapper.map(user)

        mapper.map(UserUpdate().apply { name = "grace" }, user)

        assertThat(mapper.map(user).name).isEqualTo("grace")
        assertThat(mappings).isEqualTo(2)
    }

    @Test
    fun `collections and pages are mapped element by element`() {
        val users = listOf(User().apply { name = "a" }, User().apply { name = "b" })

        assertThat(mapper.map(users).map { it.name }).containsExactly("a", "b")
        assertThat(mapper.map(PageImpl(users)).map { it.name }).containsExactly("a", "b")
    }

    @Test
    fun `lid exposes only the public identifiers`() {
        val user = User()

        val lid = mapper.lid(user)

        assertThat(lid.pk).isEqualTo(user.pk)
        assertThat(lid.sk).isEqualTo(user.sk)
        assertThat(lid.getId()).isNotEqualTo(user.getId())
    }

    @Test
    fun `document mapping must be overridden to be used`() {
        assertThatThrownBy { mapper.map(UserDocument()) }.isInstanceOf(NotImplementedError::class.java)
    }

    @Test
    fun `document mappers map single documents collections and pages`() {
        val documentMapper = object : BaseMapper<User, UserRead, UserUpdate> {
            override fun mapping(source: User) = UserRead().apply { name = source.name }
            override fun <D : BaseDocumented<User>> mapping(source: D) = UserRead().apply { name = (source as UserDocument).name }
        }
        val documents = listOf(UserDocument().apply { name = "a" }, UserDocument().apply { name = "b" })

        assertThat(documentMapper.map(documents[0]).name).isEqualTo("a")
        assertThat(documentMapper.map(documents[0]).pk).isEqualTo(documents[0].pk)
        assertThat(documentMapper.map(documents).map { it.name }).containsExactly("a", "b")
        assertThat(documentMapper.map(PageImpl(documents)).map { it.name }).containsExactly("a", "b")
    }

    @Test
    fun `projection mapper maps and caches projections`() {
        var calls = 0
        val projectionMapper = object : BaseProjectionMapper<User, User, UserRead, UserUpdate>() {
            override val mapper get() = throw UnsupportedOperationException()
            override fun pmapping(source: User): UserRead {
                calls++
                return UserRead().apply { name = "p:" + source.name }
            }

            override fun mapping(source: User) = UserRead().apply { name = source.name }
        }
        val user = User().apply { name = "ada" }

        assertThat(projectionMapper.pmap(user)).isSameAs(projectionMapper.pmap(user))
        assertThat(calls).isEqualTo(1)
        assertThat(projectionMapper.pmap(user).name).isEqualTo("p:ada")
        assertThat(projectionMapper.pmap(user).pk).isEqualTo(user.pk)
        assertThat(projectionMapper.pmap(listOf(User())).single().name).isEqualTo("p:old")
        assertThat(projectionMapper.pmap(PageImpl(listOf(User()))).single().name).isEqualTo("p:old")
    }

    @Test
    fun `documents are only built inside a map session`() {
        val document: Any? = BaseDocumented.from(User()) { UserDocument() }

        assertThat(document).isNull()
        assertThat(DocumentRegistryHolder.hasContext()).isFalse()
    }

    @Test
    fun `a map session copies identity builds each document once and survives cycles`() {
        val owner = User().apply { name = "owner" }
        val user = User().apply { name = "ada"; this.owner = owner; archived = true }
        owner.owner = user // cycle

        val (document, again) = BaseDocumented.mapSession {
            BaseDocumented.from(user) { UserDocument() } to BaseDocumented.from(user) { UserDocument() }
        }

        assertThat(document).isNotNull().isSameAs(again)
        assertThat(document!!.name).isEqualTo("ada")
        assertThat(document.getId()).isEqualTo(user.getId())
        assertThat(document.pk).isEqualTo(user.pk)
        assertThat(document.sk).isEqualTo(user.sk)
        assertThat(document.archived).isTrue()
        assertThat(document.ref).isNull()
        assertThat(document.owner!!.name).isEqualTo("owner")
        // The cycle back to the document under construction is cut instead of recursing forever.
        assertThat(document.owner!!.owner).isNull()
        assertThat(DocumentRegistryHolder.hasContext()).isFalse()
    }

    @Test
    fun `map sessions nest and always close`() {
        assertThatThrownBy {
            BaseDocumented.mapSession {
                BaseDocumented.mapSession { assertThat(DocumentRegistryHolder.current()).isNotNull() }
                assertThat(DocumentRegistryHolder.hasContext()).isTrue()
                throw IllegalStateException("boom")
            }
        }.isInstanceOf(IllegalStateException::class.java)

        assertThat(DocumentRegistryHolder.hasContext()).isFalse()
    }

    @Test
    fun `sk values survive mapping unchanged`() {
        val user = User()
        user.sk = LK("abcde-12345-fghij")

        assertThat(mapper.map(user).sk).isEqualTo(LK("abcde-12345-fghij"))
    }
}
