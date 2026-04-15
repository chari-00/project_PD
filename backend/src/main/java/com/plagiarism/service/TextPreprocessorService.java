package com.plagiarism.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class TextPreprocessorService {

    private static final Set<String> STOP_WORDS = new LinkedHashSet<>(Arrays.asList(
        "a", "an", "the", "and", "or", "but", "if", "while", "because", "as", "until", "of", "at",
        "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after",
        "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again",
        "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both",
        "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same",
        "so", "than", "too", "very", "can", "will", "just", "dont", "should", "now", "is", "am", "are",
        "was", "were", "be", "been", "being", "do", "does", "did", "have", "has", "had", "having", "i",
        "you", "he", "she", "it", "we", "they", "them", "this", "that", "these", "those"
    ));

    public String[] preprocess(String text, boolean removeStopWords) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }

        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isEmpty()) {
            return new String[0];
        }

        String[] tokens = normalized.split(" ");
        if (!removeStopWords) {
            return tokens;
        }

        return Arrays.stream(tokens)
                .filter(token -> !STOP_WORDS.contains(token))
                .toArray(String[]::new);
    }
}
