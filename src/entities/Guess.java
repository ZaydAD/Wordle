package entities;

public class Guess {
    private int guessId;
    private String word;

    public Guess(int guessId, String word) {
        setGuessId(guessId);
        setWord(word);
    }

    public int getGuessId() { return guessId; }
    public String getWord() { return word; }

    public void setGuessId(int guessId) { this.guessId = guessId; }
    public void setWord(String word) { this.word = word; }

    @Override
    public String toString() {
        return String.format("""
                GuessId - %-10s
                Word - %-10s
                """, getGuessId(), getWord());
    }
}