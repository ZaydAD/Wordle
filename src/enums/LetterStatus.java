package enums;

public enum LetterStatus {
    NOT_FILLED(0),
    WRONG(0),
    IN_WORD(2),
    CORRECT(10);

    private final int points;

    LetterStatus(int points) { this.points = points; }

    @Override
    public String toString() {
        return String.valueOf(points);
    }
}
