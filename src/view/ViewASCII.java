package view;

import controller.GameSession;
import database.Database;
import entities.CharFeedback;
import entities.GameState;
import entities.Player;
import enums.Colors;
import enums.GameStatus;
import enums.LetterStatus;
import enums.PlayerLoadMethod;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViewASCII implements View {
    private final Scanner scanner = new Scanner(System.in);
    private final int COLUMNS = 5;
    private final int ROWS = 6;
    private final int BOX_WIDTH = 3;
    private final int BOX_HEIGHT = 1;
    @Override
    public void gameBoard(GameState game){

        clearScreen();
        if(game.getCurrentAttempt() < game.getAttemptsMax()+1){ // Check if game is not finished yet
            int numOfChars = requiredNumOfChars(game);
            if(numOfChars == COLUMNS){ // Depend on current game progress display a message
                System.out.printf("Please provide a %d-letter word or a substring.\n", numOfChars);
            }else{
                System.out.printf("Please finish the last modified row by providing %d letters.\n", numOfChars);
            }
            System.out.println("If you wish to leave please type \"!exit\" and press Enter.");
        }

        for (int row = 0; row < ROWS; row++) { // Draw the Wordle grid with the guesses
            printLine(); // Draw the top border of each row

            for (int h = 0; h < BOX_HEIGHT; h++) { // Draw the middle part (with spaces for letters)
                for (int col = 0; col < COLUMNS; col++) {
                    System.out.print("|");

                    int letterIndex = row * COLUMNS + col; // Calculate current letter index
                    char letter;
                    LetterStatus letterStatus;
                    String color;

                    if(game.getFeedback().size() > letterIndex) { // Adjust a letter
                        letter = game.getFeedback().get(letterIndex).getCharacter();
                        letterStatus = game.getFeedback().get(letterIndex).getStatus();
                    }else{ // Adjust blank space
                        letter = ' ';
                        letterStatus = LetterStatus.NOT_FILLED;
                    }

                    color = adaptColor(letterStatus); // Adjust a color to the letter

                    spacePadding(color);
                    System.out.printf("%s%s%s",color,letter, Colors.RESET); // Print cell's content
                    spacePadding(color);
                }
                System.out.println("|");
            }
        }
        printLine(); // Draw the bottom border of the last row
    }

    private void printLine() {
        for (int col = 0; col < COLUMNS; col++) {
            System.out.print("+");
            for (int i = 0; i < BOX_WIDTH; i++) {
                System.out.print("-");
            }
        }
        System.out.println("+");
    }

    private String adaptColor(LetterStatus letterStatus) {
        if(letterStatus==LetterStatus.WRONG){
            return Colors.RED.toString();
        } else if (letterStatus==LetterStatus.IN_WORD) {
            return Colors.YELLOW.toString();
        }else if(letterStatus==LetterStatus.CORRECT){
            return Colors.GREEN.toString();
        }else{
            return Colors.RESET.toString();
        }
    }

    private void spacePadding(String color) {
        for (int i = 0; i < BOX_WIDTH /2; i++) { // Space padding for box width
            System.out.printf("%s %s",color, Colors.RESET);
        }
    }

    @Override
    public void keyboardFeedback(List<CharFeedback> letters){
        char[] keyboardSequence = {'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'Z', 'X', 'C', 'V', 'B', 'N', 'M'};
        StringBuilder whiteSpace = new StringBuilder("  ");
        String color;
        LetterStatus letterStatus;
        for(int i=0; i<26; i++){
            letterStatus = LetterStatus.NOT_FILLED;
            for(CharFeedback letter : letters) {
               if(letter.getStatus().ordinal() > letterStatus.ordinal() && letter.getCharacter() == keyboardSequence[i]){
                   letterStatus = letter.getStatus();
               }
            }
            color = adaptColor(letterStatus);

            spacePadding(color);
            System.out.printf("%s%s%s", color, keyboardSequence[i], Colors.RESET); // Print cell's content
            spacePadding(color);
            if(i==9 || i==18){
                System.out.print("\n" + whiteSpace);
                whiteSpace.append("  ");
            }
        }
        System.out.println();
    }

    @Override
    public void leaderBoard(List<String> players, List<Integer> scores) {
        clearScreen();
        int LEADERBOARD_WIDTH = 40;
        StringBuilder leaderboardHeader = new StringBuilder();
        if(LEADERBOARD_WIDTH > 12){
            leaderboardHeader.append("=".repeat((LEADERBOARD_WIDTH - 12) / 2));
            leaderboardHeader.append(" LEADERBOARD ");
            leaderboardHeader.append("=".repeat((LEADERBOARD_WIDTH - 12) / 2));
        }
        System.out.println(leaderboardHeader);
        int PADDING = (LEADERBOARD_WIDTH/2)-4;
        for(int i = 0; i < players.size(); i++){
            System.out.printf("%-3d %-"+PADDING+"s - %"+PADDING+"d\n",
                    i + 1, players.get(i), scores.get(i));
        }
        System.out.println("\nPress enter to leave the leaderboard");
        scanner.nextLine();
        scanner.nextLine();
    }

    @Override
    public int greetingMenu(){
        clearScreen();
        System.out.println("==================================");
        System.out.println("         Welcome to WORDLE!       ");
        System.out.println("==================================");
        System.out.println("\nPress any key to continue...");
        scanner.nextLine();

        boolean inputValid = false;
        int choice = 0;
        do {  // Loop to ensure valid input for login
            clearScreen();
            System.out.println("========== Choose an option ==========");
            System.out.println("\n       1. Login ");
            System.out.println("       2. Sign up ");
            System.out.println("       3. Continue as Guest");
            System.out.println("       4. Exit ");

            try {
                choice = scanner.nextInt();
                if (choice >= 1 && choice <= 4) {
                    inputValid = true;
                }else {
                    System.out.println("Invalid choice. Please try again.");
                }
            }catch (InputMismatchException e) {
                System.out.println("Invalid input. Try again.");
                scanner.nextLine();
            }
        }while(!inputValid);

        return choice;
    }
    @Override
    public String[] logIn(PlayerLoadMethod PLAYER_LOAD){
        clearScreen();

        String[] LoginDetails = new String[2];
        if(PLAYER_LOAD == PlayerLoadMethod.LOG_IN_FORM) {
            String email = "", username = "";
            boolean validEmail;
            do {
                do {
                    System.out.println("========== Login Screen ==========\n");
                    System.out.print(" Username: ");
                    if (username.isEmpty()) {
                        username = scanner.nextLine();
                    } else {
                        System.out.println(username);
                    }
                    if (username.isEmpty()) {
                        clearScreen();
                        System.out.println("Please enter a username.");
                    }
                } while (username.isEmpty());
                System.out.print(" E-mail: ");
                if (email.isEmpty()) {
                    email = scanner.nextLine();
                } else {
                    System.out.println(email);
                }
                validEmail = isValidEmail(email);
                if (email.isEmpty()) {
                    clearScreen();
                    System.out.println("Please enter an e-mail.");
                } else if (!validEmail) {
                    clearScreen();
                    email = "";
                    System.out.println("Please enter a valid e-mail.");
                }
            } while (email.isEmpty() && !validEmail);

            LoginDetails[0] = username;
            LoginDetails[1] = email;
        }else{
            boolean validInput = false;
            do {
                List<String> players = Database.loadPlayersNames(0);
                for (int i = 0; i < players.size(); i++) {
                    System.out.printf(" %d. %s\n", i+1, players.get(i));
                }
                System.out.println("Choose a player which you would like to login as.");
                System.out.print(" Your choice: ");
                int choice = scanner.nextInt();
                if(choice >= 1 && choice <= players.size()+1) {
                    validInput = true;
                    LoginDetails[0] = players.get(choice-1);
                    LoginDetails[1] = null;
                }else{
                    clearScreen();
                    System.out.println("Invalid input. Try again.");
                }
            }while(!validInput);
        }
        return LoginDetails;
    }

    // Method to validate the email
    private boolean isValidEmail(String email) {
        // Regex for validating email format
        String emailRegex = "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~](?:\\.?[A-Za-z0-9!#$%&'*+/=?^_`{|}~])*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private boolean isValidBirthday(String birthday) {
        if(birthday.length() == 10){
            String[] unwrappedDate = birthday.split("/");
            int[] date = new int[3];
            for(int i=0; i < 3; i++){
                date[i] = Integer.parseInt(unwrappedDate[i]);
            }
            int monthMaxDays = 0;
            if(date[1] >= 1 && date[1] <= 12){
                int[] thirtyOneDays = {1,3,5,7,8,10,12};
                int[] thirtyDays = {4,6,9,11};

                for(int month : thirtyOneDays){
                    if (date[1] == month) {
                        monthMaxDays = 31;
                        break;
                    }
                }
                for(int month : thirtyDays){
                    if (date[1] == month) {
                        monthMaxDays = 30;
                        break;
                    }
                }
                if(date[1] == 2){
                    if(date[2]%4==0){
                        monthMaxDays = 29;
                    }else{
                        monthMaxDays = 28;
                    }
                }
            }else{
                return false;
            }
            if(!(date[0] >= 1 && date[0] <= monthMaxDays))
            {
                return false;
            }
            return date[2] >= 1 && date[2] <= 9999;
        }
        return false;
    }

    private boolean isValidGender(String gender) {
        return gender.equals("M") || gender.equals("F") || gender.equals("O");
    }

    @Override
    public String[] signUp(){
        clearScreen();
        scanner.nextLine();
        String username = "", email = "", birthday = "", gender = "";
        boolean validEmail, validBirthday, validGender, usernameExists = false, emailExists = false;
        do {
            do {
                do {
                    do {
                        System.out.print(" Username: ");
                        if(username.isEmpty()){
                            username = scanner.nextLine();
                        }else{
                            System.out.println(username);
                        }
                        if (username.isEmpty()) {
                            clearScreen();
                            System.out.println("Please enter a username.");
                        } else{
                            usernameExists = Database.checkIfUsernameExists(username);
                        }
                        if (usernameExists) {
                            clearScreen();
                            System.out.println("This username is taken.");
                            username = "";
                        }
                    } while (username.isEmpty() || usernameExists);
                    System.out.print(" E-mail: ");
                    if(email.isEmpty()){
                        email = scanner.nextLine();
                    }else{
                        System.out.println(email);
                    }
                    validEmail = isValidEmail(email);
                    if (email.isEmpty()) {
                        clearScreen();
                        System.out.println("Please enter an e-mail.");
                    } else if (!validEmail) {
                        clearScreen();
                        email = "";
                        System.out.println("Please enter a valid e-mail.");
                    } else{
                        emailExists = Database.checkIfEmailExists(email);
                    }
                    if (emailExists) {
                        clearScreen();
                        System.out.println("This email was already used to make the account.");
                        email = "";
                    }
                } while (email.isEmpty() || !validEmail || emailExists);
                System.out.print(" Birthday (DD/MM/YYYY): ");
                if(birthday.isEmpty()){
                    birthday = scanner.nextLine();
                }else{
                    System.out.println(birthday);
                }
                validBirthday = isValidBirthday(birthday);
                if (birthday.isEmpty()) {
                    clearScreen();
                    System.out.println("Please enter a birthday.");
                }else if(!validBirthday){
                    clearScreen();
                    birthday = "";
                    System.out.println("Please enter a birthday in valid format.");
                }
            } while (birthday.isEmpty() && !validBirthday);
            System.out.print(" Gender M/F/O (Male, Female, Other): ");
            if(gender.isEmpty()){
                gender = scanner.nextLine();
            }else{
                System.out.println(gender);
            }
            validGender = isValidGender(gender);
            if (gender.isEmpty()) {
                clearScreen();
                System.out.println("Please enter a gender.");
            }else if(!validGender){
                clearScreen();
                gender = "";
                System.out.println("Please enter a gender in valid format.");
            }
        }while(gender.isEmpty() && !validGender);
        //validate inputs or pass them to a handler method if required
        System.out.println("\nLogin details captured. Processing login...");
        return new String[]{username, gender, email, birthday, "false"};
    }
    @Override
    public int mainMenu(Player player, GameSession gameSession){
        String color = "";
        int choice = 0;
        clearScreen();

        do {
            if (!player.getGuestStatus()) {
                System.out.printf("Welcome %s! You have %d points!\n", player.getUsername(), player.getPlayerStats().getScore());
            } else {
                System.out.println("You're logged as guest. Your progress won't be saved!\n");
            }
            if (!gameSession.isLoadGameAvailable()) {
                color = "\u001B[90m";
            }
            System.out.println("========== Start Menu ==========");
            System.out.println("\n       1. Start Game ");
            System.out.println(color + "       2. Load previous game \u001B[0m");
            System.out.println("       3. Leaderboard ");
            System.out.println("       4. Instructions");
            System.out.println("       5. Exit Game \n");
            try {
                choice = scanner.nextInt();
                if(choice > 5 || choice < 1){
                    clearScreen();
                    System.out.println("Please type a number from 1 to 5.");
                    scanner.nextLine();
                }
            }catch (InputMismatchException ex){
                clearScreen();
                System.out.println("Please type a number from 1 to 5.");
                scanner.nextLine();
            }
        }while(choice < 0 || choice > 5);
        return choice;
    }

    @Override
    public void gameResult(GameStatus status, int scoreEarned, int curAttempt, String wordToGuess){
        if(status != GameStatus.SAVED) {
            System.out.println("========== The game is finished! ==========");
            if (status == GameStatus.WIN) {
                System.out.printf("Congratulations! You won on %d try!\nYou gain %d points for this game!\n", curAttempt, scoreEarned);
            } else {
                System.out.printf("Bad luck! You have lost the game!\nThe word to guess was %s.\n", wordToGuess);
            }
        }else {
            System.out.println("========== The game has been saved! ==========");
        }
        System.out.println("Press enter to return to the main menu  ");
        scanner.nextLine();
    }

    @Override
    public void helpMenu() {
        clearScreen();
        System.out.println("========== Game instruction ==========");
        System.out.println("        HOW TO PLAY THE GAME?");
        System.out.printf("""
                Your goal is to guess a %d-letter word having %d attempts. With every filled row you get a feedback
                
                for your process. Inputted letter can have a background in three colors. If a letter has red background it means it doesn't
                
                appear in the word to guess. If it's yellow then the letter appears in the word but it's placed in different place. A letter
                
                with green background means you guessed a correct letter in correct place and you're closer to guess the word!
                
                """,COLUMNS, ROWS);

        System.out.println("        HOW DO I GET POINTS?\n");
        System.out.print("""
                Your score is calculated based on three things:\s
                 1. your time spent to solve the game
                 2. number of attempts\
                
                 3. amount of guessed letters
                
                """);

        System.out.print("        CAN I SAVE MY PROGRESS?");
        System.out.print("""
                Yes you can save your progress in any time by typing !exit but remember every time you begin a new game your progress
                 is being overwritten.

                """);
        System.out.println("Press enter to leave the instruction.");
        scanner.nextLine();
        scanner.nextLine();
    }

    @Override
    public String inputWord(GameState game) {
        String input;
        boolean inputValid;
        do {
            inputValid = true;
            System.out.println("\nEnter a word: ");

            input = scanner.nextLine().toUpperCase(Locale.ROOT);
            // Set allowed characters
            Pattern digit = Pattern.compile("[0-9]");
            Pattern special = Pattern.compile ("[!@#$%&*()_+=|<>?{}\\[\\]~-]");
            // Check for digits and special characters inside user's input
            Matcher hasDigit = digit.matcher(input);
            Matcher hasSpecial = special.matcher(input);

            String errorMessage = "";
            if(!input.equals("!EXIT")) {
                if (input.isEmpty()) {
                    inputValid = false;
                }
                if (hasDigit.find()) {
                    errorMessage += "You're not allowed to use digits!\n";
                    inputValid = false;
                }
                if (hasSpecial.find()) {
                    errorMessage += "You're not allowed to use special characters!\n";
                    inputValid = false;
                }
                if (input.length() > requiredNumOfChars(game)) { // Check user's input length
                    if (requiredNumOfChars(game) == 5) {
                        errorMessage += "Your word is too long!\n";
                    } else {
                        errorMessage += "Your substring is too long!\n";
                    }
                    inputValid = false;
                }
                for (String word : game.getWords()) { // Check if user already wrote the word
                    if (input.equals(word)) {
                        errorMessage += "You already wrote the word!\n";
                        inputValid = false;
                        break;
                    }
                }
                if (!inputValid) {
                    gameBoard(game);
                    System.out.print(errorMessage);
                }
            }
        }while(!inputValid);
        return input;
    }

    @Override
    public void clearScreen() {
        System.out.print("\033[H\033[2J"); // ANSI escape codes for clearing screen
        System.out.flush();               // Ensure the screen is cleared
    }

    public int requiredNumOfChars(GameState game){
        return COLUMNS - game.getFeedback().size()%COLUMNS;
    }
}