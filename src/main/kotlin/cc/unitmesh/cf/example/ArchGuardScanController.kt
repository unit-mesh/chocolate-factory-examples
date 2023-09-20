package cc.unitmesh.cf.example

import cc.unitmesh.cf.code.CodeSplitter
import cc.unitmesh.cf.example.infra.SentenceTransformersEmbedding
import cc.unitmesh.store.ElasticsearchStore
import chapi.domain.core.CodeDataStruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/scanner/{systemId}/reporting")
class ArchGuardScanController(
    @Value("\${elasticsearch.uris}")
    private var elasticsearchUrl: String
) {
    // todo: add for custom api keys
    val store: ElasticsearchStore = ElasticsearchStore(elasticsearchUrl, indexName = "cf-example")

    val splitter: CodeSplitter = CodeSplitter()
    val embedding = SentenceTransformersEmbedding()

    @PostMapping("/class-items")
    fun saveClassItems(
        @PathVariable systemId: String,
        @RequestParam language: String,
        @RequestParam path: String,
        @RequestBody input: List<CodeDataStruct>,
    ): ResponseEntity<String> {
        val documents = input.map(splitter::split).flatten()
        val embeddings = documents.map { embedding.embed(it.text) }
        store.addAll(embeddings, documents)

        // return ok
        return ResponseEntity<String>("", HttpStatus.OK)
    }
}