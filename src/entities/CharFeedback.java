package entities;

import enums.LetterStatus;

public class CharFeedback {
    private int feedbackId;
    private char character;
    private LetterStatus status;

    public CharFeedback(int feedbackId, char character, LetterStatus status) {
        setFeedbackId(feedbackId);
        setCharacter(character);
        setStatus(status);
    }

    public char getCharacter() { return character; }
    public LetterStatus getStatus() { return status; }

    public void setFeedbackId(int feedbackId) { this.feedbackId = feedbackId; }
    public void setCharacter(char character) { this.character = character; }
    public void setStatus(LetterStatus status) { this.status = status; }

    @Override
    public String toString() {
        return String.valueOf(character);
    }
}