# Service Utils

[![CI](https://github.com/emirhannaneli/service-utils/actions/workflows/ci.yml/badge.svg)](https://github.com/emirhannaneli/service-utils/actions/workflows/ci.yml)

Utility library for Spring Boot services: base models, specifications, repositories, mappers,
controllers and a consistent error/response format.

Built and tested against **Spring Boot 4.1**, **Kotlin 2.4** and **Java 17+**.

## Installation

The library is published to two registries; use whichever suits you.

### GitHub Packages

GitHub requires authentication for Maven packages, even public ones. Create a personal access
token with the `read:packages` scope and expose it as `GITHUB_ACTOR` / `GITHUB_TOKEN`
(or as `gpr.user` / `gpr.key` Gradle properties).

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/emirhannaneli/service-utils")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("net.lubble:service-utils:<version>")
}
```

### repo.emirman.dev

```kotlin
repositories {
    maven {
        url = uri("https://repo.emirman.dev/releases/")
    }
}

dependencies {
    implementation("net.lubble:service-utils:<version>")
}
```

## Usage

```kotlin
@SpringBootApplication
@EnableLubbleUtils
class Application
```

## Configuration

| Property | Default | Description |
| --- | --- | --- |
| `lubble.exception-handling` | `true` | Registers the built-in `@ControllerAdvice` exception handlers. |
| `lubble.expose-exception-details` | `false` | Adds the exception class and message to HTTP 500 responses. Leave it off in production: those messages carry SQL, class names and other internals. The error is always logged. |
| `lubble.pagination.max-size` | `1000` | Upper bound for the `size` request parameter, so a single request cannot load a whole table. Larger values are reduced to it. |
| `lubble.security.cookie.prefix` | empty | Prefix of the cookie names in `CookieType`. |
| `lubble.oauth2.enabled` | `true` | Registers `OAuthTool`. |
| `lubble.oauth2.google.client-id` | empty | Audience expected in Google ID tokens. |
| `lubble.oauth2.google.issuers` | Google's issuers | Accepted issuers of Google ID tokens. |

## Development

```shell
./gradlew build                 # compile and run the test suite
./gradlew publishToMavenLocal   # try a local build in another project
```

## Releasing

Versions follow [Semantic Versioning](https://semver.org) and are derived from
[Conventional Commits](https://www.conventionalcommits.org):

| Commit | Version bump |
| --- | --- |
| `fix: ...` | patch |
| `feat: ...` | minor |
| `feat!: ...` or a `BREAKING CHANGE:` footer | major |

Every push to `main` updates a release pull request (version bump + `CHANGELOG.md`).
Merging it creates the tag and GitHub release and publishes the package to GitHub Packages, and to
`repo.emirman.dev` when the `REPO_USER` / `REPO_KEY` repository secrets are set.

# Service Utils ```EN```

`Service Utils` is a comprehensive library designed to enhance and streamline development within Spring Boot projects. It provides a standardized and efficient framework with the following key features:

## Key Features

1. **Streamlined Development**:
   - Provides a cohesive structure for models, specifications, services, repositories, and DTO (Data Transfer Object) classes.
   - Makes the development process more efficient and maintainable.

2. **Flexible Structure**:
   - Offers flexibility in organizing various components.
   - Allows adaptation to the specific needs of projects while maintaining a consistent and logical hierarchy.

3. **Context Improvements**:
   - Enhances context handling to manage contextual information effectively throughout the application lifecycle.

4. **Automated Exception Handling**:
   - Includes mechanisms for automatic exception handling.
   - Ensures consistent management of exceptions across the project and reduces boilerplate code.

5. **Structured Notifications**:
   - Provides a standardized approach to handling and logging notifications.
   - Ensures clear and consistent communication of important events and errors.

`Service Utils` serves as a foundational framework that simplifies and accelerates development, promotes best practices, and helps maintain high code quality and organization in Spring Boot projects.


# Service Utils ```TR```

`Service Utils`, Spring Boot projelerinde geliştirme sürecini hızlandırmak ve standartlaştırmak için tasarlanmış kapsamlı bir kütüphanedir. Bu kütüphane, aşağıdaki temel özellikleri sunarak projelerin daha verimli ve düzenli bir şekilde yönetilmesine yardımcı olur:

## Temel Özellikler

1. **Geliştirilmiş Geliştirme Süreci**:
   - Modeller, spesifikasyonlar, servisler, repository'ler ve DTO (Data Transfer Object) sınıfları için uyumlu bir yapı sağlar.
   - Geliştirme sürecini daha verimli ve sürdürülebilir hale getirir.

2. **Esnek Yapı**:
   - Çeşitli bileşenlerin nasıl organize edileceği konusunda esneklik sunar.
   - Projelerin özel ihtiyaçlarına uyum sağlar ve tutarlı bir hiyerarşi sağlar.

3. **Kapsam İyileştirmeleri**:
   - Kapsam bilgilerini etkili bir şekilde yönetir ve uygulama yaşam döngüsü boyunca iyi bir bağlam yönetimi sağlar.

4. **Otomatik İstisna Yönetimi**:
   - İstisnaları otomatik olarak işleyen mekanizmalar içerir.
   - İstisnaların projede tutarlı bir şekilde yönetilmesini sağlar ve fazla kod yazımını azaltır.

5. **Yapılandırılmış Bilgilendirme**:
   - Önemli olaylar ve hataları net ve tutarlı bir şekilde iletmek için standart bir yol sunar.
   - Bilgilendirme ve günlükleme işlemlerini düzenli bir şekilde yönetir.

`Service Utils`, Spring Boot projelerinde temel bir yapı sağlar, geliştirme sürecini hızlandırır ve kod kalitesini yüksek tutar.
