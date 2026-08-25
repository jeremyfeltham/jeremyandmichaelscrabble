import java.util.List;
import java.util.ArrayList;

// this is the entry point of the program, it just sets up the window and hands off
// control to GameManager once the player hits start
public class Scrabble {
    GameRenderer gameRenderer;

    public static void main(String[] args) {
        Scrabble scrabble = new Scrabble();
        scrabble.start();
    }   

    public void start(){
        gameRenderer = new GameRenderer();
        gameRenderer.initialiseFrame();
        startMenu();
    }

    // playerTypes comes from the dropdowns on the menu screen
    // 0 = human, 1-3 = bot at that difficulty, 4 = no player in that slot
    public void startGame(int[] playerTypes){
        GameManager gameManager = new GameManager();
        WordDictionary dictionary = new WordDictionary("words.txt");
        List<Player> players = new ArrayList<>();
        for (int playerType: playerTypes){
            if (playerType == 0){
                players.add(new Player());
            }
            else if (playerType != 4){
                // anything thats not human and not "none" must be a bot
                Player newPlayer = new Player();
                newPlayer.isBot = true;
                newPlayer.botDifficulty = playerType-1;
                players.add(newPlayer);
            }
        }

        gameManager.initialiseGame(gameRenderer, players, dictionary, this);

    }
    
    // basically the same as startGame right now, load functionality isnt done yet
    public void loadGame(int[] playerTypes){
        GameManager gameManager = new GameManager();
        WordDictionary dictionary = new WordDictionary("words.txt");
        List<Player> players = new ArrayList<>();
        for (int playerType: playerTypes){
            if (playerType == 0){
                players.add(new Player());
            }
            else if (playerType != 4){
                Player newPlayer = new Player();
                newPlayer.isBot = true;
                newPlayer.botDifficulty = playerType-1;
                players.add(newPlayer);
            }
        }

        gameManager.initialiseGame(gameRenderer, players, dictionary, this);

        //to-do: load in board, player hands, player scores, current player, etc from save file.
    }

    public void startMenu(){
        gameRenderer.initialiseMainMenuRender(this);
    }
    
    public void startWinScreen(int winner, int score){
        gameRenderer.initialiseWinnerScreen(winner, this, score);
    }
}
