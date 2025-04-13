import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    java
    jacoco
    checkstyle
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

checkstyle {
    toolVersion = "10.9.3"
    configProperties["checkstyleDir"] = file("config/checkstyle").absolutePath
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

val kafkaClientVersion: String by extra("3.8.1")
val springBootStarterVersion: String by extra("3.1.4")
val preLiquibaseVersion: String by extra("1.6.0")
val testContainerVersion: String by extra("1.20.5")
val kafkaTestVersion: String by extra("3.3.3")
val awaitilityVersion: String by extra("3.1.6")
val curatorVersion: String by extra("5.8.0")
val instancioVersion: String by extra("5.4.1")
val h2Version: String by extra("2.3.232")
val grpcVersion: String by extra("3.1.0.RELEASE")
val protobufVersion: String by extra("4.30.2")
val javaxVersion: String by extra("1.3.2")

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-web:$springBootStarterVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootStarterVersion")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("net.lbruun.springboot:preliquibase-spring-boot-starter:$preLiquibaseVersion")
    implementation("org.apache.curator:curator-x-async:$curatorVersion")
    implementation("org.apache.curator:curator-recipes:$curatorVersion")
    implementation("net.devh:grpc-client-spring-boot-starter:$grpcVersion")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("javax.annotation:javax.annotation-api:$javaxVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:$testContainerVersion")
    testImplementation("org.testcontainers:kafka:$testContainerVersion")
    testImplementation("org.springframework.kafka:spring-kafka-test:$kafkaTestVersion")
    testImplementation("org.awaitility:awaitility-proxy:$awaitilityVersion")
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("org.projectlombok:lombok")
    testImplementation("org.apache.curator:curator-test:$curatorVersion")
    testImplementation("org.instancio:instancio-junit:$instancioVersion")
    testImplementation("com.h2database:h2:$h2Version")

    compileOnly("org.projectlombok:lombok")

    annotationProcessor("org.projectlombok:lombok")

    testAnnotationProcessor("org.projectlombok:lombok")

    runtimeOnly("org.apache.kafka:kafka-clients:$kafkaClientVersion")
    runtimeOnly("org.liquibase:liquibase-core")
    runtimeOnly("org.postgresql:postgresql")

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
                    "**/grpc/protobufs/**"
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
    failFast = true
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