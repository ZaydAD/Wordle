package entities;

import enums.GameStatus;
import java.util.ArrayList;
import java.util.List;

public class GameState {
    private static final int ATTEMPTS_MAX = 6;
    private static final int WORDS_LENGTH = 5;
    private int currentAttempt;
    private Guess guess;
    private GameStatus status;
    private List<CharFeedback> feedback;
    private List<String> words;

    public GameState(boolean clear) {
        setCurrentAttempt(0);
        setGuess(null);
        setStatus(GameStatus.IN_PROGRESS);
        setFeedback(null, clear);
        setWords(null, clear);
    }

    public int getAttemptsMax() { return ATTEMPTS_MAX; }
    public int getCurrentAttempt() { return currentAttempt; }
    public Guess getGuess() { return guess; }
    public GameStatus getStatus() { return status; }
    public List<CharFeedback> getFeedback() { return feedback; }
    public List<String> getWords() { return words; }

    public String getLastWord() {
        if (!words.isEmpty()) {
            return words.getLast(); // Correctly retrieve the last word
        }
        return ""; // Return an empty string if no words are present
    }

    public void setCurrentAttempt(int currentAttempt) { this.currentAttempt = currentAttempt; }
    public void setGuess(Guess guess) { this.guess = guess; }
    public void setStatus(GameStatus status) { this.status = status; }

    public void setFeedback(List<CharFeedback> feedback, boolean clear) {
        if (clear || feedback == null) {
            this.feedback = new ArrayList<>();
        } else {
            this.feedback = feedback;
        }
    }

    public void setWords(List<String> words, boolean clear) {
        if (clear || words == null) {
            this.words = new ArrayList<>();
        } else {
            this.words = words;
        }
    }

    public void addWord(String word) { this.words.add(word); }

    public void addFeedback(CharFeedback feedback) { this.feedback.add(feedback); }

    @Override
    public String toString() {
        return String.format(
                """
                        Maximum attempts - %-10s
                        Current attempt - %-10s
                        Guess - %-10s
                        GameStatus - %-10s
                        Feedback - %-10s
                        Words - %-10s
                        """,
            getAttemptsMax(), getCurrentAttempt(), getGuess(), getStatus(), getFeedback(), getWords()
        );
    }
}
