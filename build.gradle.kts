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

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

	implementation("com.theokanning.openai-gpt3-java:service:0.14.0")
	implementation("com.squareup.retrofit2:converter-jackson:2.9.0")
	implementation("ai.djl.huggingface:tokenizers:0.23.0")
	implementation("com.knuddels:jtokkit:0.6.1")
	implementation("org.apache.velocity:velocity-engine-core:2.3")
	implementation("io.reactivex.rxjava3:rxjava:3.1.7")

	// ArchGuard 背后的代码抽象模型 Chapi
	implementation("com.phodal.chapi:chapi-domain:2.1.3")

	// 核心库
	implementation("cc.unitmesh:cocoa-core:0.4.1")
	// 代码拆分
	implementation("cc.unitmesh:code-splitter:0.4.1")
	// Elastisearch 向量化存储，普通搜索
	implementation("cc.unitmesh:store-elasticsearch:0.4.1")
	// 本地化的 embedding
	// 用于每次更新代码，重新 embedding => CPU
	implementation("cc.unitmesh:sentence-transformers:0.4.1")

	runtimeOnly("com.h2database:h2")
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
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
