package cc.unitmesh.cf.example.infra

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.cf.core.llms.LlmProvider
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.service.OpenAiService
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.springframework.stereotype.Component
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import java.time.Duration

@Component
class OpenAiProvider(val config: OpenAiConfiguration) : LlmProvider {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(OpenAiProvider::class.java)
    }

    private val timeout = Duration.ofSeconds(600)

    override var temperature = 0.0

    var totalTokens = 0L;
    private val openai: OpenAiService by lazy {
        // for proxy
        if (config.apiHost != null) {
            val mapper = OpenAiService.defaultObjectMapper()
            val client = OpenAiService.defaultClient(config.apiKey, timeout)

            val host = config.apiHost!!.removeSurrounding("\"")

            val retrofit = Retrofit.Builder()
                .baseUrl(host)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

            val api = retrofit.create(OpenAiApi::class.java)
            OpenAiService(api)
        } else {
            OpenAiService(config.apiKey, Duration.ZERO)
        }
    }

    private fun prepareRequest(messages: List<LlmMsg.ChatMessage>): ChatCompletionRequest? {
        return ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .temperature(temperature)
            .messages(messages.map { it.toInternal() })
            .stream(false)
            .build()
    }

    fun LlmMsg.ChatMessage.toInternal() =
        com.theokanning.openai.completion.chat.ChatMessage(this.role.name.lowercase(), this.content)

    override fun completion(messages: List<LlmMsg.ChatMessage>): String {
        val request = prepareRequest(messages)

        var result = ""
        openai.streamChatCompletion(request)
            .doOnError {
                log.error("Completion failed: {}", it.message, it);
            }
            .blockingForEach { response ->
                val completion = response.choices[0].message
                if (completion != null && completion.content != null) {
                    result += completion.content
                }
            }

        return result
    }

    override fun streamCompletion(messages: List<LlmMsg.ChatMessage>): Flowable<String> {
        val request = prepareRequest(messages)

        return Flowable.create({ emitter ->
            val disposable = openai.streamChatCompletion(request)
                .doOnSubscribe {
                    log.info("Start completion: {}", messages)
                }
                .subscribe(
                    { response ->
                        val completion = response.choices[0].message
                        if (completion != null && completion.content != null) {
                            emitter.onNext(completion.content)
                        }
                    },
                    { error ->
                        log.error("Completion failed: {}", error.message, error)
                        emitter.onError(error)
                    },
                    {
                        emitter.onComplete()
                    }
                )

            emitter.setCancellable {
                // This will be called when the Flowable is unsubscribed
                disposable.dispose()
            }
        }, BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
    }

}