import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.1.3"
	id("io.spring.dependency-management") version "1.1.3"
	kotlin("jvm") version "1.8.22"
	kotlin("plugin.spring") version "1.8.22"
	kotlin("plugin.jpa") version "1.8.22"
	kotlin("plugin.serialization") version "1.8.22"
}

group = "cc.unitmesh"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

extra["springBootAdminVersion"] = "3.1.5"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
	implementation("de.codecentric:spring-boot-admin-starter-server")

	implementation("com.theokanning.openai-gpt3-java:service:0.14.0")
	implementation("com.squareup.retrofit2:converter-jackson:2.9.0")
	implementation("ai.djl.huggingface:tokenizers:0.23.0")
	implementation("com.knuddels:jtokkit:0.6.1")
	implementation("org.apache.velocity:velocity-engine-core:2.3")

	//     implementation 'cc.unitmesh:cocoa-core:0.2.3'
	implementation("cc.unitmesh:cocoa-core:0.2.3")
	implementation("cc.unitmesh:store-elasticsearch:0.2.3")
	implementation("cc.unitmesh:sentence-transformers:0.2.3")
	implementation("io.reactivex.rxjava3:rxjava:3.1.7")

	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	runtimeOnly("com.h2database:h2")
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
}

dependencyManagement {
	imports {
		mavenBom("de.codecentric:spring-boot-admin-dependencies:${property("springBootAdminVersion")}")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
