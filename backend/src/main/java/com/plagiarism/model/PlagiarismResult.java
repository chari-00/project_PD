package com.plagiarism.model;

import java.util.List;

public class PlagiarismResult {

    private int lcsLength;
    private int doc1TokenCount;
    private int doc2TokenCount;
    private double similarityPercentage;
    private String plagiarismLevel;
    private List<String> matchedSequence;
    private String[] doc1Tokens;
    private String[] doc2Tokens;
    private int[][] dpMatrix;

    public int getLcsLength() {
        return lcsLength;
    }

    public void setLcsLength(int lcsLength) {
        this.lcsLength = lcsLength;
    }

    public int getDoc1TokenCount() {
        return doc1TokenCount;
    }

    public void setDoc1TokenCount(int doc1TokenCount) {
        this.doc1TokenCount = doc1TokenCount;
    }

    public int getDoc2TokenCount() {
        return doc2TokenCount;
    }

    public void setDoc2TokenCount(int doc2TokenCount) {
        this.doc2TokenCount = doc2TokenCount;
    }

    public double getSimilarityPercentage() {
        return similarityPercentage;
    }

    public void setSimilarityPercentage(double similarityPercentage) {
        this.similarityPercentage = similarityPercentage;
    }

    public String getPlagiarismLevel() {
        return plagiarismLevel;
    }

    public void setPlagiarismLevel(String plagiarismLevel) {
        this.plagiarismLevel = plagiarismLevel;
    }

    public List<String> getMatchedSequence() {
        return matchedSequence;
    }

    public void setMatchedSequence(List<String> matchedSequence) {
        this.matchedSequence = matchedSequence;
    }

    public String[] getDoc1Tokens() {
        return doc1Tokens;
    }

    public void setDoc1Tokens(String[] doc1Tokens) {
        this.doc1Tokens = doc1Tokens;
    }

    public String[] getDoc2Tokens() {
        return doc2Tokens;
    }

    public void setDoc2Tokens(String[] doc2Tokens) {
        this.doc2Tokens = doc2Tokens;
    }

    public int[][] getDpMatrix() {
        return dpMatrix;
    }

    public void setDpMatrix(int[][] dpMatrix) {
        this.dpMatrix = dpMatrix;
    }
}
