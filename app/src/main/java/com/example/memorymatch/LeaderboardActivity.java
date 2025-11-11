package com.example.memorymatch;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView leaderboardList;
    private ProgressBar leaderboardLoading;
    private LeaderboardAdapter leaderboardAdapter;
    private FirestoreScores firestore;
    private FirebaseAuth auth;
    private ListenerRegistration leaderboardReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        leaderboardList = findViewById(R.id.leaderboardList);
        leaderboardLoading = findViewById(R.id.leaderboardLoading);

        auth = FirebaseAuth.getInstance();
        firestore = new FirestoreScores();

        leaderboardAdapter = new LeaderboardAdapter(this::confirmAndDelete);
        leaderboardList.setLayoutManager(new LinearLayoutManager(this));
        leaderboardList.setAdapter(leaderboardAdapter);

        listenToLeaderboard();
    }

    private void listenToLeaderboard() {
        leaderboardLoading.setVisibility(android.view.View.VISIBLE);
        leaderboardReg = firestore.listenTopScores(20, (snap, err) -> {
            leaderboardLoading.setVisibility(android.view.View.GONE);
            if (err != null) {
                Toast.makeText(this, "Error loading leaderboard.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (snap == null) return;

            List<LeaderboardItem> list = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                String id = d.getId();
                String uid = d.getString("uid");
                String name = d.getString("name");
                Long sc = d.getLong("score");
                Long ts = d.getLong("timeSeconds");
                list.add(new LeaderboardItem(id, uid, name,
                        sc != null ? sc.intValue() : 0,
                        ts != null ? ts.intValue() : 0));
            }
            leaderboardAdapter.setItems(list);
        });
    }

    private void confirmAndDelete(LeaderboardItem item) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || item.uid == null || !item.uid.equals(user.getUid())) {
            Toast.makeText(this, "You can delete only your own scores.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete score?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) ->
                        firestore.deleteScore(item.id)
                                .addOnSuccessListener(v ->
                                        Toast.makeText(this, "Score deleted.", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (leaderboardReg != null) leaderboardReg.remove();
    }
}
