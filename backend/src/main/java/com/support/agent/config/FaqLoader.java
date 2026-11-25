package com.support.agent.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Seeds the vector store with sample FAQs on first start (when it is empty). */
@Component
public class FaqLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(FaqLoader.class);
    private final VectorStore vectorStore;
    private final ObjectMapper mapper;

    public FaqLoader(VectorStore vectorStore, ObjectMapper mapper) {
        this.vectorStore = vectorStore;
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean empty = vectorStore
            .similaritySearch(SearchRequest.builder().query("help").topK(1).build()).isEmpty();
        if (!empty) {
            return;
        }
        List<Map<String, String>> faqs = mapper.readValue(
            new ClassPathResource("faqs.json").getInputStream(), new TypeReference<>() {});
        List<Document> docs = faqs.stream()
            .map(f -> new Document(f.get("title") + "\n" + f.get("content"),
                                   Map.<String, Object>of("title", f.get("title"))))
            .toList();
        vectorStore.add(docs);
        log.info("Seeded {} FAQ entries", docs.size());
    }
}
