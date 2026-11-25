package com.support.agent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class ChatDtos {
    private ChatDtos() {}

    public record ChatRequest(String conversationId, @NotBlank String message) {}

    public record Source(String title, double score) {}

    public record ChatResponse(String conversationId, String answer, double confidence,
                               boolean escalated, List<Source> sources) {}
}
