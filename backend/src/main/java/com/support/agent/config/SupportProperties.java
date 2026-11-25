package com.support.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "support")
public record SupportProperties(int topK, double confidenceThreshold, int historySize, String corsOrigin) {}
