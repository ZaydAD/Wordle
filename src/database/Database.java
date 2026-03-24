package database;

import controller.GameSession;
import entities.CharFeedback;
import entities.GameState;
import entities.Guess;
import entities.Player;
import enums.GameStatus;
import enums.LetterStatus;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {

    //05_IMPLEMENT_connection
    private static final String DB_NAME = "ascii6";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "postgres";
    private static final String CONNECTION_URL = "jdbc:postgresql://127.0.0.1:5432/" + DB_NAME;

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONNECTION_URL, USERNAME, PASSWORD);
    }

    public static void saveGame(GameSession game) {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false); // Disable auto commit
            String checkForGameSessionSQL = """
                    SELECT COUNT(game_session_id) count_users_sessions FROM game_sessions WHERE game_session_id=? AND user_id=?;
                    """;
            PreparedStatement pstmt = connection.prepareStatement(checkForGameSessionSQL);
            pstmt.setInt(1, game.getGameId());
            pstmt.setInt(2, game.getPlayer().getPlayerId());
            ResultSet rsFeedback = pstmt.executeQuery();
            int anyGameSessions = 0;
            while(rsFeedback.next()) {
                anyGameSessions = rsFeedback.getInt("count_users_sessions");
            }
            // Insert game session data
            String insertGameSessionSQL = """
                INSERT INTO game_sessions (game_session_id, user_id, duration, time_stamp, word_id, attempts_num, score)
                VALUES (?, ?, ?, ?, ?, ?, ?);
            """;
            String updateGameSessionSQL = """
                    UPDATE game_sessions SET duration=?, time_stamp=?, word_id=?, attempts_num=?
                    WHERE game_session_id=? AND user_id=?;
                    """;
            if(anyGameSessions == 0){
                pstmt = connection.prepareStatement(insertGameSessionSQL);
                pstmt.setInt(1, game.getGameId());
                pstmt.setInt(2, game.getPlayer().getPlayerId());
                pstmt.setInt(3, game.getDuration());
                pstmt.setTimestamp(4, game.getTimestamp());
                pstmt.setInt(5, game.getGameState().getGuess().getGuessId());
                pstmt.setInt(6, game.getGameState().getCurrentAttempt());
                pstmt.setInt(7, game.getPlayer().getPlayerStats().getScore());
                pstmt.executeUpdate();
            } else{
                pstmt = connection.prepareStatement(updateGameSessionSQL);
                pstmt.setInt(1,game.getDuration());
                pstmt.setTimestamp(2, game.getTimestamp());
                pstmt.setInt(3,game.getGameState().getGuess().getGuessId());
                pstmt.setInt(4,game.getGameState().getCurrentAttempt());
                pstmt.setInt(5, game.getGameId());
                pstmt.setInt(6, game.getPlayer().getPlayerId());
                pstmt.executeUpdate();
            }
            // Insert character feedback data for each character
            String insertCharFeedbackSQL = """
                INSERT INTO cell (game_session_id, user_id, position, character, status)
                VALUES (?, ?, ?, ?, ?);
            """;
            try (PreparedStatement pstmtcf = connection.prepareStatement(insertCharFeedbackSQL)) {
                int lastLetterIndex = getLastLetterIndex(game.getPlayer().getPlayerId());// Start with position 1
                for (int i=lastLetterIndex; i<game.getGameState().getFeedback().size(); i++) {
                    CharFeedback feedback = game.getGameState().getFeedback().get(i);
                    pstmtcf.setInt(1, game.getGameId());
                    pstmtcf.setInt(2, game.getPlayer().getPlayerId());
                    pstmtcf.setInt(3, i+1);
                    pstmtcf.setString(4, String.valueOf(feedback.getCharacter()));
                    pstmtcf.setString(5, feedback.getStatus().name());
                    pstmtcf.addBatch();
                }
                pstmtcf.executeBatch();
            }

            connection.commit(); // Commit the transaction
            System.out.println("Game saved successfully!");
        } catch (SQLException e) {
            System.out.println("Error saving game.");
            e.printStackTrace();
        }
    }



    public static GameState loadGame(int playerId) {
    try (Connection connection = getConnection()) {
        // Step 1: Load the most recent game session for the player
        String loadGameSessionSQL = """
            SELECT game_session_id, user_id, duration, time_stamp, word_id, attempts_num, score
            FROM game_sessions
            WHERE user_id = ?
            ORDER BY time_stamp DESC
            LIMIT 1;
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(loadGameSessionSQL)) {
            pstmt.setInt(1, playerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Retrieve game session details
                int gameSessionId = rs.getInt("game_session_id");
                int duration = rs.getInt("duration");
                Timestamp timestamp = rs.getTimestamp("time_stamp");
                int wordId = rs.getInt("word_id");
                int attemptsNum = rs.getInt("attempts_num");
                int score = rs.getInt("score");

                // Step 2: Initialize the GameState object
                GameState gameState = new GameState(false); // Pass 'false' because we don't want to clear feedback and words
                gameState.setCurrentAttempt(attemptsNum);
                gameState.setStatus(GameStatus.IN_PROGRESS); // Assuming status is always IN_PROGRESS

                // Step 3: Load the associated word (Guess)
                String loadWordSQL = """
                    SELECT word_to_guess
                    FROM words
                    WHERE word_id = ?;
                """;
                try (PreparedStatement pstmtWord = connection.prepareStatement(loadWordSQL)) {
                    pstmtWord.setInt(1, wordId);
                    ResultSet rsWord = pstmtWord.executeQuery();
                    if (rsWord.next()) {
                        String wordToGuess = rsWord.getString("word_to_guess");
                        gameState.setGuess(new Guess(wordId, wordToGuess));
                    }
                }

                // Step 4: Load the character feedback
                String loadFeedbackSQL = """
                    SELECT position, character, status
                    FROM cell
                    WHERE game_session_id = ? AND user_id = ?
                    ORDER BY position;
                """;

                try (PreparedStatement pstmtFeedback = connection.prepareStatement(loadFeedbackSQL)) {
                    pstmtFeedback.setInt(1, gameSessionId);
                    pstmtFeedback.setInt(2, playerId);
                    ResultSet rsFeedback = pstmtFeedback.executeQuery();

                    List<CharFeedback> feedbackList = new ArrayList<>();
                    while (rsFeedback.next()) {
                        int position = rsFeedback.getInt("position");
                        char character = rsFeedback.getString("character").charAt(0);
                        String letterStatusString = rsFeedback.getString("status");
                        LetterStatus letterStatus = switch (letterStatusString) {
                            case "CORRECT" -> LetterStatus.CORRECT;
                            case "IN_WORD" -> LetterStatus.IN_WORD;
                            case "WRONG" -> LetterStatus.WRONG;
                            case null, default -> LetterStatus.NOT_FILLED;
                        };
                        feedbackList.add(new CharFeedback(position, character, letterStatus));
                    }
                    gameState.setFeedback(feedbackList, false);
                }

                // Step 5: Set additional game state details (e.g., words if necessary)
                if (5 - gameState.getFeedback().size() % 5 == 5) { // Check if row is completely filled
                    StringBuilder word = new StringBuilder();
                    for (int i = 0; i < 5; i++) {
                        word.append(gameState.getFeedback().get((gameState.getCurrentAttempt()-1) * 5 + i)); // append the word to the right row
                    }
                    gameState.addWord(word.toString());
                }

                System.out.println("Game loaded successfully!");
                return gameState;
            } else {
                System.out.println("No game found for this player.");
                return null;
            }
        }
    } catch (SQLException e) {
        System.out.println("Error loading game.");
        e.printStackTrace();
        return null;
    }
}

public static int getLastLetterIndex(int playerId) {
    try (Connection connection = getConnection()) {
        // Query to get the highest position from the cell table for the given player
        String query = """
            SELECT MAX(position) AS last_position
            FROM cell
            WHERE user_id = ?;
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, playerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("last_position"); // Return the last position (letter index)
            } else {
                // Return -1 if no positions were found (no feedback available)
                return -1;
            }
        }
    } catch (SQLException e) {
        System.out.println("Error fetching last letter index.");
        e.printStackTrace();
        return -1; // Return -1 in case of an error
    }
}
    //09_IMPLEMENT_PLAYER
    public static List<String> loadPlayersNames(int numOfPlayers) {
        try {
            Connection connection = getConnection();
            Statement statement = connection.createStatement();

            List<String> players = new ArrayList<>();
            String fetchQuery;
            //LOAD PLAYERS FOR LOGIN
            if(numOfPlayers == 0){
                String LoginQuery = "SELECT username from users";
                ResultSet rs = statement.executeQuery(LoginQuery);
                while(rs.next()){
                    players.add(rs.getString("username"));
                }
                //Load Players for leaderboard
            }else{
                fetchQuery = " FETCH FIRST " + numOfPlayers + " ROWS ONLY";
                String LeaderBoardQuery = "SELECT u.username from users u " +
                        "                  JOIN game_sessions gs ON (u.user_id = gs.user_id) ORDER BY gs.score DESC " + fetchQuery +";";
                ResultSet rs = statement.executeQuery(LeaderBoardQuery);
                while(rs.next()){
                    players.add(rs.getString("username"));
                }
            }

            //==Get the highest user_id and increment by 1
            //modify SQL to game_session
            connection.close();
            return players;
            // return rs.getString();
        }catch (SQLException exc){
            System.out.println("An error occurred while loading players.");
            exc.printStackTrace();
        }
        return null;
    }
    public static List<Integer> loadBestScores(int numOfScores){
        try {
            Connection connection = getConnection();
            Statement statement = connection.createStatement();

            String fetchQuery;
            if(numOfScores == 0){
                fetchQuery = "";
            }else{
                fetchQuery = " FETCH FIRST " + numOfScores + " ROWS ONLY";
            }
            //==Get the highest user_id and increment by 1
            ResultSet rs = statement.executeQuery("SELECT score FROM game_sessions ORDER BY score DESC" + fetchQuery + ";");
            List<Integer> scores = new ArrayList<>();
            while (rs.next()) {
                scores.add(rs.getInt("score"));
            }
            connection.close();
            return scores;
            // return rs.getString();
        }catch (SQLException exc){
            System.out.println("An error occurred while loading players.");
            exc.printStackTrace();
        }
        return null;
    }

    public static void addNewPlayer(Player player){
        try {
            Connection connection = getConnection();
            Statement statement = connection.createStatement();

            //==Get the highest user_id and increment by 1
            ResultSet rs = statement.executeQuery("SELECT COALESCE(MAX(user_id), 0) as MaxID FROM users;");
            //coalesce in-order to return 0 if the table is empty

            if (rs.next()) {
                player.setPlayerId(rs.getInt("MaxID") + 1); //Increment by 1
            }
            //====================Query that stores new player data inside the database==========================

            statement.executeUpdate(
                    "INSERT INTO users (user_id, username, gender, email, birthdate)" +
                            "VALUES ("+ player.getPlayerId() + ", " +
                            "'"+ player.getUsername() +"', " +
                            "'" + player.getGender() + "', " +
                            "'" + player.getEmail() + "', " +
                            "'" +player.getBirthday()+"');");

            connection.close();
        } catch (SQLException exc) {
            System.out.println("Error during sign-up.");
            exc.printStackTrace();
        }
    }

    public static boolean checkIfUsernameExists(String username){
        Connection connection = null;
        try {
            connection = getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        String checkForTheEmail = """
                SELECT COUNT(email) if_exists FROM users WHERE username=?;
                """;
        try (PreparedStatement pstmt = connection.prepareStatement(checkForTheEmail)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("if_exists") > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public static boolean checkIfEmailExists(String email){
        Connection connection = null;
        try {
            connection = getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        String checkForTheEmail = """
                SELECT COUNT(email) if_exists FROM users WHERE email=?;
                """;
        try (PreparedStatement pstmt = connection.prepareStatement(checkForTheEmail)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("if_exists") > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public static Player loadPlayer(String username, String email){
        try {
            Connection connection = getConnection();

            Statement statement = connection.createStatement();
            String query;

            if(email != null) {
                query = "SELECT * FROM users " +    //Remember to set user input to lowercase
                        "WHERE lower(username) = '" + username.toLowerCase() + "' AND lower(email) = '" + email.toLowerCase() + "';";
            }else{
                query = "SELECT * FROM users " +    //Remember to set user input to lowercase
                        "WHERE lower(username) = '" + username.toLowerCase() + "';";
            }
            ResultSet rs = statement.executeQuery(query);

            if (rs.next()) {
                int user_id = rs.getInt("user_id");
                String dbEmail = email;
                if(dbEmail == null){
                    dbEmail = rs.getString("email");
                }
                String gender = rs.getString("gender");
                String birthday = rs.getString("birthdate");

                Player player = new Player();
                player.setPlayerId(user_id);
                player.setUsername(username);
                player.setEmail(dbEmail);
                player.setGender(gender);
                player.setBirthday(birthday);
                player.setGuestStatus(false);
                connection.close();
                return player;
            } else {
                System.out.println("No player found with the given username and email.");
                connection.close();
                return null;
            }

        } catch (SQLException exc) {
            System.out.println("Error during loading player.");
            exc.printStackTrace();
            return null;
        }
    }

    public static int getHighestPlayersScore(Player player){
        try {
            Connection connection = getConnection();
            Statement statement = connection.createStatement();

            int userId = player.getPlayerId();
            String query = "SELECT score FROM game_sessions WHERE user_id=" + userId + ";";
            ResultSet rs = statement.executeQuery(query);

            if (rs.next()) {
                int score = rs.getInt("score");
                connection.close();
                return score;
            } else {
                System.out.println("No player's score found in the database.");
                connection.close();
                return -1;
            }

        } catch (SQLException exc) {
            System.out.println("Error during loading player.");
            exc.printStackTrace();
            return -1;
        }
    }
    public static void updateHighestPlayersScore(Player player, int score){
        try {
            Connection connection = getConnection();
            Statement statement = connection.createStatement();

            int highestScore = Database.getHighestPlayersScore(player);

            if(highestScore < score) {
                String query = "UPDATE game_sessions SET score=" + score + " WHERE user_id=" + player.getPlayerId() + ";";
                statement.executeUpdate(query);
                connection.close();
            }

        } catch (SQLException exc) {
            System.out.println("Error during loading player.");
            exc.printStackTrace();
        }
    }

    //Generated by ChatGPT
    public static void loadWordsFromFileAndStoreInDatabase(String filePath) {
        try (Connection connection = getConnection()) {

            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS words (
                    word_id INT CONSTRAINT pk_word_id PRIMARY KEY,
                    word_to_guess VARCHAR(255) NOT NULL UNIQUE 
                );
                """;
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createTableSQL);
            }

            //check if the table is empty
            String checkTableSQL = "SELECT COUNT(*) AS count FROM words";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(checkTableSQL)) {
                if (rs.next() && rs.getInt("count") > 0) {
//                    System.out.println("The table already contains words.");
                    return;
                }
            }



            // Read words from the file and insert into the database
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;

                // Query to find the maximum word_id in the table
                String getMaxIdSQL = "SELECT COALESCE(MAX(word_id), 0) AS max_id FROM words";
                int currentMaxId = 0;
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(getMaxIdSQL)) {
                    if (rs.next()) {
                        currentMaxId = rs.getInt("max_id");
                    }
                }

                String insertSQL = "INSERT INTO words (word_id, word_to_guess) VALUES (?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
                    int wordId = currentMaxId + 1; // Start with the next available ID
                    while ((line = reader.readLine()) != null) {
                        String word = line.trim().replaceAll("[^a-zA-Z]", ""); // Clean up non-alphabetic characters
                        if (!word.isEmpty()) {
                            pstmt.setInt(1, wordId);
                            pstmt.setString(2, word);
                            pstmt.addBatch(); // Add to batch for efficient insertion
                            wordId++;
                        }
                    }
                    pstmt.executeBatch(); // Execute batch insertions
//                    System.out.println("Words have been successfully stored in the database.");
                }
            } catch (IOException e) {
                System.out.println("Error reading the file: " + e.getMessage());
            }
        } catch (SQLException exc) {
            System.out.println("An error occurred while interacting with the database.");
            exc.printStackTrace();
        }
    }

    //08_IMPLEMENT_DDL
    public static void createDatabase() {
        try {
            Connection connection = getConnection();
            Statement statement = connection.createStatement();

//            statement.executeUpdate("CREATE TABLE IF NOT EXISTS words (\n" +
//                                        "  word_id INT CONSTRAINT pk_word_id PRIMARY KEY,\n" +
//                                        "    word_to_guess VARCHAR(255) NOT NULL\n" +
//                                        ");");

            statement.executeUpdate("\n" +
                    "CREATE TABLE IF NOT EXISTS users (\n" +
                    "     user_id INT CONSTRAINT pk_user_id PRIMARY KEY,\n" +
                    "     username VARCHAR(255) NOT NULL,\n" +
                    "     gender VARCHAR(255) NOT NULL,\n" +
                    "     email VARCHAR(255) NOT NULL,\n" +
                    "     birthdate DATE NOT NULL\n" +
                    ");");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS game_sessions (\n" +
                    "     game_session_id INT,\n" +
                    "     user_id INT,\n" +
                    "     duration INT NOT NULL,\n" +
                    "     time_stamp TIMESTAMP NOT NULL,\n" +
                    "     word_id INT CONSTRAINT fk_word_id REFERENCES words(word_id),\n" +
                    "     attempts_num INT,\n" +
                    "     score INT CONSTRAINT ch_score_isPositive CHECK (score >= 0),\n" +
                    "     constraint pk_game_session_id PRIMARY KEY (game_session_id, user_id),\n" +
                    "     constraint fk_user_id FOREIGN KEY (user_id) REFERENCES users(user_id)\n" +
                    ");");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS cell (\n" +
                    "    cell_id SERIAL PRIMARY KEY,\n" +
                    "    game_session_id INT NOT NULL,\n" +
                    "    user_id INT NOT NULL,\n" +
                    "    position INT NOT NULL,\n" +
                    "    character VARCHAR(1) NOT NULL,\n" +
                    "    status VARCHAR(20),\n" +
                    "    CONSTRAINT fk_game_session FOREIGN KEY (game_session_id, user_id) REFERENCES game_sessions(game_session_id, user_id)\n" +
                    ");");

        } catch (SQLException exc) {
            System.out.println("Error creating the database.");
            exc.printStackTrace();
        }
    }

    public static Guess getRandomWord() {
        try {
            Connection connection = getConnection();
            Statement statement = connection.createStatement();

            ResultSet rs = statement.executeQuery("SELECT * FROM words " +
                                                             "ORDER BY random() LIMIT 1");

            if (rs.next()) {
                int guessId = rs.getInt("word_id");
                String word = rs.getString("word_to_guess");
                return new Guess(guessId, word);
            }
        } catch (SQLException exc) {
            System.out.println("Error getting random word from the database.");
            exc.printStackTrace();
        }
        return null;
    }

    public static void clearProgress(GameSession game) {
        try(Connection connection = getConnection()){
            int lastPosition = getLastLetterIndex(game.getPlayer().getPlayerId());
            int gameSessionId = game.getGameId();
            int playerId = game.getPlayer().getPlayerId();
            if(lastPosition > 0){
                String clearProgressSQL = """
                    DELETE FROM cell WHERE game_session_id=? AND user_id=?;
                """;
                PreparedStatement pstmt;
                pstmt = connection.prepareStatement(clearProgressSQL);
                pstmt.setInt(1, gameSessionId);
                pstmt.setInt(2, playerId);
                pstmt.executeUpdate();
            }
        }catch (SQLException e) {
            System.out.println("Error saving game.");
            e.printStackTrace();
        }
    }

    public static boolean checkForGameId(Player player){
        try(Connection connection = getConnection()){
            int userId = player.getPlayerId();
            String checkForGameSessionSQL = """
                    SELECT user_id FROM game_sessions WHERE user_id=?;
                    """;
            try (PreparedStatement pstmt = connection.prepareStatement(checkForGameSessionSQL)) {
                pstmt.setInt(1,userId);
                ResultSet rs = pstmt.executeQuery();
                if(rs.next()){
                    if(userId == rs.getInt("user_id")){
                        return true;
                    }
                }
                System.out.println("Words have been successfully stored in the database.");
            }
        }catch (SQLException e) {
            System.out.println("Error saving game.");
            e.printStackTrace();
        }
        return false;
    }

    public static int createGameId() {
        try (Connection connection = getConnection()) {
            // Query to get the maximum game_session_id
            String checkForGameSessionSQL = """
                SELECT MAX(game_session_id) AS max_game_session_index FROM game_sessions;
                """;

            // Prepare and execute the SQL query
            try (PreparedStatement pstmt = connection.prepareStatement(checkForGameSessionSQL)) {
                ResultSet rs = pstmt.executeQuery();

                // If the table is empty, start from 1
                if (rs.next()) {
                    int maxId = rs.getInt("max_game_session_index");

                    // Check for gaps in game_session_id (optional)
                    for (int i = 1; i <= maxId; i++) {
                        if (!isGameIdUsed(connection, i)) {
                            return i; // Return the first available ID
                        }
                    }

                    // If no gaps, return the next ID in sequence
                    return maxId + 1;
                } else {
                    return 1; // If the table is empty, start at 1
                }
            }
        } catch (SQLException e) {
            System.out.println("Error creating game session ID.");
            e.printStackTrace();
        }
        return -1; // Return -1 if an error occurs
    }

    // Helper method to check if a game_session_id is in use
    private static boolean isGameIdUsed(Connection connection, int gameId) throws SQLException {
        String checkGameIdSQL = "SELECT 1 FROM game_sessions WHERE game_session_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(checkGameIdSQL)) {
            pstmt.setInt(1, gameId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Returns true if the ID exists
        }
    }

    public static int getGameId(Player player){
        try(Connection connection = getConnection()){
            int userId = player.getPlayerId();
            String checkForGameSessionSQL = """
                    SELECT game_session_id FROM game_sessions WHERE user_id=?;
                    """;
            try (PreparedStatement pstmt = connection.prepareStatement(checkForGameSessionSQL)) {
                pstmt.setInt(1,userId);
                ResultSet rs = pstmt.executeQuery();
                if(rs.next()){
                    return rs.getInt("game_session_id");
                }
                System.out.println("Words have been successfully stored in the database.");
            }
        }catch (SQLException e) {
            System.out.println("Error saving game.");
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public String toString() {
        return String.format("""
                Database name - %-10s
                Username - %-10s
                Password - %-10s
                URL: - %-10s
                """, DB_NAME, USERNAME, PASSWORD, CONNECTION_URL);
    }
}
