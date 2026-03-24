import controller.GameSession;
import database.Database;
import view.ViewASCII;

public class Main {
    public static void main(String[] args) {
        Database.loadWordsFromFileAndStoreInDatabase("src/database/processed_words.txt");
        Database.createDatabase();
        GameSession game = new GameSession(
                new ViewASCII()
        );
        game.launchGame();
    }
}
