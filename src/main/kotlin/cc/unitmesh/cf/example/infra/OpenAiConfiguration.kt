package cc.unitmesh.cf.example.infra

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "openai")
class OpenAiConfiguration {
    lateinit var apiKey: String
    var apiHost: String? = null
}
