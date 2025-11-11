package com.example.memorymatch;

import static android.content.ContentValues.TAG;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class FirestoreScores {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<DocumentReference> saveScore(Score s) {
        return db.collection("scores").add(s);
    }

    public ListenerRegistration listenTopScores(int limit,
                                                EventListener<QuerySnapshot> listener) {

        return db.collection("scores")
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(limit)
                .addSnapshotListener(listener);
    }

    public Task<Void> deleteScore(String docId) {
        return db.collection("scores").document(docId).delete();
    }


    // ✅ Save or update user's high score safely (merge mode)
    public void saveUserHighScore(String uid, int highScore) {
        Map<String, Object> data = new HashMap<>();
        data.put("highScore", highScore);

        db.collection("users").document(uid)
                .set(data, SetOptions.merge()) // ✅ merge instead of overwrite
                .addOnSuccessListener(aVoid -> Log.d(TAG, "High score saved: " + highScore))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save high score", e));
    }

    // ✅ Retrieve user's high score properly
    public void getUserHighScore(String uid, HighScoreCallback callback) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    int value = 0;
                    if (doc.exists() && doc.contains("highScore")) {
                        Long stored = doc.getLong("highScore");
                        if (stored != null) value = stored.intValue();
                        Log.d(TAG, "High score loaded: " + value);
                    } else {
                        Log.d(TAG, "No high score found, returning 0");
                    }
                    callback.onResult(value);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting high score", e);
                    callback.onResult(0);
                });
    }

    public interface HighScoreCallback {
        void onResult(int value);
    }
}
