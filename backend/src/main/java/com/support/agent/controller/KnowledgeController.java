package com.support.agent.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Add FAQ / knowledge-base articles. Content is chunked, embedded and stored in pgvector. */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    public record Article(@NotBlank String title, @NotBlank String content) {}

    private final VectorStore vectorStore;

    public KnowledgeController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostMapping
    public Map<String, Object> add(@Valid @RequestBody Article a) {
        List<Document> chunks = new TokenTextSplitter()
            .apply(List.of(new Document(a.content(), Map.of("title", a.title()))));
        vectorStore.add(chunks);
        return Map.of("chunks", chunks.size());
    }
}
