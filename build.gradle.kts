plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.spring") version "2.4.20"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.4.20"
    id("com.google.cloud.tools.jib") version "3.5.4"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
}

group = "com.libri"
version = "0.0.1-SNAPSHOT"

val envProps: Map<String, String> =
    buildMap {
        val envFile = file("$rootDir/.env")
        if (envFile.exists()) {
            envFile.useLines { lines ->
                lines
                    .filter { it.isNotBlank() && !it.trimStart().startsWith("#") && it.contains("=") }
                    .forEach { line ->
                        val (key, value) = line.split("=", limit = 2)
                        put(key.trim(), value.trim())
                    }
            }
        }
    }

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("io.netty:netty-resolver-dns-native-macos::osx-aarch_64")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

openApi {
    outputDir.set(file("$projectDir/openapi"))
    outputFileName.set("openapi.json")
    customBootRun {
        environment.set(envProps)
        workingDir.set(layout.projectDirectory)
    }
}

jib {
    from {
        image = "gcr.io/distroless/java21-debian12"

        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }

    container {
        environment =
            mapOf(
                "APP_VERSION" to (System.getProperty("version") ?: "dev"),
            )
    }
}
