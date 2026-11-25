package com.support.agent.escalation;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/escalations")
public class EscalationController {
    private final EscalationRepository repo;

    public EscalationController(EscalationRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Escalation> list() {
        return repo.findAll();
    }
}
