plugins {
    kotlin("jvm") version "2.4.21"
    `maven-publish`
}

group = "net.lubble"
version = "3.0.0" // x-release-please-version
description = "Lubble Utility Library"

// Spring Boot line the library is compiled and tested against. Everything Spring manages
// (Spring, Jackson, Reactor, Jakarta, JUnit, Mockito, ...) takes its version from this BOM, so the
// library never compiles against an API newer than what a consumer on the same Boot line runs.
val springBootVersion = "4.1.3"

// Published (`api`) dependencies carry explicit versions so the POM stays self-contained.
// Jackson follows the version managed by the Spring Boot BOM above.
val jacksonVersion = "3.1.8"
val aspectjVersion = "1.9.27"
val commonsLangVersion = "3.20.0"
val auth0JwtVersion = "4.6.2"
val googleApiClientVersion = "2.9.2"
val bouncyCastleVersion = "1.87"
val ulidVersion = "8.3.0"

repositories {
    mavenCentral()
}

dependencies {
    val springBootBom = platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
    compileOnly(springBootBom)
    testImplementation(springBootBom)

    api("com.auth0:java-jwt:$auth0JwtVersion")
    api("org.aspectj:aspectjweaver:$aspectjVersion")
    api("com.google.api-client:google-api-client:$googleApiClientVersion")
    api("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion")
    api("tools.jackson.core:jackson-core:$jacksonVersion")
    api("tools.jackson.module:jackson-module-kotlin:$jacksonVersion")
    api("tools.jackson.module:jackson-module-blackbird:$jacksonVersion")
    api("de.huxhorn.sulky:de.huxhorn.sulky.ulid:$ulidVersion")

    // Used at runtime by LJPAProjection (FieldUtils), so it must reach consumers.
    implementation("org.apache.commons:commons-lang3:$commonsLangVersion")

    compileOnly("io.projectreactor:reactor-core")
    compileOnly("org.springframework.boot:spring-boot-starter-data-mongodb")
    compileOnly("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    compileOnly("org.springframework.boot:spring-boot-starter-security")
    compileOnly("org.springframework.boot:spring-boot-starter-graphql")
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.data:spring-data-jpa")
    compileOnly("org.springframework.data:spring-data-commons")
    compileOnly("jakarta.validation:jakarta.validation-api")
    compileOnly("jakarta.persistence:jakarta.persistence-api")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.boot:spring-boot-starter-graphql")
    testImplementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    testImplementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    testImplementation("org.springframework.boot:spring-boot-starter-aspectj")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    // Consumers run on 17+ (Spring Boot 4 baseline); keep the published bytecode at 17.
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=warn", "-jvm-default=no-compatibility")
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "Emirman"
            val releasesRepoUrl = "https://repo.emirman.dev/releases/"
            val snapshotsRepoUrl = "https://repo.emirman.dev/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = System.getenv("REPO_USER") ?: ""
                password = System.getenv("REPO_KEY") ?: ""
            }
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/emirhannaneli/service-utils")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])

            artifact(tasks.named("kotlinSourcesJar"))

            pom {
                name = "service-utils"
                description = project.description
                url = "https://github.com/emirhannaneli/service-utils"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                scm {
                    url = "https://github.com/emirhannaneli/service-utils"
                    connection = "scm:git:https://github.com/emirhannaneli/service-utils.git"
                }
            }
        }
    }
}
