# Service Utils

Service Utils is a Kotlin toolkit that packages the shared building blocks we use across Spring Boot services. The modules are
written to slot into existing projects with minimal wiring so current applications can upgrade without sweeping refactors.

## Quick Start
1. Add the private Maven repository and dependency.
2. Enable the auto-configuration via `@EnableLubbleUtils` on a configuration class.
3. Provide a `messages.properties` bundle so the built-in responses can resolve localized texts.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven(url = "https://repo.emirman.dev")
    }
}

// build.gradle.kts
dependencies {
    implementation("net.lubble:service-utils:<version>")
}
```

```kotlin
@EnableLubbleUtils
@Configuration
class ServiceConfig
```

## Provided Modules
- **Response helpers** – `Response`, `PageResponse`, `GraphResponse` and `GraphPageResponse` keep REST and GraphQL payloads
  uniform and surface pagination metadata consistently.
- **Centralized exception handling** – `ExceptionModel` plus ready-to-use `@ControllerAdvice` classes translate business
  exceptions into localized HTTP responses and GraphQL errors.
- **Context and request utilities** – `AppContextUtil`, `RequestTool`, `ReFormat`, and cookie helpers simplify servlet and
  reactive request handling without leaking framework details to calling code.
- **Persistence helpers** – specification builders, projection utilities, and codecs (`LK`, `LKCodec`, `LKToStringConverter`)
  reduce boilerplate in Mongo and JPA repositories.
- **Security & configuration models** – strongly typed configuration classes for OAuth, cookies, and authentication make it
  easy to share defaults across services.

## Testing
Run the unit tests locally with:

```bash
./gradlew test
```

The suite covers response serialization, cookie helpers, request utilities, and enum handling to guard against regressions.

---

# Service Utils ```TR```

Service Utils, Spring Boot tabanlı servisler arasında paylaşılan yardımcı bileşenleri paketleyen Kotlin bir araç setidir. Modüller
mevcut projelere minimum ekleme ile uyum sağlar; böylece güncelleme için kapsamlı refaktörlere gerek kalmaz.

## Hızlı Başlangıç
1. Özel Maven deposunu ve bağımlılığı ekleyin.
2. Bir konfigürasyon sınıfına `@EnableLubbleUtils` ekleyerek otomatik yapılandırmayı aktifleştirin.
3. Yerelleştirilmiş mesajlar için `messages.properties` dosyasını sağlayın.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven(url = "https://repo.emirman.dev")
    }
}

// build.gradle.kts
dependencies {
    implementation("net.lubble:service-utils:<version>")
}
```

```kotlin
@EnableLubbleUtils
@Configuration
class ServiceConfig
```

## Sağlanan Modüller
- **Yanıt yardımcıları** – `Response`, `PageResponse`, `GraphResponse` ve `GraphPageResponse` REST ve GraphQL çıktılarını
  tutarlı hale getirir, sayfalama bilgisini aynı yapıda sunar.
- **Merkezi istisna yönetimi** – `ExceptionModel` ve hazır `@ControllerAdvice` sınıfları iş kuralları hatalarını yerelleştirilmiş
  HTTP/GraphQL yanıtlarına çevirir.
- **Context ve istek yardımcıları** – `AppContextUtil`, `RequestTool`, `ReFormat` ve çerez yardımcıları servlet ve reaktif
  istekleri çerçeve bağımlılığı sızdırmadan yönetmenizi sağlar.
- **Veri katmanı yardımcıları** – spesifikasyon, projeksiyon ve codec yapıları (`LK`, `LKCodec`, `LKToStringConverter`) Mongo ve
  JPA repository'lerinde tekrar eden kodu azaltır.
- **Güvenlik ve konfigürasyon modelleri** – OAuth, çerez ve kimlik doğrulama için tip güvenli ayar sınıfları servisler arasında
  ortak varsayılanları paylaşmayı kolaylaştırır.

## Testler
Yerel testleri çalıştırmak için:

```bash
./gradlew test
```

Test paketi yanıt serileştirmesi, çerez yardımcıları, istek yardımcıları ve enum davranışını kapsar; böylece geriye dönük
uyumluluk korunur.
