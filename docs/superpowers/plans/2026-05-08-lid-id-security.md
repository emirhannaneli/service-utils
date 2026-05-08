# LID ID Güvenliği Uygulama Planı

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `LID` ve `BaseModel` içindeki bozuk `pk` üretimini, `SecureRandom` tabanlı, JS-safe (53-bit), çakışma-dirençli bir üretime taşımak; `LK` (sk) üretimini 51.7 bit'ten ~77.5 bit'e çıkarmak ve duplikasyonu tek üretim noktasında toplamak.

**Architecture:** `pk` üretimi `LID.newPk()` static fonksiyonunda toplanır (`SecureRandom.nextLong() and ((1L shl 53) - 1)`). `LK()` constructor 2 blok yerine 3 blok (`xxxxx-xxxxx-xxxxx`) üretir. `LID` ve `BaseModel` default constructor'ları `LID.newPk()` çağırır. `pk` ve `sk` JPA kolon uzunlukları büyütülür. Geriye uyumluluk: constructor imzaları korunur, eski DB kayıtları okumada bozulmaz.

**Tech Stack:** Kotlin 2.2.21, JUnit 5, AssertJ, Mockito, BouncyCastle (mevcut), `de.huxhorn.sulky.ulid`, JPA annotations.

**Spec:** `docs/superpowers/specs/2026-05-08-lid-id-security-design.md`

---

## File Structure

**Modify:**
- `src/main/kotlin/net/lubble/util/LK.kt` — `LK()` constructor 3 blok format
- `src/main/kotlin/net/lubble/util/model/LID.kt` — `newPk()` companion fonksiyonu, default constructor, `pk` ve `sk` `@Column(length=...)` güncellemesi, kullanılmayan import temizliği
- `src/main/kotlin/net/lubble/util/model/BaseModel.kt` — default constructor `LID.newPk()` çağırır, duplikasyon temizliği
- `build.gradle.kts` — version `1.24.13` → `2.0.0`

**Create:**
- `src/test/kotlin/net/lubble/util/LKTest.kt`
- `src/test/kotlin/net/lubble/util/model/LIDTest.kt`

---

### Task 1: `LK` 3-blok format için failing test

**Files:**
- Test: `src/test/kotlin/net/lubble/util/LKTest.kt`

- [ ] **Step 1: Test dosyasını oluştur**

```kotlin
package net.lubble.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LKTest {

    private val expectedFormat = Regex("^[a-z0-9]{5}-[a-z0-9]{5}-[a-z0-9]{5}$")

    @Test
    fun `default constructor produces 17 character three-block lowercase alphanumeric value`() {
        val lk = LK()

        assertThat(lk.value).hasSize(17)
        assertThat(lk.value).matches(expectedFormat)
    }

    @Test
    fun `default constructor produces unique values across many invocations`() {
        val values = (1..10_000).map { LK().value }.toSet()

        assertThat(values).hasSize(10_000)
    }

    @Test
    fun `string constructor preserves arbitrary input including legacy 11-char format`() {
        val legacy = "abcde-12345"

        val lk = LK(legacy)

        assertThat(lk.value).isEqualTo(legacy)
    }
}
```

- [ ] **Step 2: Testi çalıştır, fail etmesini doğrula**

Run: `./gradlew test --tests "net.lubble.util.LKTest"`
Expected: FAIL — `default constructor produces 17 character three-block...` testi `hasSize(17)` assertion'ı başarısız olur (mevcut LK 11 karakter üretir).

- [ ] **Step 3: `LK()` constructor'ını 3 blok yapacak şekilde güncelle**

`src/main/kotlin/net/lubble/util/LK.kt:16-28` aralığını şu şekilde değiştir:

```kotlin
    constructor() {
        val characters = "abcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom.getInstance("DEFAULT", "BC")

        key.clear()
        repeat(3) { i ->
            repeat(5) {
                key.append(characters[random.nextInt(characters.length)])
            }
            if (i < 2) key.append("-")
        }
        value = key.toString()
    }
```

(Tek değişiklik: `repeat(2) { i -> ... if (i < 1) ... }` → `repeat(3) { i -> ... if (i < 2) ... }`. Geri kalan satırlar aynı.)

- [ ] **Step 4: Testi tekrar çalıştır, geçtiğini doğrula**

Run: `./gradlew test --tests "net.lubble.util.LKTest"`
Expected: PASS — üç test de yeşil.

- [ ] **Step 5: Commit**

```bash
git add src/test/kotlin/net/lubble/util/LKTest.kt src/main/kotlin/net/lubble/util/LK.kt
git commit -m "feat!: extend LK to 3-block 17-char format

Increase entropy from ~51.7 to ~77.5 bits. Generation moves from 2x5
lowercase alphanumeric blocks (xxxxx-xxxxx) to 3x5 (xxxxx-xxxxx-xxxxx).

BREAKING CHANGE: sk column must be widened from varchar(11) to varchar(17).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: `LID.newPk()` companion fonksiyonu için failing test

**Files:**
- Test: `src/test/kotlin/net/lubble/util/model/LIDTest.kt`

- [ ] **Step 1: Test dosyasını oluştur**

```kotlin
package net.lubble.util.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LIDTest {

    private val pkMax: Long = (1L shl 53) - 1   // 9_007_199_254_740_991

    @Test
    fun `newPk returns values within zero to two pow fifty three minus one inclusive`() {
        repeat(10_000) {
            val pk = LID.newPk()

            assertThat(pk).isBetween(0L, pkMax)
        }
    }

    @Test
    fun `newPk produces no collisions across one hundred thousand invocations`() {
        val pks = (1..100_000).map { LID.newPk() }.toSet()

        assertThat(pks).hasSize(100_000)
    }

    @Test
    fun `newPk never returns negative even at extreme distribution edges`() {
        // SecureRandom.nextLong() can produce Long.MIN_VALUE; mask must still keep result non-negative.
        repeat(100_000) {
            assertThat(LID.newPk()).isNotNegative()
        }
    }
}
```

- [ ] **Step 2: Testi çalıştır, fail etmesini doğrula**

Run: `./gradlew test --tests "net.lubble.util.model.LIDTest"`
Expected: FAIL — derleme hatası: `Unresolved reference 'newPk'`.

- [ ] **Step 3: `LID.kt` companion'a `newPk()` ekle**

`src/main/kotlin/net/lubble/util/model/LID.kt` içinde:

1. Import bloğuna ekle (alfabetik düzen): `import java.security.SecureRandom`
2. `companion object` bloğunu (mevcut `lid(model)` fonksiyonuyla birlikte) şu hale getir:

```kotlin
    companion object {
        private val SECURE_RANDOM by lazy { SecureRandom.getInstance("DEFAULT", "BC") }
        private const val PK_MASK: Long = (1L shl 53) - 1

        @JvmStatic
        fun newPk(): Long = SECURE_RANDOM.nextLong() and PK_MASK

        @JvmStatic
        fun lid(model: BaseModel): LID {
            return LID(
                id = model.getId(),
                pk = model.pk,
                sk = model.sk
            )
        }
    }
```

`SecureRandom`'ın `lazy` olması: `LK` companion init'inin BouncyCastle provider'ı register etmesini bekleyebilmek için. Aynı pattern.

- [ ] **Step 4: Testi tekrar çalıştır, geçtiğini doğrula**

Run: `./gradlew test --tests "net.lubble.util.model.LIDTest"`
Expected: PASS — üç test de yeşil.

- [ ] **Step 5: Commit**

```bash
git add src/test/kotlin/net/lubble/util/model/LIDTest.kt src/main/kotlin/net/lubble/util/model/LID.kt
git commit -m "feat: add LID.newPk() static helper using SecureRandom

Single source of truth for pk generation. Returns SecureRandom.nextLong()
masked to 53 bits, keeping output within JS Number.MAX_SAFE_INTEGER.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: `LID` default constructor'ı `newPk()` kullanacak şekilde değiştir + kolon uzunluklarını güncelle

**Files:**
- Modify: `src/main/kotlin/net/lubble/util/model/LID.kt`

- [ ] **Step 1: `LID` default constructor'ı için failing/regression test ekle**

`src/test/kotlin/net/lubble/util/model/LIDTest.kt` dosyasının sonuna (companion test class'ının içinde, mevcut testlerin altına) ekle:

```kotlin
    @Test
    fun `default LID constructor uses newPk masked range`() {
        val pkMax: Long = (1L shl 53) - 1

        repeat(1_000) {
            val lid = LID()

            assertThat(lid.pk).isBetween(0L, pkMax)
            assertThat(lid.sk.value).matches(Regex("^[a-z0-9]{5}-[a-z0-9]{5}-[a-z0-9]{5}$"))
            assertThat(lid.getId()).hasSize(26)
        }
    }
```

- [ ] **Step 2: Testi çalıştır, mevcut davranışla karşılaştır**

Run: `./gradlew test --tests "net.lubble.util.model.LIDTest.default LID constructor uses newPk masked range"`
Expected: Çoğu çağrıda PASS gibi görünebilir (eski `pk` üretimi de coincidentally `[0, 10^12]` aralığında); ama amaç kolonu güncellemeden önce davranışı kilitlemek. Bu testi bir kez çalıştırıp **devam et**, sonraki adımda gerçek değişikliği yapacağız.

- [ ] **Step 3: `LID.kt` default constructor + kolon uzunluklarını güncelle**

`src/main/kotlin/net/lubble/util/model/LID.kt` üzerinde üç değişiklik:

**3a.** `@Column` `pk` uzunluğu (line 30):

Eski:
```kotlin
    @Column(name = "pk", unique = true, nullable = false, updatable = false, length = 12)
```
Yeni:
```kotlin
    @Column(name = "pk", unique = true, nullable = false, updatable = false, length = 16)
```

**3b.** `@Column` `sk` uzunluğu (lines 39-46):

Eski:
```kotlin
    @Column(
        name = "sk",
        unique = true,
        updatable = false,
        nullable = false,
        length = 11,
        columnDefinition = "varchar(11)"
    )
```
Yeni:
```kotlin
    @Column(
        name = "sk",
        unique = true,
        updatable = false,
        nullable = false,
        length = 17,
        columnDefinition = "varchar(17)"
    )
```

**3c.** Default constructor (lines 55-62):

Eski:
```kotlin
    constructor() : this(
        id = ULID().nextULID(),
        pk = String.format(
            "%012d",
            abs(UUID.randomUUID().mostSignificantBits - (UUID.randomUUID().leastSignificantBits + System.currentTimeMillis())) % 1000000000000
        ).toLong(),
        sk = LK()
    )
```
Yeni:
```kotlin
    constructor() : this(
        id = ULID().nextULID(),
        pk = newPk(),
        sk = LK()
    )
```

**3d.** Kullanılmayan import'ları sil:

`LID.kt` dosyasının üst kısmında kalan kullanılmayanları kaldır:
- `import java.util.*` — sadece `UUID` için kullanılıyordu (artık yok); başka `*` referansı yoksa sil. **Dikkat:** `UUID` kullanımı kalktı ama `import java.util.*` başka bir tip için (örn. yok) gerekmiyorsa sil. Dosyada `UUID` dışında `java.util.*` kullanımı yok — sil.
- `import kotlin.math.abs` — sil.

- [ ] **Step 4: Tüm `LIDTest`'i çalıştır, hepsinin geçtiğini doğrula**

Run: `./gradlew test --tests "net.lubble.util.model.LIDTest"`
Expected: PASS — dört test de yeşil.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/net/lubble/util/model/LID.kt src/test/kotlin/net/lubble/util/model/LIDTest.kt
git commit -m "feat!: replace LID pk generation with SecureRandom-based newPk

Default constructor now delegates to LID.newPk() and produces JS-safe,
collision-resistant 53-bit values instead of a UUID/time arithmetic mix
constrained to 12 digits. Removes the abs(Long.MIN_VALUE) edge case and
the predictability introduced by currentTimeMillis().

BREAKING CHANGE: pk column length increased from 12 to 16; sk column
length increased from varchar(11) to varchar(17). Consumer services must
migrate their schemas.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: `BaseModel` default constructor'ı `LID.newPk()` kullanacak şekilde temizle

**Files:**
- Modify: `src/main/kotlin/net/lubble/util/model/BaseModel.kt`

- [ ] **Step 1: `BaseModel` default constructor için regression test ekle**

`src/test/kotlin/net/lubble/util/model/LIDTest.kt` sonuna ekle:

```kotlin
    @Test
    fun `default BaseModel constructor delegates pk to LID newPk`() {
        val pkMax: Long = (1L shl 53) - 1

        repeat(1_000) {
            val model = BaseModel()

            assertThat(model.pk).isBetween(0L, pkMax)
            assertThat(model.sk.value).matches(Regex("^[a-z0-9]{5}-[a-z0-9]{5}-[a-z0-9]{5}$"))
        }
    }
```

- [ ] **Step 2: Testi çalıştır, fail etmesini bekle**

Run: `./gradlew test --tests "net.lubble.util.model.LIDTest.default BaseModel constructor delegates pk to LID newPk"`
Expected: PASS gibi görünebilir (eski üretim çoğunlukla 12 hane, hâlâ `[0, 2^53-1]` içinde) — bu test refactor sonrası **regression guard** olarak kalır. Devam et.

- [ ] **Step 3: `BaseModel.kt` default constructor'ı güncelle**

`src/main/kotlin/net/lubble/util/model/BaseModel.kt` üzerinde:

**3a.** Default constructor (lines 88-95):

Eski:
```kotlin
    constructor() : this(
        id = ULID().nextULID(),
        pk = String.format(
            "%012d",
            abs(UUID.randomUUID().mostSignificantBits - (UUID.randomUUID().leastSignificantBits + System.currentTimeMillis())) % 1000000000000
        ).toLong(),
        sk = LK()
    )
```
Yeni:
```kotlin
    constructor() : this(
        id = ULID().nextULID(),
        pk = LID.newPk(),
        sk = LK()
    )
```

**3b.** Kullanılmayan import'ları sil:

`BaseModel.kt` üst kısmında:
- `import java.util.*` — `UUID` kullanımı kalktı, dosyada başka `java.util.*` referansı yok; sil. **Dikkat:** Eğer `Date` veya `UUID` dışında bir tip kullanılıyorsa kontrol et, yoksa sil.
- `import kotlin.math.abs` — sil.

- [ ] **Step 4: Tüm test suite'i çalıştır**

Run: `./gradlew test`
Expected: PASS — tüm yeni testler ve mevcut (varsa) testler yeşil.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/net/lubble/util/model/BaseModel.kt src/test/kotlin/net/lubble/util/model/LIDTest.kt
git commit -m "refactor: BaseModel default constructor reuses LID.newPk

Removes duplicated pk generation logic from BaseModel. Single source of
truth is now LID.newPk(). No behavioral difference beyond what landed in
the prior LID change.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: Sürümü `2.0.0`'a bump et ve build doğrula

**Files:**
- Modify: `build.gradle.kts:7`

- [ ] **Step 1: `build.gradle.kts` üzerinde version'ı güncelle**

Eski:
```kotlin
version = "1.24.13"
```
Yeni:
```kotlin
version = "2.0.0"
```

- [ ] **Step 2: Tam build çalıştır**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL — derleme yeşil, tüm testler geçer.

- [ ] **Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "chore: bump version to 2.0.0

BREAKING CHANGE: pk and sk schema column lengths increased (pk 12->16,
sk 11->17). Consumer services must run a schema migration before
upgrading.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Self-Review Notları

**Spec coverage:**
- pk SecureRandom + 53-bit mask → Task 2, Task 3
- sk 3-blok 77.5 bit → Task 1
- abs(Long.MIN_VALUE) yapısal olarak imkansız → Task 2 (mask), `newPk never returns negative` testi
- Tek üretim noktası (LID.newPk) → Task 2, Task 4
- Şema kolon uzunlukları → Task 3 (3a, 3b)
- Versiyon bump → Task 5
- Test'ler → her task TDD ile

**Type/sembol tutarlılığı:**
- `LID.newPk(): Long` — Task 2'de tanımlı, Task 3 ve Task 4'te aynı imza ile kullanılıyor.
- `PK_MASK: Long = (1L shl 53) - 1` — Task 2'de tanımlı, testlerde aynı değer (`pkMax`) ile karşılaştırılıyor.
- `LK()` 17-karakter format — Task 1'de tanımlı, Task 3 ve Task 4 testlerinde aynı regex.

**Placeholder taraması:** Yok. Tüm kod ve komutlar tam yazılı.

**Kapsam dışı (spec ile uyumlu, planda yok):**
- Tüketici servislerin migration script'leri (Flyway/Liquibase).
- ULID değişikliği.
- pk için DI/strategy override.
