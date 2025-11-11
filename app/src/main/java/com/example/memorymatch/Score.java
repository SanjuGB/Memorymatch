package com.example.memorymatch;

public class Score {
    public String uid;
    public String name;
    public int score;
    public int timeSeconds;
    public com.google.firebase.Timestamp createdAt;

    public Score() {} // Firestore needs empty constructor

    public Score(String uid, String name, int score, int timeSeconds) {
        this.uid = uid;
        this.name = (name == null || name.trim().isEmpty()) ? "Guest" : name.trim();
        this.score = score;
        this.timeSeconds = timeSeconds;
        this.createdAt = com.google.firebase.Timestamp.now();
    }
}
