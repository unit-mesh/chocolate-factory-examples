package cc.unitmesh.cf.example.semantic.flow

import cc.unitmesh.cf.core.flow.ProblemAnalyzer
import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.cf.core.llms.LlmProvider
import cc.unitmesh.cf.example.semantic.CodeSemanticWorkflow
import cc.unitmesh.cf.example.semantic.model.ExplainQuery

class SemanticProblemAnalyzer(
    private val completion: LlmProvider
): ProblemAnalyzer {
    override fun analyze(domain: String, question: String): ExplainQuery {
        val stageContext = CodeSemanticWorkflow.ANALYSIS
        val systemPrompt = stageContext.format()

        val messages = listOf(
            LlmMsg.ChatMessage(LlmMsg.ChatRole.System, systemPrompt),
            LlmMsg.ChatMessage(LlmMsg.ChatRole.User, question),
        ).filter { it.content.isNotBlank() }

        val completion = completion.completion(messages)
        return ExplainQuery.parse(question, completion)
    }

}