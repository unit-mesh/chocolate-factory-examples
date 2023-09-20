package cc.unitmesh.cf.example.semantic

import cc.unitmesh.cf.core.flow.Workflow
import cc.unitmesh.cf.core.flow.model.Answer
import cc.unitmesh.cf.core.flow.model.ChatWebContext
import cc.unitmesh.cf.core.flow.model.StageContext
import cc.unitmesh.cf.core.flow.model.WorkflowResult
import cc.unitmesh.cf.core.llms.LlmProvider
import cc.unitmesh.cf.example.semantic.context.SemanticVariableResolver
import cc.unitmesh.cf.example.semantic.flow.SemanticProblemAnalyzer
import cc.unitmesh.cf.example.semantic.flow.SemanticSolutionExecutor
import cc.unitmesh.cf.example.semantic.model.ExplainQuery
import cc.unitmesh.cf.example.infra.SentenceTransformersEmbedding
import cc.unitmesh.store.ElasticsearchStore
import io.reactivex.rxjava3.core.Flowable
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CodeSemanticWorkflow : Workflow() {
    @Value("\${elasticsearch.uris}")
    private lateinit var elasticsearchUrl: String

    @Autowired
    private lateinit var llmProvider: LlmProvider

    @Autowired
    private lateinit var variableResolver: SemanticVariableResolver

    val embedding = SentenceTransformersEmbedding()

    override val stages: LinkedHashMap<StageContext.Stage, StageContext>
        get() = linkedMapOf(
            ANALYSIS.stage to ANALYSIS,
            EXECUTE.stage to EXECUTE,
        )

    val domainName = "code-semantic"

    /**
     * 核心 workflow
     */
    override fun execute(prompt: StageContext, chatWebContext: ChatWebContext): Flowable<WorkflowResult> {
        val store = ElasticsearchStore(elasticsearchUrl, indexName = "cf-example")
        // 取最后一条消息
        val question = chatWebContext.messages.last().content
        // 生成语义化分析后的 DSL，包含问题、英文、中文、假设式文档（hypotheticalCode）
        val analyze = SemanticProblemAnalyzer(llmProvider)
            .analyze(domainName, question)

        // todo: send to local

        log.info("Semantic analyze: {}", analyze)

        // 执行本地查询，再发送到 LLM
        val answerFlowable: Flowable<Answer> =
            SemanticSolutionExecutor(llmProvider, store, embedding, variableResolver).execute(analyze)

        return toFlowableResult(answerFlowable)
    }

    companion object {
        val log: Logger = org.slf4j.LoggerFactory.getLogger(CodeSemanticWorkflow::class.java)
        val ANALYSIS: StageContext = StageContext(
            id = "SemanticAnalyze",
            stage = StageContext.Stage.Analyze,
            systemPrompt = """Your are a senior software developer, your job is to transpile user's question relative to codebase.
                |
                |- YOU MUST follow the DSL format.
                |- You MUST translate user's question into a DSL query.
                |- `englishQuery` is a reference to the document that you think is the answer to the question.
                |- `originLanguageQuery` 是从用户的问题中提取出的关键信息，以用于搜索引擎检索。
                |- `hypotheticalCode` is a code snippet that could hypothetically be returned by a code search engine as the answer.
                |- `hypotheticalCode` code snippet should be between 5 and 10 lines long
                | 
                |For examples:
                |
                |""".trimMargin(),
            examples = ExplainQuery.QAExamples,
        )
        val EXECUTE: StageContext = StageContext(
            id = "SemanticExecute",
            stage = StageContext.Stage.Execute,
            systemPrompt = """Your job is to answer a query about a codebase using the information above.
                |
                |You must use the following formatting rules at all times:
                |  - Provide only as much information and code as is necessary to answer the query and be concise
                |  - If you do not have enough information needed to answer the query, do not make up an answer
                |  - When referring to code, you must provide an example in a code block
                |  - Keep number of quoted lines of code to a minimum when possible
                |  - Basic markdown is allowed
                |  - If you have enough information, try your best to answer more details.
                |  - You MUST provide key process of your thinking,
                | 
                |相关的代码：
                |${'$'}{relevantCode} 
                |
                |
                |Take a deep breath, and start to answer the question.
                |
                |用户的问题：${'$'}{question}
                |
                |""".trimMargin()
        );
    }
}
