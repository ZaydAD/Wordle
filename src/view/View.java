package view;

import controller.GameSession;
import entities.CharFeedback;
import entities.GameState;
import entities.Player;
import enums.GameStatus;
import enums.PlayerLoadMethod;

import java.util.List;

public interface View {
    void gameBoard(GameState game);
    void keyboardFeedback(List<CharFeedback> letters);
    void leaderBoard(List<String> players, List<Integer> scores);
    int greetingMenu();
    String[] logIn(PlayerLoadMethod PLAYER_LOAD);
    String[] signUp();

    int mainMenu(Player player, GameSession gameSession);

    void gameResult(GameStatus status, int i, int curAttempt, String wordToGuess);
    void helpMenu();
    String inputWord(GameState game);
    void clearScreen();
}