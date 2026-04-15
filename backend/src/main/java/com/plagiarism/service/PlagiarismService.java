package com.plagiarism.service;

import com.plagiarism.model.PlagiarismResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlagiarismService {

    private static final int MAX_TOKENS_FOR_FULL_MATRIX = 2000;

    private final TextPreprocessorService preprocessor;
    private final LcsEngineService lcsEngine;

    public PlagiarismService(TextPreprocessorService preprocessor, LcsEngineService lcsEngine) {
        this.preprocessor = preprocessor;
        this.lcsEngine = lcsEngine;
    }

    public PlagiarismResult analyze(String doc1Text, String doc2Text, boolean removeStopWords) {
        String[] X = preprocessor.preprocess(doc1Text, removeStopWords);
        String[] Y = preprocessor.preprocess(doc2Text, removeStopWords);

        int n = X.length;
        int m = Y.length;

        PlagiarismResult result = new PlagiarismResult();
        result.setDoc1TokenCount(n);
        result.setDoc2TokenCount(m);
        result.setDoc1Tokens(X);
        result.setDoc2Tokens(Y);

        if (n == 0 || m == 0) {
            result.setLcsLength(0);
            result.setSimilarityPercentage(0.0);
            result.setPlagiarismLevel("MINIMAL");
            result.setMatchedSequence(List.of());
            return result;
        }

        boolean useFullMatrix = (n <= MAX_TOKENS_FOR_FULL_MATRIX && m <= MAX_TOKENS_FOR_FULL_MATRIX);

        int lcsLength;
        List<String> matchedSequence;

        if (useFullMatrix) {
            int[][] dp = lcsEngine.buildDpMatrix(X, Y);
            lcsLength = lcsEngine.getLcsLength(dp);
            matchedSequence = lcsEngine.backtrack(dp, X, Y);

            if (n <= 50 && m <= 50) {
                result.setDpMatrix(dp);
            }
        } else {
            lcsLength = lcsEngine.computeLcsLengthOptimized(X, Y);
            matchedSequence = List.of();
        }

        double similarity = lcsEngine.computeSimilarity(lcsLength, n, m);
        String level = lcsEngine.classifyPlagiarism(similarity);

        result.setLcsLength(lcsLength);
        result.setSimilarityPercentage(similarity);
        result.setPlagiarismLevel(level);
        result.setMatchedSequence(matchedSequence);

        return result;
    }
}
