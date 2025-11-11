package com.example.memorymatch;

import android.animation.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.animation.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String[] emojiArray = {
            "🐶","🐶","🐱","🐱",
            "🐸","🐸","🐼","🐼",
            "🐵","🐵","🦊","🦊",
            "🐰","🐰","🐯","🐯"
    };

    private TextView[] cards = new TextView[16];
    private TextView scoreText, timerText, highScoreText;
    private Button resetButton, leaderboardButton;
    private GridLayout gridLayout;

    private int firstCardIndex = -1;
    private boolean isBusy = false;
    private int matchedCount = 0;
    private int score = 0;
    private long startTime = 0L;
    private long firstClickTime = 0L;
    private boolean isGameRunning = false;
    private boolean timerStarted = false;

    private final Handler handler = new Handler();
    private final Handler timerHandler = new Handler();

    private View winModalRoot;
    private TextView winScoreText;
    private Button playAgainButton;
    private EditText playerNameInput;
    private int highScore = 0;

    private FirebaseAuth auth;
    private FirestoreScores firestore;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isGameRunning) {
                long elapsed = (SystemClock.elapsedRealtime() - startTime) / 1000;
                animateTextChange(timerText, "Time: " + elapsed + "s");
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        firestore = new FirestoreScores();

        // 🔒 Check if user is logged in
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // No user -> redirect to RegisterActivity
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
            return; // stop further execution of this activity
        }

        gridLayout = findViewById(R.id.gridLayout);
        scoreText = findViewById(R.id.scoreText);
        timerText = findViewById(R.id.timerText);
        resetButton = findViewById(R.id.resetButton);
        leaderboardButton = findViewById(R.id.leaderboardButton);
        highScoreText = findViewById(R.id.highScoreText);

        resetButton.setOnClickListener(v -> {
            animateButton(resetButton);
            resetGame();
        });

        leaderboardButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });

        // ✅ Load user's high score from Firestore
        loadHighScore();

        // Inflate modal
        View modalView = getLayoutInflater().inflate(R.layout.winner_modal, null);
        addContentView(modalView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        winModalRoot = modalView.findViewById(R.id.winModalRoot);
        winScoreText = modalView.findViewById(R.id.winScore);
        playAgainButton = modalView.findViewById(R.id.playAgainButton);

        playAgainButton.setOnClickListener(v -> {
            hideWinModal();
            handler.postDelayed(this::resetGame, 300);
        });

        startGame();
    }

    // ----------------- GAME LOGIC -----------------
    private void startGame() {
        gridLayout.removeAllViews();
        Arrays.fill(cards, null);

        firstCardIndex = -1;
        matchedCount = 0;
        score = 0;
        timerStarted = false;
        isGameRunning = true;
        updateScore();
        timerText.setText("Time: 0s");

        List<String> emojis = Arrays.asList(emojiArray);
        Collections.shuffle(emojis);
        emojis.toArray(emojiArray);

        gridLayout.post(() -> {
            int gridWidth = gridLayout.getWidth();
            int columns = 4, rows = 4, spacing = 6;
            int totalHorizontalSpacing = spacing * (columns - 1);
            int cardSize = (gridWidth - totalHorizontalSpacing) / columns;

            gridLayout.setColumnCount(columns);
            gridLayout.setRowCount(rows);

            for (int i = 0; i < cards.length; i++) {
                TextView card = new TextView(this);
                card.setText("");
                card.setTextSize(36);
                card.setGravity(Gravity.CENTER);
                card.setBackgroundResource(R.drawable.card_bg);
                card.setTextColor(Color.BLACK);
                card.setTag("hidden");

                int finalI = i;
                card.setOnClickListener(v -> onCardClick(finalI));

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cardSize;
                params.height = cardSize;

                int col = i % columns;
                int row = i / columns;
                if (col < columns - 1) params.rightMargin = spacing;
                if (row < rows - 1) params.bottomMargin = spacing;

                gridLayout.addView(card, params);
                cards[i] = card;
            }
        });
    }

    private void onCardClick(int index) {
        if (!isGameRunning || isBusy) return;
        TextView card = cards[index];
        if (card.getTag().equals("matched") || index == firstCardIndex) return;

        if (!timerStarted) {
            timerStarted = true;
            startTime = SystemClock.elapsedRealtime();
            timerHandler.post(timerRunnable);
        }

        flipCard(card, "", emojiArray[index]);
        card.getBackground().setTint(Color.parseColor("#9575CD"));

        if (firstCardIndex == -1) {
            firstCardIndex = index;
            firstClickTime = SystemClock.elapsedRealtime();
        } else {
            isBusy = true;
            int firstIndexCopy = firstCardIndex;
            int secondIndex = index;
            handler.postDelayed(() -> checkMatch(firstIndexCopy, secondIndex), 700);
        }
    }

    private void checkMatch(int firstIndex, int secondIndex) {
        TextView firstCard = cards[firstIndex];
        TextView secondCard = cards[secondIndex];

        if (emojiArray[firstIndex].equals(emojiArray[secondIndex])) {
            firstCard.setTag("matched");
            secondCard.setTag("matched");
            firstCard.getBackground().setTint(Color.parseColor("#FFEB3B"));
            secondCard.getBackground().setTint(Color.parseColor("#FFEB3B"));
            matchedCount += 2;
            score += 10;
            vibrate(120);
            updateScore();

            if (matchedCount == emojiArray.length) {
                isGameRunning = false;
                timerHandler.removeCallbacks(timerRunnable);
                playConfetti();
                saveCurrentScore();
                handler.postDelayed(() -> showWinModal(score), 600);
            }
        } else {
            score -= 2;
            vibrate(60);
            updateScore();
            handler.postDelayed(() -> {
                flipCard(firstCard, emojiArray[firstIndex], "");
                flipCard(secondCard, emojiArray[secondIndex], "");
                firstCard.getBackground().setTint(Color.parseColor("#D1C4E9"));
                secondCard.getBackground().setTint(Color.parseColor("#D1C4E9"));
            }, 400);
        }

        firstCardIndex = -1;
        handler.postDelayed(() -> isBusy = false, 400);
    }
    // ✅ Load high score from Firestore
    private void loadHighScore() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        firestore.getUserHighScore(user.getUid(), new FirestoreScores.HighScoreCallback() {
            @Override
            public void onResult(int value) {
                highScore = value;
                runOnUiThread(() -> highScoreText.setText("High Score: " + highScore));
            }
        });
    }
    private void saveCurrentScore() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        int elapsed = (int) ((SystemClock.elapsedRealtime() - startTime) / 1000);

        // 📨 Get player name from email (before '@')
        String email = user.getEmail();
        String name;

        if (email != null && email.contains("@")) {
            name = email.substring(0, email.indexOf('@'));
        } else {
            // fallback for anonymous or invalid accounts
            name = "Guest";
        }

        Score s = new Score(user.getUid(), name, score, elapsed);
        firestore.saveScore(s);

        // ✅ Update high score if beaten
        if (score > highScore) {
            highScore = score;
            highScoreText.setText("High Score: " + highScore);
            firestore.saveUserHighScore(user.getUid(), highScore);
        }
    }


    // ----------------- ANIMATIONS -----------------
    private void updateScore() { animateTextChange(scoreText, "Score: " + score); }
    private void animateTextChange(TextView v, String text) {
        v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100)
                .withEndAction(() -> {
                    v.setText(text);
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                });
    }
    private void flipCard(TextView card, String from, String to) {
        ObjectAnimator flipOut = ObjectAnimator.ofFloat(card, "rotationY", 0f, 90f);
        ObjectAnimator flipIn = ObjectAnimator.ofFloat(card, "rotationY", -90f, 0f);
        flipOut.setDuration(150); flipIn.setDuration(150);
        flipOut.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                card.setText(to); flipIn.start();
            }
        }); flipOut.start();
    }

    private void playConfetti() {
        View root = findViewById(android.R.id.content);
        ObjectAnimator flash = ObjectAnimator.ofArgb(root, "backgroundColor",
                Color.parseColor("#121212"), Color.parseColor("#FFD700"), Color.parseColor("#121212"));
        flash.setDuration(1200);
        flash.start();
    }

    private void animateButton(Button button) {
        ObjectAnimator rotation = ObjectAnimator.ofFloat(button, "rotation", 0f, 360f);
        rotation.setDuration(500);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());
        rotation.start();
    }

    private void showWinModal(int finalScore) {
        winScoreText.setText("Final Score: " + finalScore);
        winModalRoot.setVisibility(View.VISIBLE);
        winModalRoot.setAlpha(0f);
        winModalRoot.animate().alpha(1f).setDuration(400).start();
    }

    private void hideWinModal() {
        winModalRoot.animate()
                .alpha(0f).setDuration(300)
                .withEndAction(() -> winModalRoot.setVisibility(View.GONE)).start();
    }

    private void vibrate(long ms) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null) return;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        else v.vibrate(ms);
    }

    private void resetGame() {
        isGameRunning = false;
        handler.removeCallbacksAndMessages(null);
        timerHandler.removeCallbacks(timerRunnable);

        // Subtle "board reset" animation
        gridLayout.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0f)
                .setDuration(250)
                .withEndAction(() -> {
                    for (TextView card : cards) {
                        if (card != null) {
                            card.setText("");
                            card.setTag("hidden");
                            card.getBackground().setTint(Color.parseColor("#D1C4E9"));
                        }
                    }
                    Arrays.fill(cards, null);
                    gridLayout.removeAllViews();
                    gridLayout.setAlpha(1f);
                    gridLayout.setScaleX(1f);
                    gridLayout.setScaleY(1f);
                    startGame();
                })
                .start();
    }

}
