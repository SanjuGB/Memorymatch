package com.example.memorymatch;

import android.animation.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.animation.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
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
    private TextView scoreText, timerText;
    private Button resetButton;
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

    private int comboCount = 0;
    private long lastMatchTime = 0L;



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

        gridLayout = findViewById(R.id.gridLayout);
        scoreText = findViewById(R.id.scoreText);
        timerText = findViewById(R.id.timerText);
        resetButton = findViewById(R.id.resetButton);

        resetButton.setOnClickListener(v -> {
            animateButton(resetButton);
            resetGame();
        });

        // Inflate modal layout manually and add to root
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
            handler.postDelayed(this::resetGame, 300); // ⏳ small delay after fade
        });


        startGame();
    }

    // ---------------------------------------
    // 🔊 HAPTIC FEEDBACK HELPERS
    // ---------------------------------------
    private void vibrate(long milliseconds) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(milliseconds);
            }
        }
    }

    private void vibratePattern(long[] pattern) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    // ---------------------------------------
    // 🏆 MODAL HANDLING
    // ---------------------------------------
    private void showWinModal(int finalScore) {
        winScoreText.setText("Final Score: " + finalScore);
        winModalRoot.setVisibility(View.VISIBLE);
        winModalRoot.setAlpha(0f);
        winModalRoot.animate().alpha(1f).setDuration(400).start();

        View modal = findViewById(R.id.winModal);
        modal.setScaleX(0.7f);
        modal.setScaleY(0.7f);
        modal.animate().scaleX(1f).scaleY(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // Vibrate celebratory pattern 🎉
        vibratePattern(new long[]{0, 200, 100, 300});
    }

    private void hideWinModal() {
        winModalRoot.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> winModalRoot.setVisibility(View.GONE))
                .start();
    }

    // ---------------------------------------
    // 🎮 GAME LOGIC
    // ---------------------------------------
    private void startGame() {
        gridLayout.removeAllViews();
        Arrays.fill(cards, null); // ✅ Clear previous references

        firstCardIndex = -1;
        matchedCount = 0;
        score = 0;
        timerStarted = false;
        isGameRunning = true;
        updateScore();
        timerText.setText("Time: 0s");

        // Shuffle emojis
        List<String> emojis = Arrays.asList(emojiArray);
        Collections.shuffle(emojis);
        emojis.toArray(emojiArray);

        gridLayout.post(() -> {
            int gridWidth = gridLayout.getWidth();
            int columns = 4;
            int rows = 4;
            int spacing = 6;
            int totalHorizontalSpacing = spacing * (columns - 1);
            int cardSize = (gridWidth - totalHorizontalSpacing) / columns;

            gridLayout.removeAllViews();
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

    private void resetGame() {

        isGameRunning = false;
        handler.removeCallbacksAndMessages(null);
        timerHandler.removeCallbacks(timerRunnable);

        // ✅ Reset all existing cards' colors before removing them
        for (TextView card : cards) {
            if (card != null) {
                card.getBackground().setTint(Color.parseColor("#D1C4E9"));
                card.setText("");
                card.setTag("hidden");
            }
        }

        // ✅ Clear all card references
        Arrays.fill(cards, null);

        // Smooth fade-out transition before restarting
        gridLayout.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    gridLayout.setAlpha(1f);
                    startGame();
                })
                .start();
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
        card.getBackground().setTint(Color.parseColor("#9575CD")); // dark purple

        if (firstCardIndex == -1) {
            firstCardIndex = index;
            firstClickTime = SystemClock.elapsedRealtime();
        } else {
            isBusy = true;
            final int firstIndexCopy = firstCardIndex;
            final int secondIndex = index;
            handler.postDelayed(() -> checkMatch(firstIndexCopy, secondIndex), 700);
        }
    }

    private void checkMatch(int firstIndex, int secondIndex) {
        if (firstIndex < 0 || secondIndex < 0 ||
                firstIndex >= cards.length || secondIndex >= cards.length) return;

        TextView firstCard = cards[firstIndex];
        TextView secondCard = cards[secondIndex];

        if (firstCard.getTag().equals("matched") || secondCard.getTag().equals("matched")) {
            isBusy = false;
            return;
        }

        if (emojiArray[firstIndex].equals(emojiArray[secondIndex])) {
            // ✅ MATCH FOUND
            firstCard.setTag("matched");
            secondCard.setTag("matched");
            firstCard.getBackground().setTint(Color.parseColor("#FFEB3B"));
            secondCard.getBackground().setTint(Color.parseColor("#FFEB3B"));
            matchedCount += 2;
            score += 10;
            vibrate(120); // stronger buzz

            // 🌟 Flash glow animation on match
            AnimatorSet glow = new AnimatorSet();
            ObjectAnimator scaleUpX1 = ObjectAnimator.ofFloat(firstCard, "scaleX", 1f, 1.2f, 1f);
            ObjectAnimator scaleUpY1 = ObjectAnimator.ofFloat(firstCard, "scaleY", 1f, 1.2f, 1f);
            ObjectAnimator scaleUpX2 = ObjectAnimator.ofFloat(secondCard, "scaleX", 1f, 1.2f, 1f);
            ObjectAnimator scaleUpY2 = ObjectAnimator.ofFloat(secondCard, "scaleY", 1f, 1.2f, 1f);
            glow.playTogether(scaleUpX1, scaleUpY1, scaleUpX2, scaleUpY2);
            glow.setDuration(350);
            glow.setInterpolator(new OvershootInterpolator());
            glow.start();

            // 💥 Floating “+10” text
            showFloatingText("+10", Color.parseColor("#FFF176"));

            // ⚡ Reaction bonus
            long reactionTime = (SystemClock.elapsedRealtime() - firstClickTime) / 1000;
            if (reactionTime <= 3) {
                score += 5;
                showFloatingText("⚡ Quick Match! +5", Color.YELLOW);
                vibratePattern(new long[]{0, 100, 100, 150}); // double buzz for fast play
            }

            updateScore();// success feedback

            long now = SystemClock.elapsedRealtime();
            if (now - lastMatchTime < 4000) {
                comboCount++;
            } else {
                comboCount = 1;
            }
            lastMatchTime = now;

// 🎯 Combo bonuses
            if (comboCount >= 3) {
                int comboBonus = comboCount * 5;
                score += comboBonus;
                showFloatingText("🔥 " + comboCount + "x Combo! +" + comboBonus, Color.parseColor("#FFA000"));
                vibratePattern(new long[]{0, 150, 80, 200});
            }


            if (matchedCount == emojiArray.length) {
                isGameRunning = false;
                timerHandler.removeCallbacks(timerRunnable);
                playConfetti();
                handler.postDelayed(() -> showWinModal(score), 600);
            }

        } else {
            // ❌ NOT A MATCH
            score -= 2;
            vibrate(60); // subtle buzz for wrong guess
            updateScore();
            shakeView(firstCard);
            shakeView(secondCard);

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

    // ---------------------------------------
    // 🎨 UI ANIMATIONS
    // ---------------------------------------
    private void updateScore() {
        animateTextChange(scoreText, "Score: " + score);
    }

    private void flipCard(TextView card, String fromText, String toText) {
        ObjectAnimator flipOut = ObjectAnimator.ofFloat(card, "rotationY", 0f, 90f);
        ObjectAnimator flipIn = ObjectAnimator.ofFloat(card, "rotationY", -90f, 0f);
        flipOut.setDuration(150);
        flipIn.setDuration(150);
        flipOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                card.setText(toText);
                flipIn.start();
            }
        });
        flipOut.start();
    }

    private void animateTextChange(TextView view, String newText) {
        view.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> {
            view.setText(newText);
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
        });
    }

    private void shakeView(View view) {
        Animation shake = new TranslateAnimation(0, 15, 0, 0);
        shake.setInterpolator(new CycleInterpolator(2));
        shake.setDuration(300);
        view.startAnimation(shake);
    }

    private void playConfetti() {
        View root = findViewById(android.R.id.content);
        ObjectAnimator flash = ObjectAnimator.ofArgb(root, "backgroundColor",
                Color.parseColor("#121212"), Color.parseColor("#FFD700"), Color.parseColor("#121212"));
        flash.setDuration(1200);
        flash.start();
    }

    private void showFloatingText(String text, int color) {
        TextView floatingText = new TextView(this);
        floatingText.setText(text);
        floatingText.setTextColor(color);
        floatingText.setTextSize(24);
        floatingText.setGravity(Gravity.CENTER);
        floatingText.setShadowLayer(10, 0, 0, Color.parseColor("#FFF59D"));
        floatingText.setTypeface(floatingText.getTypeface(), android.graphics.Typeface.BOLD);

        // 🎯 Position: Center horizontally, slightly above gridLayout
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER_HORIZONTAL;

        // Get grid position
        int[] gridPos = new int[2];
        gridLayout.getLocationOnScreen(gridPos);
        int gridTop = gridPos[1];

        // 🪄 Random offset to prevent overlapping
        // 🪄 Random offset to prevent overlapping (more dramatic)
        int verticalOffset = (int) (Math.random() * 160) - 80; // -80 to +80
        int horizontalOffset = (int) (Math.random() * 200) - 100; // -100 to +100
        params.topMargin = gridTop - 200 + verticalOffset;


        addContentView(floatingText, params);
        floatingText.setTranslationX(horizontalOffset);

        // 🌟 Initial appearance
        floatingText.setAlpha(0f);
        floatingText.setScaleX(0.6f);
        floatingText.setScaleY(0.6f);

        // ✨ Animate the text floating upward with glow
        floatingText.animate()
                .alpha(1f)
                .translationYBy(-200)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> {
                    floatingText.animate()
                            .alpha(0f)
                            .translationYBy(-150)
                            .setDuration(1300)
                            .setInterpolator(new AccelerateInterpolator())
                            .withEndAction(() -> ((FrameLayout) floatingText.getParent()).removeView(floatingText))
                            .start();
                })
                .start();

        // 💫 Add sparkle particles trail
        for (int i = 0; i < 5; i++) {
            addSparkleParticle(gridTop - 150 + verticalOffset, horizontalOffset, color);
        }
    }
    private void addSparkleParticle(int startY, int offsetX, int color) {
        View particle = new View(this);
        particle.setBackgroundColor(color);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(10, 10);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = startY + (int)(Math.random() * 30) + 50;
        particle.setTranslationX(offsetX + (float)(Math.random() * 60 - 30));

        addContentView(particle, params);

        particle.setAlpha(0.8f);
        particle.setScaleX(0.7f);
        particle.setScaleY(0.7f);

        // Randomized shimmer animation
        particle.animate()
                .translationYBy(-300 - (float)(Math.random() * 100))
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(1000 + (int)(Math.random() * 400))
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> ((FrameLayout) particle.getParent()).removeView(particle))
                .start();
    }



    private void animateButton(Button button) {
        ObjectAnimator rotation = ObjectAnimator.ofFloat(button, "rotation", 0f, 360f);
        rotation.setDuration(500);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());
        rotation.start();
    }
}
