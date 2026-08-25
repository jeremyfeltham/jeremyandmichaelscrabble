import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BorderLayout;

// is one square on the board. Stores the letter and multiplier and the actual jlabel stuff just to make life easier and not have to add more for loops in game renderer
public class Square {
    public char letter;
    public JPanel visual;
    public boolean placeable; // true if the currently selected tile could legally go here
    public int multiplier = 1;
    public int tileX;
    public int tileY;
    public int posX, posY;
    public boolean isWordMulti = false; // true = multiplies the whole word, false = just this letter
    public boolean isNew = false; // true if this letter was placed this turn (not scored yet)

    // point values for each letter, same as real scrabble
    private int getLetterValue(char letter) {
        if (letter == 'A' || letter == 'E' || letter == 'I' || letter == 'O' || letter == 'U' || letter == 'L' || letter == 'N' || letter == 'S' || letter == 'T' || letter == 'R') {
            return 1;
        }
        if (letter == 'D' || letter == 'G') {
            return 2;
        }
        if (letter == 'B' || letter == 'C' || letter == 'M' || letter == 'P') {
            return 3;
        }
        if (letter == 'F' || letter == 'H' || letter == 'V' || letter == 'W' || letter == 'Y') {
            return 4;
        }
        if (letter == 'K') {
            return 5;
        }
        if (letter == 'J' || letter == 'X') {
            return 8;
        }
        if (letter == 'Q' || letter == 'Z') {
            return 10;
        }
        return 0; 
    }

    // isVisual is false for the "fake" boards the bot uses to test moves, since those
    // dont need to actually be drawn on screen, saves making a bunch of swing components
    public Square(char letter, int posX, int posY, GameManager manager, boolean isVisual){
        this.letter = letter;
        this.posX = posX;
        this.posY = posY;
        if (isVisual){
            Color baseBgColor = new Color(45, 45, 68);
            Color baseBorderColor = new Color(74, 85, 162);
            
            // REPLACED: Swapped standard JPanel with RoundedPanel (8px corner curve radius)
            visual = new RoundedPanel(new BorderLayout(), 8, baseBorderColor);
            visual.setBackground(baseBgColor);
            visual.setBounds(posX, posY, 50, 50); 
            
            // top right corner shows the points value of the letter
            int pointsValue = getLetterValue(letter);
            JLabel pointsLabel = new JLabel(String.valueOf(pointsValue), JLabel.RIGHT); 
            pointsLabel.setFont(new Font("Monospaced", Font.PLAIN, 14)); 
            pointsLabel.setForeground(new Color(255, 180, 0)); 
            visual.add(pointsLabel, BorderLayout.NORTH);
            
            // middle shows the actual letter
            JLabel letterLabel = new JLabel(String.valueOf(letter), JLabel.CENTER);
            letterLabel.setFont(new Font("Monospaced", Font.BOLD, 18)); 
            letterLabel.setForeground(new Color(255, 255, 255));
            visual.add(letterLabel, BorderLayout.CENTER);
            
            // bottom shows the multiplier text (gets overwritten properly in updateVisual)
            JLabel multiplierLabel = new JLabel("3x", JLabel.CENTER); 
            multiplierLabel.setFont(new Font("Monospaced", Font.PLAIN, 10)); 
            multiplierLabel.setForeground(new Color(180, 180, 200)); 
            visual.add(multiplierLabel, BorderLayout.SOUTH);

            // tell the game manager whenever this square gets clicked so it can try place a tile
            visual.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    manager.handleSquareClick(tileX, tileY);
                }
            });
        }
    }


    // redraws this square's labels and colour based on its current state,
    // gets called every frame for every square on the board
    public void updateVisual(){
        JLabel letterLabel = (JLabel) visual.getComponent(1);
        letterLabel.setText(String.valueOf(letter));
        JLabel pointsLabel = (JLabel) visual.getComponent(0);
        if (letter != ' '){
            pointsLabel.setText(String.valueOf(getLetterValue(letter)));
        }
        else{
            pointsLabel.setText("");
        }
        JLabel multiLabel = (JLabel) visual.getComponent(2);
        if (multiplier == 1){
            multiLabel.setText("");
        }
        else if (isWordMulti){
            multiLabel.setText("Word " + String.valueOf(multiplier) + "x");
        }
        else{
            multiLabel.setText("Letter " + String.valueOf(multiplier) + "x");
            multiLabel.setFont(new Font("Monospaced", Font.PLAIN, 8));
        }

        // highlight the square a lighter colour if a tile could be placed here right now
        if (placeable){
            visual.setBackground(new Color(60, 60, 130));
        }
        else{
            visual.setBackground(new Color(45, 45, 68));
        }
    }
}
