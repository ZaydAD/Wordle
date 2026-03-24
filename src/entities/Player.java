package entities;

public class Player {
    private int playerId;
    private boolean isGuest;
    private String username;
    private String email;
    private String birthday;
    private String gender;

    private PlayerStats playerStats;

    public Player() {
        setGuestStatus(true);
        setUsername(null);
        setEmail(null);
        setPlayerStats(new PlayerStats());
    }

    public int getPlayerId() { return playerId; }
    public boolean getGuestStatus() { return isGuest; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getBirthday() { return birthday; }
    public String getGender() { return gender; }
    

    public void setPlayerId(int playerId) { this.playerId = playerId; }
    public void setGuestStatus(boolean guestStatus) { this.isGuest = guestStatus; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public void setGender(String gender) { this.gender = gender; }

    public PlayerStats getPlayerStats() { return playerStats; }
    public void setPlayerStats(PlayerStats playerStats) { this.playerStats = playerStats; }
    public void updateScore(int score) { playerStats.setScore(score); }

    @Override
    public String toString() {
        return String.format("%s %s %s %s %s", getPlayerId(), getGuestStatus(), getUsername(), getEmail(), getBirthday());
    }
}