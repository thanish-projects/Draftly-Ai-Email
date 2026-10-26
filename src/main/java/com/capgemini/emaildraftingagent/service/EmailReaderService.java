package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.dto.EmailReaderRequest;
import com.capgemini.emaildraftingagent.dto.EmailReaderResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class EmailReaderService {

    private static final Pattern SIGN_OFF = Pattern.compile(
            "(?i)^(best(?: regards)?|kind regards|warm regards|regards|sincerely|thank you|thanks|cheers|"
                    + "sent from my .+)[,!\\s.]*$");
    private static final Pattern SIGNATURE_SEPARATOR = Pattern.compile("^--\\s*$");
    private static final Pattern WHITESPACE = Pattern.compile("[\\t ]+");

    private final OpenAIService aiClient;

    public EmailReaderService(OpenAIService aiClient) {
        this.aiClient = aiClient;
    }

    public EmailReaderResponse read(EmailReaderRequest request) {
        EmailReaderContext result = analyzeForProcessing(request);
        return new EmailReaderResponse(result.intent(), result.summary());
    }

    public EmailReaderContext analyzeForProcessing(EmailReaderRequest request) {
        String cleanedEmail = cleanEmail(request.email());
        if (cleanedEmail.isBlank()) {
            throw new AiAnalysisException("Email content is empty after removing whitespace and signatures.");
        }

        EmailReaderAiResult analysis = aiClient.summarizeEmail(request.subject(), cleanedEmail);
        if (analysis.intent() == null || analysis.summary() == null || analysis.summary().isBlank()
                || analysis.importantSentences() == null) {
            throw new AiAnalysisException("The AI provider returned an incomplete email reading result.");
        }

        List<String> importantSentences = analysis.importantSentences().stream()
                .filter(sentence -> sentence != null && !sentence.isBlank())
                .map(String::strip)
                .toList();
        return new EmailReaderContext(analysis.intent(), analysis.summary().strip(),
                importantSentences, cleanedEmail);
    }

    static String cleanEmail(String email) {
        String normalized = email.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n");
        List<String> cleanedLines = new ArrayList<>();
        for (String line : lines) {
            String cleanedLine = WHITESPACE.matcher(line).replaceAll(" ").strip();
            if (isSignatureStart(cleanedLine) && !cleanedLines.isEmpty()) {
                break;
            }
            if (!cleanedLine.isEmpty() || (!cleanedLines.isEmpty()
                    && !cleanedLines.getLast().isEmpty())) {
                cleanedLines.add(cleanedLine);
            }
        }
        while (!cleanedLines.isEmpty() && cleanedLines.getLast().isEmpty()) {
            cleanedLines.removeLast();
        }
        return String.join("\n", cleanedLines);
    }

    private static boolean isSignatureStart(String line) {
        return SIGNATURE_SEPARATOR.matcher(line).matches() || SIGN_OFF.matcher(line).matches();
    }
}
