import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.adarshr.test-logger") version "4.0.0"
    id("com.autonomousapps.dependency-analysis") version "2.10.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

val kafkaClientVersion: String by extra("3.8.0")
val springBootStarterVersion: String by extra("3.1.4")
val preLiquibaseVersion: String by extra("1.6.0")
val testContainerVersion: String by extra("1.20.5")

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootStarterVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootStarterVersion")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("net.lbruun.springboot:preliquibase-spring-boot-starter:$preLiquibaseVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:$testContainerVersion")

    compileOnly("org.projectlombok:lombok")

    annotationProcessor("org.projectlombok:lombok")

    runtimeOnly("org.apache.kafka:kafka-clients:$kafkaClientVersion")
    runtimeOnly("org.liquibase:liquibase-core")
    runtimeOnly("org.postgresql:postgresql")

    testRuntimeOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}


val jacocoTestReport by tasks.getting(JacocoReport::class) {
    reports {
        html.required.set(true)
        xml.required.set(true)
        html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
    }

    afterEvaluate {
        // Define patterns excluded from test coverage
        classDirectories.setFrom(files(classDirectories.files.map { file ->
            fileTree(file).apply {
                exclude(
                    "**/Application**",
                )
            }
        }))
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(jacocoTestReport)
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

testlogger {
    theme = ThemeType.STANDARD
    showExceptions = true
    showStackTraces = true
    showFullStackTraces = false
    showCauses = true
    slowThreshold = 2000
    showSummary = true
    showSimpleNames = false
    showPassed = true
    showSkipped = true
    showFailed = true
    showOnlySlow = false
    showStandardStreams = false
    showPassedStandardStreams = true
    showSkippedStandardStreams = true
    showFailedStandardStreams = true
    logLevel = LogLevel.LIFECYCLE
}