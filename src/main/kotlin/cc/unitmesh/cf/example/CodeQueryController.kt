package cc.unitmesh.cf.example

import cc.unitmesh.cf.core.flow.model.ChatWebContext
import cc.unitmesh.cf.core.flow.model.Message
import cc.unitmesh.cf.core.flow.model.WorkflowResult
import cc.unitmesh.cf.example.semantic.CodeSemanticWorkflow
import io.reactivex.rxjava3.core.Flowable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/code-query")
class CodeQueryController(val workflow: CodeSemanticWorkflow) {
    @PostMapping
    fun query(@RequestBody question: QuestionRequest): Flowable<WorkflowResult> {
        val stage = CodeSemanticWorkflow.ANALYSIS
        val webContext = ChatWebContext(
            messages = listOf(
                Message("user", question.question)
            ),
            id = UUID.randomUUID().toString(),
            stage = stage.stage
        )

        return workflow.execute(stage, webContext)
    }
}

data class QuestionRequest(
    val question: String,
)