package com.capgemini.emaildraftingagent.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Email Drafting Agent API",
        version = "1.0.0",
        description = "AI-powered email analysis, task extraction, scheduling, and reply drafting"))
public class OpenApiConfiguration {
}
