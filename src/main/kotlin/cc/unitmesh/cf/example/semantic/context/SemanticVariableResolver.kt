package cc.unitmesh.cf.example.semantic.context

import cc.unitmesh.cf.core.context.variable.VariableResolver
import cc.unitmesh.cf.example.semantic.model.ExplainQuery
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.springframework.stereotype.Component
import java.io.StringWriter

@Component
class SemanticVariableResolver() : VariableResolver<SemanticVariables> {
    override val velocityContext: VelocityContext = VelocityContext()
    override var variables: SemanticVariables? = SemanticVariables(
        englishQuery = "",
        originLanguageQuery = "",
        hypotheticalCode = "",
        relevantCode = listOf(),
    )

    fun putQuery(query: ExplainQuery) {
        velocityContext.put("question", query.question)
        velocityContext.put("englishQuery", query.englishQuery)
        velocityContext.put("originLanguageQuery", query.originLanguageQuery)
        velocityContext.put("hypotheticalCode", query.hypotheticalCode)
    }

    fun putCode(lang: String, code: List<String>) {
        velocityContext.put("relevantCode", code.map {
            "```$lang\n$it\n```\n"
        }.joinToString("\n"))
    }

    override fun compile(input: String): String {
        val sw = StringWriter()
        Velocity.evaluate(velocityContext, sw, "#" + this.javaClass.name, input)
        return sw.toString()
    }
}