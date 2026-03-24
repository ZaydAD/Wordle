package entities;

public class PlayerStats {
    private int score;

    // Default constructor
    public PlayerStats() {
        setScore(0);
    }

    public int getScore() { return score; }

    public void setScore(int score) { this.score = score; }

    @Override
    public String toString() {
        return "PlayerStats{" +
                ", score=" + score +
                '}';
    }
}