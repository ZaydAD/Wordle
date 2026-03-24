package controller;

import database.Database;
import entities.CharFeedback;
import entities.GameState;
import entities.Player;
import enums.GameStatus;
import enums.LetterStatus;
import enums.PlayerLoadMethod;
import java.sql.Timestamp;
import java.util.List;
import view.View;

public class GameSession {
    private int gameId;
    private GameState gameState;
    private Player player;
    private View view;
    private final PlayerLoadMethod PLAYER_LOAD = PlayerLoadMethod.LIST;
    private boolean loadGameAvailable;
    private int secondsPlayed = 0; // Stores the number of seconds played
    private long startTimeMillis;  // Stores the start time in milliseconds

    public GameSession(View view){
        setGameId(0);
        setGameState(new GameState(true));
        setPlayer(new Player());
        setView(view);
    }

    public boolean isLoadGameAvailable() { return loadGameAvailable; }

    public int getDuration(){
        // Calculate the elapsed time in seconds
        long elapsedMillis = System.currentTimeMillis() - startTimeMillis; // Get the difference in milliseconds
        secondsPlayed = (int) (elapsedMillis / 1000); // Convert milliseconds to seconds and store
        return secondsPlayed;
    }

    public Timestamp getTimestamp() { return new Timestamp(System.currentTimeMillis()); }

    public int getGameId() { return gameId; }
    public GameState getGameState() { return gameState; }
    public Player getPlayer() { return player; }
    public View getView() { return view; }

    public void setGameId(int gameId) { this.gameId = gameId; }
    public void setGameState(GameState gameState) { this.gameState = gameState; }
    public void setPlayer(Player player) { this.player = player; }
    public void setView(View view) { this.view = view; }

    public void launchGame() {
        Database.loadWordsFromFileAndStoreInDatabase("src/database/processed_words.txt");
        int option = view.greetingMenu(); // Displays the greeting menu and receives player's option
        switch (option) {
            case 1:
                String[] LoginDetails = view.logIn(PLAYER_LOAD); // Displays the login form depending on set login option and returns provided credentials
                player.setUsername(LoginDetails[0]);

                Player playerData = Database.loadPlayer(player.getUsername(), player.getEmail()); // Loads player's data from the database
                if(PLAYER_LOAD == PlayerLoadMethod.LOG_IN_FORM){ // Defines a way to apply email to player object
                    player.setEmail(LoginDetails[1]);
                }else{
                    player.setEmail(playerData.getEmail());
                }
                player.setPlayerId(playerData.getPlayerId());
                player.setBirthday(playerData.getBirthday());
                player.setGender(playerData.getGender());
                player.setGuestStatus(playerData.getGuestStatus());

                break;
            case 2:
                String[] data = view.signUp(); // Displays the sign-up form and returns provided credentials
                player.setUsername(data[0]);
                player.setGender(data[1]);
                player.setEmail(data[2]);
                player.setBirthday(data[3]);
                player.setGuestStatus(Boolean.parseBoolean(data[4]));

                Database.addNewPlayer(player); // Adds new player to the database
                break;
            case 3:
                view.clearScreen();
                break;
        }
        loadGameAvailable = false; // Defines if the user can use the load last game option

        if(Database.checkForGameId(player)){ // Checks if a game session was created for the player and if some game progress was saved previously
            if(Database.getLastLetterIndex(player.getPlayerId()) != 0) {
                loadGameAvailable = true;
            }
            this.setGameId(Database.getGameId(player));
        }else{
            this.setGameId(Database.createGameId());
        }

        boolean leaveLoop = false; // A condition which loops the main loop below
        do{
            option = view.mainMenu(player, this); // Displays the main menu and some simple data about the player
            switch (option) {
                case 1:
                    startNewGame();
                    break;
                case 2:
                    if(loadGameAvailable) {
                        loadLastGame();
                    }
                    break;
                case 3: // Displays the leaderboard and gets necessary data for it
                    List<String> topPlayers = Database.loadPlayersNames(5);
                    List<Integer> topScores = Database.loadBestScores(5);
                    if(topPlayers != null && topScores != null) {
                        view.leaderBoard(topPlayers, topScores);
                    }else{
                        System.out.println("Looks like currently we can't load the leaderboard... Sorry!");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
                case 4:
                    view.helpMenu();
                    break;
                case 5:
                    System.out.println("Exiting the game. Goodbye!");
                    leaveLoop = true;
                    break;
            }
        }while(!leaveLoop); // leaves the game
    }

    public void startNewGame() { // Sets default values for new game
        Database.clearProgress(this);
        this.gameState.setCurrentAttempt(0);
        this.gameState.setGuess(Database.getRandomWord());
        this.gameState.setStatus(GameStatus.IN_PROGRESS);
        this.gameState.setFeedback(null, true);
        this.gameState.setWords(null, true);

        startTimeMillis = System.currentTimeMillis(); // Store the start time

        playTheGame(0,0); // Begins new game with start values
    }

    public void loadLastGame() { // Sets default values for load game
         GameState gs = Database.loadGame(getPlayer().getPlayerId()); // Gets mandatory data to load the last game
         if (gs != null) { // If there are data for previous game then it applies them into the game
             this.gameState = gs;
             this.gameState.setCurrentAttempt(gs.getCurrentAttempt());
             this.gameState.setGuess(gs.getGuess());
             this.gameState.setStatus(GameStatus.IN_PROGRESS);
             this.gameState.setFeedback(gameState.getFeedback(), false);
             this.gameState.setWords(gameState.getWords(), false);

             startTimeMillis = System.currentTimeMillis(); // Store the start time

             int lastLetterIndex = Database.getLastLetterIndex(getPlayer().getPlayerId()); // Fetches the last letter index from the database (position from the cell table)

             playTheGame(lastLetterIndex % 5, gs.getCurrentAttempt()); // Pass the last letter index in a row to the playTheGame method
         } else {
             System.out.println("Error loading last game.");
         }
 
    }

    public void playTheGame(int lastLetterIndex, int lastAttempt){
        GameStatus gameProgress = this.gameState.getStatus();
        String wordToGuess = this.gameState.getGuess().getWord();
        int letterIndex = lastLetterIndex;
        int curAttempt = lastAttempt;
        int pointsEarned = 0;

        do {
            view.gameBoard(gameState); // Prints/updates the board
            view.keyboardFeedback(gameState.getFeedback());// Prints/updates the keyboard
            String input = view.inputWord(gameState).toUpperCase(); // Gets user's input

            if(input.equals("!EXIT")){
                gameProgress = GameStatus.SAVED;
                if(!player.getGuestStatus()) {
                    Database.saveGame(this);
                }else{
                    loadGameAvailable = false;
                }
            }else {
                for (char letter : input.toLowerCase().toCharArray()) { // Check for each letter in user's input
                    LetterStatus letterStatus = LetterStatus.WRONG; // Set default status if nothing will change
                    if (letter == wordToGuess.charAt(letterIndex)) { // Check if a letter is in right place
                        letterStatus = LetterStatus.CORRECT;
                    } else {
                        for (char guessLetter : wordToGuess.toCharArray()) { // Check if any of guess's letter matches the letter
                            if (letter == guessLetter) {
                                letterStatus = LetterStatus.IN_WORD;
                                break;
                            }
                        }
                    }
                    gameState.addFeedback(new CharFeedback((curAttempt * 5 + letterIndex),
                            (char) (letter - 32), // Change the letter to uppercase format by changing ASCII number
                            letterStatus));
                    letterIndex++;
                }

                curAttempt = gameState.getCurrentAttempt();
                if (5 - gameState.getFeedback().size() % 5 == 5) { // Check if row is completely filled
                    StringBuilder word = new StringBuilder();
                    for (int i = 0; i < 5; i++) {
                        word.append(gameState.getFeedback().get((curAttempt) * 5 + i)); // append the word to the right row
                    }
                    gameState.addWord(word.toString());
                    curAttempt++;
                    letterIndex = 0;
                }
                loadGameAvailable = true;
                gameState.setCurrentAttempt(curAttempt);
                if (gameState.getLastWord().toLowerCase().equals(wordToGuess)) {
                    gameProgress = GameStatus.WIN;
                    pointsEarned = countPoints(gameState);

                    player.updateScore(pointsEarned);

                    Database.saveGame(this);
                    Database.updateHighestPlayersScore(player, pointsEarned);
                    Database.clearProgress(this);

                    loadGameAvailable = false;
                } else if (gameState.getCurrentAttempt() == gameState.getAttemptsMax()) { // Check if the game should be finished
                    gameProgress = GameStatus.LOST;

                    Database.saveGame(this);
                    Database.clearProgress(this);

                    loadGameAvailable = false;
                }
            }
        }while(gameProgress == GameStatus.IN_PROGRESS);

        view.gameBoard(gameState);
        view.keyboardFeedback(gameState.getFeedback());
        view.gameResult(gameProgress, pointsEarned, curAttempt, wordToGuess);
    }

    private int countPoints(GameState gameState) {
        int score = player.getPlayerStats().getScore();
        int ATTEMPTS_MAX = gameState.getAttemptsMax();
        int LAST_ATTEMPT = gameState.getCurrentAttempt();

        List<CharFeedback> feedbacks = gameState.getFeedback();

        for(CharFeedback feedback : feedbacks){
            score += Integer.parseInt(String.valueOf(feedback.getStatus())) * (ATTEMPTS_MAX - LAST_ATTEMPT);
        }

        score += (ATTEMPTS_MAX - LAST_ATTEMPT);

        return score;
    }

    @Override
    public String toString() {
        return String.format("""
                gameId - %-10s
                GameState - %-10s
                Player - %-10s
                View - %-10s""", getGameId(), getGameState(), getPlayer(), getView());
    }
}