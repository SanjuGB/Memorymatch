package com.example.memorymatch;

public class LeaderboardItem {
    public String id; // docId
    public String uid;
    public String name;
    public int score;
    public int timeSeconds;

    public LeaderboardItem(String id, String uid, String name, int score, int timeSeconds) {
        this.id = id; this.uid = uid; this.name = name;
        this.score = score; this.timeSeconds = timeSeconds;
    }
}
