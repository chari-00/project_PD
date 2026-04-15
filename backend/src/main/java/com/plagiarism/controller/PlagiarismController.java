package com.plagiarism.controller;

import com.plagiarism.model.CompareRequest;
import com.plagiarism.model.PlagiarismResult;
import com.plagiarism.service.GeminiVlmService;
import com.plagiarism.service.PlagiarismService;
import com.plagiarism.service.TextPreprocessorService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PlagiarismController {

    private final PlagiarismService plagiarismService;
    private final TextPreprocessorService preprocessorService;
    private final GeminiVlmService geminiVlmService;

    public PlagiarismController(
            PlagiarismService plagiarismService,
            TextPreprocessorService preprocessorService,
            GeminiVlmService geminiVlmService) {
        this.plagiarismService = plagiarismService;
        this.preprocessorService = preprocessorService;
        this.geminiVlmService = geminiVlmService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Plagiarism Detector API"));
    }

    @PostMapping(value = "/compare", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> compareText(@RequestBody CompareRequest request) {
        if (request.getDocument1() == null || request.getDocument1().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "document1 must not be empty"));
        }
        if (request.getDocument2() == null || request.getDocument2().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "document2 must not be empty"));
        }

        PlagiarismResult result = plagiarismService.analyze(
                request.getDocument1(),
                request.getDocument2(),
                request.isRemoveStopWords()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/compare/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> compareFiles(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2,
            @RequestParam(value = "removeStopWords", defaultValue = "false") boolean removeStopWords) {

        if (file1.isEmpty() || file2.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Both files must be non-empty"));
        }

        try {
            String text1 = new String(file1.getBytes(), StandardCharsets.UTF_8);
            String text2 = new String(file2.getBytes(), StandardCharsets.UTF_8);

            PlagiarismResult result = plagiarismService.analyze(text1, text2, removeStopWords);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to read uploaded files: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/vlm/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extractVisualText(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "file must be non-empty"));
        }

        try {
            String extractedText = geminiVlmService.extractText(file);
            return ResponseEntity.ok(Map.of(
                    "fileName", file.getOriginalFilename(),
                    "mimeType", file.getContentType(),
                    "characterCount", extractedText.length(),
                    "extractedText", extractedText
                ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "VLM extraction failed: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/compare/visual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> compareVisualFiles(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2,
            @RequestParam(value = "removeStopWords", defaultValue = "false") boolean removeStopWords) {

        if (file1 == null || file1.isEmpty() || file2 == null || file2.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Both files must be non-empty"));
        }

        try {
            String text1 = geminiVlmService.extractText(file1);
            String text2 = geminiVlmService.extractText(file2);

            PlagiarismResult analysis = plagiarismService.analyze(text1, text2, removeStopWords);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("analysis", analysis);
            response.put("document1Text", text1);
            response.put("document2Text", text2);
            response.put("document1Chars", text1.length());
            response.put("document2Chars", text2.length());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Visual comparison failed: " + e.getMessage()));
        }
    }

    @PostMapping("/tokenize")
    public ResponseEntity<?> tokenize(@RequestBody Map<String, Object> body) {
        String text = (String) body.getOrDefault("text", "");
        boolean removeStopWords = Boolean.TRUE.equals(body.get("removeStopWords"));

        if (text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "text must not be empty"));
        }

        String[] tokens = preprocessorService.preprocess(text, removeStopWords);
        return ResponseEntity.ok(Map.of(
                "tokens", tokens,
                "count", tokens.length
        ));
    }
}
