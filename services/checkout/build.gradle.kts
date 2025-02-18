plugins {
    java
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
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
val testContainerVersion: String by extra("1.19.1")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootStarterVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootStarterVersion")
    implementation("org.springframework.kafka:spring-kafka")

    implementation("net.lbruun.springboot:preliquibase-spring-boot-starter:$preLiquibaseVersion")
    testImplementation("net.lbruun.springboot:preliquibase-spring-boot-starter:$preLiquibaseVersion")
    implementation("org.liquibase:liquibase-core")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.projectlombok:lombok")

    testImplementation("org.testcontainers:postgresql:$testContainerVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainerVersion")

    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.apache.kafka:kafka-clients:$kafkaClientVersion")

}

tasks.withType<Test> {
    useJUnitPlatform()
}
