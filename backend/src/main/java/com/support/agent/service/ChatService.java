package com.support.agent.service;

import com.support.agent.config.SupportProperties;
import com.support.agent.dto.ChatDtos.ChatRequest;
import com.support.agent.dto.ChatDtos.ChatResponse;
import com.support.agent.dto.ChatDtos.Source;
import com.support.agent.entity.ChatMessage;
import com.support.agent.repository.ChatMessageRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    static final String NO_ANSWER = "NO_ANSWER";
    static final String ESCALATION_REPLY =
        "I'm not confident I can answer that correctly, so I've passed your question to a human agent. "
        + "They will get back to you shortly.";

    private static final String SYSTEM =
        "You are a customer support assistant. Answer ONLY using the knowledge base context below.\n"
        + "Use the conversation history to understand follow-up questions.\n"
        + "If the context does not contain the answer, reply with exactly: " + NO_ANSWER + "\n\n"
        + "Context:\n";

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMessageRepository messages;
    private final EscalationService escalations;
    private final SupportProperties props;

    public ChatService(ChatClient.Builder builder, VectorStore vectorStore, ChatMessageRepository messages,
                       EscalationService escalations, SupportProperties props) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.messages = messages;
        this.escalations = escalations;
        this.props = props;
    }

    public ChatResponse chat(ChatRequest req) {
        String convId = req.conversationId() == null || req.conversationId().isBlank()
            ? UUID.randomUUID().toString() : req.conversationId();

        // 1. Semantic search over FAQ / knowledge base
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder().query(req.message()).topK(props.topK()).build());
        double confidence = docs.stream().mapToDouble(ChatService::score).max().orElse(0.0);

        // 2. Conversation history for follow-ups
        List<ChatMessage> recent = new ArrayList<>(
            messages.findByConversationIdOrderByIdDesc(convId, PageRequest.of(0, props.historySize())));
        Collections.reverse(recent);
        List<Message> history = new ArrayList<>();
        for (ChatMessage m : recent) {
            history.add("user".equals(m.getRole()) ? new UserMessage(m.getContent())
                                                   : new AssistantMessage(m.getContent()));
        }

        String context = docs.stream()
            .map(d -> "## " + d.getMetadata().get("title") + "\n" + d.getText())
            .collect(Collectors.joining("\n\n"));

        // 3. RAG generation (skipped when retrieval is clearly too weak)
        boolean lowConfidence = confidence < props.confidenceThreshold();
        String answer = lowConfidence ? NO_ANSWER
            : chatClient.prompt().system(SYSTEM + context)
                .messages(history).user(req.message()).call().content();

        // 4. Escalate unresolved queries
        boolean escalate = lowConfidence || answer == null || answer.contains(NO_ANSWER);
        if (escalate) {
            escalations.create(convId, req.message(), confidence);
            answer = ESCALATION_REPLY;
        }

        messages.save(new ChatMessage(convId, "user", req.message()));
        messages.save(new ChatMessage(convId, "assistant", answer));

        List<Source> sources = docs.stream()
            .map(d -> new Source(String.valueOf(d.getMetadata().get("title")), score(d))).toList();
        return new ChatResponse(convId, answer, confidence, escalate, sources);
    }

    /** Cosine similarity of the match, 0..1. The best match is used as the confidence score. */
    private static double score(Document d) {
        return d.getScore() == null ? 0.0 : d.getScore();
    }
}
