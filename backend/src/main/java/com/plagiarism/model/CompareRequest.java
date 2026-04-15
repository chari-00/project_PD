package com.plagiarism.model;

public class CompareRequest {

    private String document1;
    private String document2;
    private boolean removeStopWords;

    public String getDocument1() {
        return document1;
    }

    public void setDocument1(String document1) {
        this.document1 = document1;
    }

    public String getDocument2() {
        return document2;
    }

    public void setDocument2(String document2) {
        this.document2 = document2;
    }

    public boolean isRemoveStopWords() {
        return removeStopWords;
    }

    public void setRemoveStopWords(boolean removeStopWords) {
        this.removeStopWords = removeStopWords;
    }
}
