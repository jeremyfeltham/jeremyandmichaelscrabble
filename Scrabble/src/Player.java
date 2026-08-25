import java.util.List;
import java.util.ArrayList;

// holds all the data for one player (human or bot), no logic in here just data
public class Player {
    List<Character> hand = new ArrayList<>(); // the letters this player currently has
    public int currentScore = 0;
    public boolean isBot;
    public int botDifficulty; // 0 = easy, higher = harder (only matters if isBot is true)
}
