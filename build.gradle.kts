plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.jpa") version "1.9.25"
	id("org.springframework.boot") version "3.5.7"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.beanbliss"
version = "0.0.1-SNAPSHOT"
description = "Bean Bliss Coffee E-commerce Platform"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot Starters
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// Kotlin
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Database
	runtimeOnly("com.mysql:mysql-connector-j")

	// Swagger / OpenAPI
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.mockk:mockk:1.13.8")
	testImplementation("com.ninja-squad:springmockk:4.0.2")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

// Docker Compose tasks for test environment
val dockerComposeFile = "docker-compose.test.yml"

val dockerComposeUp = tasks.register<Exec>("dockerComposeUp") {
	group = "docker"
	description = "Start Docker Compose services for testing"

	commandLine("docker-compose", "-f", dockerComposeFile, "up", "-d")

	// 에러 무시 (이미 실행 중인 경우)
	isIgnoreExitValue = true

	doLast {
		println("✅ Docker Compose: MySQL test container started")
		println("⏳ Waiting for MySQL to be ready...")
		Thread.sleep(5000) // 5초 대기
	}
}

val dockerComposeDown = tasks.register<Exec>("dockerComposeDown") {
	group = "docker"
	description = "Stop Docker Compose services for testing"

	commandLine("docker-compose", "-f", dockerComposeFile, "down")

	doLast {
		println("✅ Docker Compose: MySQL test container stopped")
	}
}

val dockerComposeDownVolumes = tasks.register<Exec>("dockerComposeDownVolumes") {
	group = "docker"
	description = "Stop Docker Compose services and remove volumes"

	commandLine("docker-compose", "-f", dockerComposeFile, "down", "-v")

	doLast {
		println("✅ Docker Compose: MySQL test container stopped and volumes removed")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()

	// 테스트 실행 전 Docker Compose 시작
	dependsOn(dockerComposeUp)

	// 테스트 실행 후 Docker Compose 종료
	// finalizedBy(dockerComposeDown)

	doFirst {
		println("🧪 Running tests with Docker MySQL on port 3307")
	}
}
