package com.plagiarism.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class LcsEngineService {

    /**
     * Builds the complete DP matrix for LCS computation.
     * Time:  O(n * m)
     * Space: O(n * m)  - full matrix stored for backtracking and visualization
     */
    public int[][] buildDpMatrix(String[] X, String[] Y) {
        int n = X.length;
        int m = Y.length;

        int[][] dp = new int[n + 1][m + 1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (X[i - 1].equals(Y[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp;
    }

    public int getLcsLength(int[][] dp) {
        int n = dp.length - 1;
        int m = dp[0].length - 1;
        return dp[n][m];
    }

    public List<String> backtrack(int[][] dp, String[] X, String[] Y) {
        List<String> lcs = new ArrayList<>();
        int i = X.length;
        int j = Y.length;

        while (i > 0 && j > 0) {
            if (X[i - 1].equals(Y[j - 1])) {
                lcs.add(X[i - 1]);
                i--;
                j--;
            } else if (dp[i - 1][j] > dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }

        Collections.reverse(lcs);
        return lcs;
    }

    public double computeSimilarity(int lcsLength, int n, int m) {
        int minLength = Math.min(n, m);
        if (minLength == 0) {
            return 0.0;
        }
        return Math.round(((double) lcsLength / minLength) * 10000.0) / 100.0;
    }

    public String classifyPlagiarism(double similarity) {
        if (similarity >= 80.0) {
            return "HIGH";
        }
        if (similarity >= 50.0) {
            return "MODERATE";
        }
        if (similarity >= 20.0) {
            return "LOW";
        }
        return "MINIMAL";
    }

    /**
     * Memory-optimized LCS computation using rolling array (O(min(n,m)) space).
     */
    public int computeLcsLengthOptimized(String[] X, String[] Y) {
        int n = X.length;
        int m = Y.length;

        if (n > m) {
            String[] temp = X;
            X = Y;
            Y = temp;
            n = X.length;
            m = Y.length;
        }

        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (X[i - 1].equals(Y[j - 1])) {
                    curr[j] = prev[j - 1] + 1;
                } else {
                    curr[j] = Math.max(prev[j], curr[j - 1]);
                }
            }

            int[] swap = prev;
            prev = curr;
            curr = swap;
            java.util.Arrays.fill(curr, 0);
        }

        return prev[m];
    }
}
