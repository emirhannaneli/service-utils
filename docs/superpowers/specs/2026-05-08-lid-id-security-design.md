# LID ID Güvenliği — Tasarım Dokümanı

**Tarih:** 2026-05-08
**Kapsam:** `net.lubble.util.model.LID`, `net.lubble.util.model.BaseModel`, `net.lubble.util.LK`
**Durum:** Onay bekliyor

## Problem

`LID.kt` ve `BaseModel.kt` aşağıdaki ifadeyle `pk` üretiyor:

```kotlin
abs(UUID.randomUUID().mostSignificantBits
    - (UUID.randomUUID().leastSignificantBits + System.currentTimeMillis())) % 1000000000000
```

Bu üretimin dört ayrı sorunu var:

1. **Tahmin edilebilirlik.** `currentTimeMillis()` üretime dahil edildiği için saldırgan kayıt oluşturma anının yakınlığını biliyorsa arama uzayını ciddi ölçüde daraltabilir. `pk` payload'larda frontend'e expose edildiği için bu IDOR/enumeration yüzeyi yaratır.
2. **Düşük entropi / çakışma.** `% 10^12` (≈ 2^40) sınırlaması doğum günü paradoksuyla **1M kayıtta %36 çakışma** olasılığı doğurur — pratikte unique değil.
3. **Aritmetik karıştırma.** İki ayrı `UUID.randomUUID()` parçasını çıkarma ve toplama ile karıştırmak, `SecureRandom`'un istatistiksel garantilerini bozar.
4. **`abs(Long.MIN_VALUE)` bug'ı.** `Math.abs(Long.MIN_VALUE) == Long.MIN_VALUE` (negatif). Nadir ama gerçek bir edge case.

`sk` (slug, URL'lerde kullanılıyor) `SecureRandom` ile düzgün üretiliyor ancak entropi düşük: `xxxxx-xxxxx`, 10 karakter × log₂(36) ≈ **51.7 bit**. URL enumeration için kabul edilebilir alt sınırın altında.

`id` (ULID) servis dışına expose edilmiyor; mevcut haliyle kalır.

`pk` ve `sk` üretim mantığı `LID.kt:55-62` ve `BaseModel.kt:88-95`'te **iki yerde duplicate**.

## Hedef

- `pk`: kriptografik olarak rastgele, çakışma-dirençli, JS `Number` güvenli aralığında (≤ 2^53-1).
- `sk`: ≥ 75-bit entropi, URL-friendly slug formatı korunarak.
- `abs(Long.MIN_VALUE)` durumu yapısal olarak imkansız hale getirilir.
- `pk` üretim mantığı **tek noktada** (`LID.newPk()`).

## Tasarım

### `pk` üretimi

```kotlin
companion object {
    private val SECURE_RANDOM by lazy { SecureRandom.getInstance("DEFAULT", "BC") }
    private const val PK_MASK = (1L shl 53) - 1

    @JvmStatic
    fun newPk(): Long = SECURE_RANDOM.nextLong() and PK_MASK
}
```

- **Mask 53 bit:** Sonuç her zaman `[0, 2^53-1]`. JS `Number.MAX_SAFE_INTEGER`'a tam denk; `RBase.pk` JSON'da `Number` olarak serialize edildiğinden hassasiyet kaybı olmaz.
- **Tek `nextLong()` çağrısı:** Aritmetik karıştırma yok, `SecureRandom`'un istatistiksel özellikleri korunur.
- **Negatif imkansız:** Mask sonucu mutlak olarak non-negative.
- **`currentTimeMillis()` kaldırıldı:** Zaman tabanlı tahmin yüzeyi yok.

**Çakışma direnci:** 53 bit (~9×10^15) → 100M kayıtta çakışma olasılığı ~5.5×10^-4; 1B kayıtta ~%5'in altında.

**BouncyCastle provider:** `LK` companion init'inde provider register ediliyor. Class load sırası bağımsız olsun diye `LID.SECURE_RANDOM` `lazy` ile başlatılır.

### `sk` üretimi

`LK()` constructor 2-blok yerine **3-blok** üretir:

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

- Format: `xxxxx-xxxxx-xxxxx` (15 random karakter + 2 tire = 17 karakter).
- Entropi: 15 × log₂(36) ≈ **77.5 bit**.
- Mevcut alfabe (`a-z0-9`) ve URL slug estetiği korunur.

### Constructor değişiklikleri

`LID` default constructor (`LID.kt:55-62`):

```kotlin
constructor() : this(
    id = ULID().nextULID(),
    pk = newPk(),
    sk = LK()
)
```

`BaseModel` default constructor (`BaseModel.kt:88-95`):

```kotlin
constructor() : this(
    id = ULID().nextULID(),
    pk = LID.newPk(),
    sk = LK()
)
```

Duplicate inline aritmetik kaldırılır. `kotlin.math.abs` ve `java.util.UUID` import'ları artık kullanılmıyorsa silinir.

### Şema değişiklikleri

| Alan | Önce | Sonra |
|---|---|---|
| `LID.pk` `@Column(length=...)` | `12` | `16` |
| `LID.sk` `@Column(length=..., columnDefinition=...)` | `length=11, columnDefinition="varchar(11)"` | `length=17, columnDefinition="varchar(17)"` |

JPA `@Column` length'i BIGINT için çoğu dialect'te metadata; teknik gereklilik olmasa da niyet açık olsun diye güncellenir.

### Geri uyumluluk

- **API yüzeyi:** `LID(id, pk, sk)`, `LID(pk, sk)`, `BaseModel(pk, sk)` constructor imzaları aynı kalır. Tüketici kod derlemeye devam eder.
- **Eski DB kayıtları:** `pk: Long` herhangi bir Long'u, `LK(String)` herhangi bir string'i kabul ettiği için eski kayıtların okuma path'i bozulmaz. Yeni kayıtlar uzun formatta, eski kayıtlar dokunulmaz; mixed-length kabul edilebilir.
- **Şema migration:** Tüketici servislerin sorumluluğunda. `pk` length 12→16, `sk` length 11→17.

### Versiyonlama

Şema migration breaking olduğu için **major bump → 2.0.0**. Conventional commit: `feat!:` (BREAKING CHANGE: `pk` ve `sk` kolon uzunlukları büyütüldü).

### Test

`src/test` henüz yok. Bu değişiklikle birlikte minimum birim test eklenir:

- `LID.newPk()` 10k çağrıda hep `[0, 2^53-1]` aralığında, hep non-negative.
- `LID.newPk()` 100k çağrıda 0 çakışma (sanity check).
- `LK()` üretimi tam 17 karakter; format `^[a-z0-9]{5}-[a-z0-9]{5}-[a-z0-9]{5}$` regex'ine uyar.

## Etkilenen Dosyalar

- `src/main/kotlin/net/lubble/util/model/LID.kt` — `newPk()` companion fonksiyonu, default constructor, kolon length, kullanılmayan import temizliği.
- `src/main/kotlin/net/lubble/util/model/BaseModel.kt` — default constructor (`LID.newPk()` kullanımı), kullanılmayan import temizliği.
- `src/main/kotlin/net/lubble/util/LK.kt` — `LK()` constructor 3-blok format.
- `src/test/kotlin/net/lubble/util/model/LIDTest.kt` *(yeni)*.
- `src/test/kotlin/net/lubble/util/LKTest.kt` *(yeni)*.
- Sürüm: `2.0.0` (build dosyası).

## Kapsam Dışı

- ID üretiminin tüketici servislerden override edilebilir hale getirilmesi (DI / strategy pattern).
- `pk`'nın string olarak serialize edilmesi (full 63-bit Long range için gerekli olurdu; mevcut JS Number yaklaşımı yeterli).
- ULID'nin (`id` alanı) değiştirilmesi.
- Tüketici servislerin migration script'lerinin yazılması.
