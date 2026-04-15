package com.plagiarism.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GeminiVlmService {

    private static final List<String> SUPPORTED_MIME_TYPES = List.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private static final long MAX_FILE_BYTES = 10L * 1024L * 1024L;

    private static final String EXTRACTION_PROMPT = """
            Extract all readable text from this document.
            Rules:
            - Return plain text only.
            - Keep logical paragraph breaks.
            - Do not return markdown, code fences, or explanations.
            - If text is unclear, infer the most likely word from context.
            """;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String modelName;

    public GeminiVlmService(
            ObjectMapper objectMapper,
            @Value("${gemini.api.key:}") String configuredApiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String configuredModel) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        Map<String, String> dotEnvValues = loadDotEnv();
        String envApiKey = System.getenv("GEMINI_API_KEY");
        String dotEnvApiKey = dotEnvValues.get("GEMINI_API_KEY");
        this.apiKey = firstNonBlank(configuredApiKey, envApiKey, dotEnvApiKey);

        String envModel = System.getenv("GEMINI_MODEL");
        String dotEnvModel = dotEnvValues.get("GEMINI_MODEL");
        this.modelName = firstNonBlank(configuredModel, envModel, dotEnvModel, "gemini-2.5-flash");
    }

    public String extractText(MultipartFile file) throws IOException, InterruptedException {
        ensureApiKeyConfigured();
        validateFile(file);

        String mimeType = resolveMimeType(file);
        String base64Data = Base64.getEncoder().encodeToString(file.getBytes());

        ObjectNode payload = buildRequestPayload(mimeType, base64Data);
        String responseBody = callGemini(payload.toString());

        return parseExtractedText(responseBody);
    }

    private void ensureApiKeyConfigured() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Gemini API key is missing. Set GEMINI_API_KEY in environment/.env or gemini.api.key in application properties."
            );
        }
    }

    private Map<String, String> loadDotEnv() {
        Map<String, String> values = new HashMap<>();

        Path[] candidates = new Path[] {
                Paths.get(".env"),
                Paths.get("..", ".env"),
                Paths.get("..", "code", ".env"),
                Paths.get("code", ".env")
        };

        for (Path candidate : candidates) {
            if (!Files.exists(candidate)) {
                continue;
            }

            try {
                List<String> lines = Files.readAllLines(candidate, StandardCharsets.UTF_8);
                for (String raw : lines) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }

                    int idx = line.indexOf('=');
                    if (idx <= 0) {
                        continue;
                    }

                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();

                    if ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    if (!key.isEmpty()) {
                        values.put(key, value);
                    }
                }
                break;
            } catch (IOException ignored) {
                // Fall back to remaining sources when .env cannot be read.
            }
        }

        return values;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must be non-empty");
        }

        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File is too large. Maximum supported size is 10MB");
        }

        String mimeType = resolveMimeType(file);
        if (!SUPPORTED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + mimeType + ". Supported: PDF, PNG, JPEG, WEBP"
            );
        }
    }

    private String resolveMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType.toLowerCase(Locale.ROOT).trim();
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return "application/octet-stream";
        }

        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }

        return "application/octet-stream";
    }

    private ObjectNode buildRequestPayload(String mimeType, String base64Data) {
        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode contents = root.putArray("contents");
        ObjectNode userContent = contents.addObject();
        ArrayNode parts = userContent.putArray("parts");

        parts.addObject().put("text", EXTRACTION_PROMPT);

        ObjectNode mediaPart = parts.addObject();
        ObjectNode inlineData = mediaPart.putObject("inlineData");
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64Data);

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", 0.0);
        generationConfig.put("maxOutputTokens", 8192);

        return root;
    }

    private String callGemini(String payloadJson) throws IOException, InterruptedException {
        String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + modelName
                + ":generateContent?key="
                + encodedApiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() >= 400) {
            String message = parseGeminiError(response.body());
            throw new IOException("Gemini API error (" + response.statusCode() + "): " + message);
        }

        return response.body();
    }

    private String parseExtractedText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");

        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IOException("Gemini returned no candidates");
        }

        StringBuilder text = new StringBuilder();
        JsonNode parts = candidates.get(0).path("content").path("parts");

        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.hasNonNull("text")) {
                    if (!text.isEmpty()) {
                        text.append('\n');
                    }
                    text.append(part.get("text").asText());
                }
            }
        }

        String cleaned = stripCodeFences(text.toString().trim());
        if (cleaned.isBlank()) {
            throw new IOException("Gemini returned empty extracted text");
        }

        return cleaned;
    }

    private String stripCodeFences(String text) {
        if (!text.startsWith("```")) {
            return text;
        }

        String cleaned = text.replaceFirst("^```[a-zA-Z0-9]*\\s*", "");
        cleaned = cleaned.replaceFirst("\\s*```$", "");
        return cleaned.trim();
    }

    private String parseGeminiError(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode message = root.path("error").path("message");
            if (!message.isMissingNode() && !message.asText().isBlank()) {
                return message.asText();
            }
        } catch (Exception ignored) {
            // Fall through and return raw payload if JSON parsing fails.
        }

        return responseBody == null || responseBody.isBlank() ? "Unknown Gemini error" : responseBody;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return "";
    }
}
