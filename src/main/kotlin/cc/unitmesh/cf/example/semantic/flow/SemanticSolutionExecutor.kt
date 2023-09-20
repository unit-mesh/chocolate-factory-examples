package cc.unitmesh.cf.example.semantic.flow

import cc.unitmesh.cf.core.dsl.Interpreter
import cc.unitmesh.cf.core.flow.SolutionExecutor
import cc.unitmesh.cf.core.flow.model.Answer
import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.cf.core.llms.LlmProvider
import cc.unitmesh.cf.example.semantic.CodeSemanticWorkflow
import cc.unitmesh.cf.example.semantic.context.SemanticVariableResolver
import cc.unitmesh.cf.example.semantic.model.ExplainQuery
import cc.unitmesh.cf.example.infra.SentenceTransformersEmbedding
import cc.unitmesh.nlp.embedding.EncodingTokenizer
import cc.unitmesh.nlp.embedding.OpenAiEncoding
import cc.unitmesh.rag.document.Document
import cc.unitmesh.rag.document.DocumentOrder
import cc.unitmesh.rag.store.EmbeddingStore
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable

class SemanticSolutionExecutor(
    private val completion: LlmProvider,
    private val store: EmbeddingStore<Document>,
    private val embedding: SentenceTransformersEmbedding,
    private val variables: SemanticVariableResolver,
) : SolutionExecutor<ExplainQuery> {
    companion object {
        val log: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(SemanticSolutionExecutor::class.java)!!
    }

    override val interpreters: List<Interpreter> = listOf()
    private val basePrompt = CodeSemanticWorkflow.EXECUTE.format()

    private val encodingTokenizer: EncodingTokenizer = OpenAiEncoding()

    override fun execute(solution: ExplainQuery): Flowable<Answer> {
        variables.putQuery(solution)
        // 将问题的英文、中文、假设式文档转换为向量
        val query = embedding.embed(solution.englishQuery)
        val originQuery = embedding.embed(solution.originLanguageQuery)
        val hypotheticalCode = embedding.embed(solution.hypotheticalCode)

        // 进行相似度匹配
        val hydeDocs = store.findRelevant(hypotheticalCode, 15, 0.65)
        val list = store.findRelevant(query, 15, 0.65)
        val originLangList = store.findRelevant(originQuery, 15, 0.65)

        // 删除重复的文档
        val relevantDocuments = (hydeDocs + list + originLangList)
            .distinctBy { it.embedded.text }
            .sortedByDescending { it.score }
            .take(15)

        // 根据 Token 的长度限制，动态调整代码片段的数量
        val codes: MutableList<Pair<Double, String>> = mutableListOf()
        relevantDocuments.forEach {
            codes.add(it.score to it.embedded.text)
            variables.putCode("", codes.map { it.second })
            val testPrompt = variables.compile(basePrompt)
            // todo: make 2048 configurable
            if (encodingTokenizer.encode(testPrompt).size >= 2560) {
                codes.removeAt(codes.size - 1)
                return@forEach
            }
        }

        // 根据 Lost In Middle 算法重新排序，即将 score 低的代码片段放在中间，score 高的放在两边
        val reorderCodes = DocumentOrder.lostInMiddleReorder(codes)

        variables.putCode("", reorderCodes.map { it.second })
        val finalPrompt = variables.compile(basePrompt)

        val messages = listOf(
            LlmMsg.ChatMessage(LlmMsg.ChatRole.User, finalPrompt),
        ).filter { it.content.isNotBlank() }

        log.info("Execute messages: {}", messages)
        val completion: Flowable<String> = completion.streamCompletion(messages)

        val debugInfo = """
            |```debug
            |查询条件：
            |
            |question: ${solution.question}
            |englishQuery: ${solution.englishQuery}
            |originLanguageQuery: ${solution.originLanguageQuery}
            |hypotheticalCode:
            |${solution.hypotheticalCode}
            |
            |代码片段：
            |
            |${reorderCodes.joinToString("\n") { "${it.first} ${it.second.split("\n").first()}" }}
            |
            |```
            |""".trimMargin()

        return Flowable.create({ emitter ->
            emitter.onNext(Answer(this.javaClass.name, debugInfo))
            completion
                .subscribe(
                    { result ->
                        val answer = Answer(this.javaClass.name, result)
                        emitter.onNext(answer)
                    },
                    { throwable: Throwable ->
                        emitter.tryOnError(throwable)
                    })
                {
                    emitter.onComplete()
                }
        }, BackpressureStrategy.BUFFER)
    }
}
