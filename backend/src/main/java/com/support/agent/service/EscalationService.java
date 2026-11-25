package com.support.agent.escalation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EscalationService {
    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);
    private final EscalationRepository repo;

    public EscalationService(EscalationRepository repo) {
        this.repo = repo;
    }

    public Escalation create(String conversationId, String question, double confidence) {
        log.info("Escalating conversation {} (confidence {})", conversationId, confidence);
        // Hook point: notify a ticketing system / email / Slack here.
        return repo.save(new Escalation(conversationId, question, confidence));
    }
}
