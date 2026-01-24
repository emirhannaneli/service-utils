plugins {
    kotlin("jvm") version "2.2.21"
    `maven-publish`
}

group = "net.lubble"
version = "1.23.0-SNAPSHOT"
description = "Lubble Utility Library"

val snakeYamlVersion = "2.5"
val auth0JwtVersion = "4.5.0"
val jodaTimeVersion = "2.14.0"
val aspectjVersion = "1.9.24"
val googleApiClientVersion = "2.8.1"
val reactorCoreVersion = "3.7.11"
val jacksonVersion = "3.0.4"
val springBootVersion = "4.0.2"
val springDataCommonsVersion = "4.0.2"
val springDataJpaVersion = "4.0.2"
val jakartaValidationVersion = "3.1.1"
val jakartaPersistenceVersion = "3.2.0"
val commonsLangVersion = "3.19.0"
val bounceCastleVersion = "1.82"
val ulidVersion = "8.3.0"
val kotlinVersion = "2.3.0"
val kotlinXVersion = "1.10.2"
val hibernateVersion = "7.2.1.Final"

repositories {
    mavenCentral()
}

dependencies {
    api("org.yaml:snakeyaml:$snakeYamlVersion")
    api("com.auth0:java-jwt:$auth0JwtVersion")
    api("joda-time:joda-time:$jodaTimeVersion")
    api("org.aspectj:aspectjrt:$aspectjVersion")
    api("org.aspectj:aspectjweaver:$aspectjVersion")
    api("com.google.api-client:google-api-client:$googleApiClientVersion")
    api("org.bouncycastle:bcprov-jdk18on:$bounceCastleVersion")
    api("tools.jackson.core:jackson-core:$jacksonVersion")
    api("tools.jackson.module:jackson-module-kotlin:$jacksonVersion")
    api("tools.jackson.module:jackson-module-blackbird:$jacksonVersion")
    api("tools.jackson.module:jackson-module-afterburner:$jacksonVersion")
    api("de.huxhorn.sulky:de.huxhorn.sulky.ulid:$ulidVersion")


    compileOnly("io.projectreactor:reactor-core:$reactorCoreVersion")

    compileOnly("org.springframework.boot:spring-boot-starter-data-mongodb:$springBootVersion")
    compileOnly("org.springframework.boot:spring-boot-starter-data-elasticsearch:$springBootVersion")
    compileOnly("org.springframework.boot:spring-boot-starter-security:$springBootVersion")
    compileOnly("org.springframework.boot:spring-boot-starter-graphql:$springBootVersion")
    compileOnly("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    compileOnly("org.springframework.data:spring-data-jpa:$springDataJpaVersion")
    compileOnly("org.springframework.data:spring-data-commons:$springDataCommonsVersion")
    compileOnly("jakarta.validation:jakarta.validation-api:$jakartaValidationVersion")
    compileOnly("org.apache.commons:commons-lang3:$commonsLangVersion")
    compileOnly("jakarta.persistence:jakarta.persistence-api:$jakartaPersistenceVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinXVersion")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")

    // Test dependencies
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
    testImplementation("org.assertj:assertj-core:3.25.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-security:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-data-elasticsearch:$springBootVersion")
    testImplementation("com.h2database:h2:2.2.224")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=warn", "-Xjvm-default=all")
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            val releasesRepoUrl = "https://repo.emirman.dev/releases/"
            val snapshotsRepoUrl = "https://repo.emirman.dev/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = System.getenv("REPO_USER") ?: ""
                password = System.getenv("REPO_KEY") ?: ""
            }
        }
    }
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])

            artifact(tasks.named("kotlinSourcesJar"))
        }
    }
}
