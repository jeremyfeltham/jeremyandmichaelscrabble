import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import java.awt.event.*;
import java.text.DecimalFormat;
import java.time.Instant;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;
import java.awt.Color;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

// handles what to render, GameManager tells it what to draw, this just sets and updates the swing components
public class GameRenderer {
    JFrame frame;
    JPanel[] handVisual = new JPanel[7]; // the 7 tile slots for the players hand
    JLabel currentScoreLabel = new JLabel("Current Score: 0"); 
    JLabel[] playerScoreLabels;
    JLabel timer;
    double gameStartTime;
    GameManager gameManager;

    // point values for each letter, same as real scrabble
    private int getLetterValue(char letter) {
        //if loop spam to get value of specific letters
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

    // sets up the main window, called once when the program starts
    public void initialiseFrame(){
        //initialise the frame and stuff
        frame = new JFrame("Scrabble Jeremy and Michael");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 1000);
        frame.setLocationRelativeTo(null);
        //no layout manager because absolute pixel position is easier for the grid  
        frame.setLayout(null); 
        frame.setResizable(false); 
        frame.getContentPane().setBackground(new Color(30, 30, 46));
    }

    // clears the screen and shows the "player X wins" screen with a button back to the menu
    public void initialiseWinnerScreen(int winner, Scrabble scrabble, int score){
        frame.getContentPane().removeAll();
        JLabel title = new JLabel("Player " + (winner+1) + " Wins!");
        title.setBounds(300, 0, 600, 300);
        title.setHorizontalAlignment(SwingConstants.CENTER); 
        title.setFont(new Font("Monospaced", Font.BOLD, 60)); 
        title.setForeground(new Color(255, 255, 255));
        frame.add(title);
        JLabel points = new JLabel("Score: " + score);
        points.setBounds(300, 300, 600, 300);
        points.setHorizontalAlignment(SwingConstants.CENTER); 
        points.setFont(new Font("Monospaced", Font.BOLD, 60)); 
        points.setForeground(new Color(255, 255, 255));
        frame.add(points);
        JButton exitButton = new JButton("Back to Menu");
        exitButton.setBounds(300, 520, 600, 100);
        exitButton.setOpaque(true); 
        exitButton.setBackground(new Color(45, 45, 68));
        exitButton.setFont(new Font("Monospaced", Font.BOLD, 30)); 
        exitButton.setForeground(new Color(255, 255, 255));
        exitButton.setBorder(new LineBorder(new Color(74, 85, 162), 1));
        exitButton.setFocusPainted(false);
        frame.add(exitButton);
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scrabble.startMenu();
            }
        });
        frame.setVisible(true);
        frame.repaint();
        frame.revalidate();
    }

    // clears the screen and builds the main menu: title, load, and exit buttons, and the player selection stuff
    public void initialiseMainMenuRender(Scrabble scrabble){
        frame.getContentPane().removeAll();
        JLabel title = new JLabel("Scrabble");
        title.setBounds(300, 0, 600, 300);
        title.setHorizontalAlignment(SwingConstants.CENTER); 
        title.setFont(new Font("Monospaced", Font.BOLD, 120)); 
        title.setForeground(new Color(255, 255, 255));
        frame.add(title);
        JButton tutorialButton = new JButton("Tutorial");
        tutorialButton.setBounds(300, 410, 600, 100);
        tutorialButton.setOpaque(true); 
        tutorialButton.setBackground(new Color(45, 45, 68));
        tutorialButton.setFont(new Font("Monospaced", Font.BOLD, 30)); 
        tutorialButton.setForeground(new Color(255, 255, 255));
        tutorialButton.setBorder(new LineBorder(new Color(74, 85, 162), 1));
        tutorialButton.setFocusPainted(false);
        frame.add(tutorialButton);
        tutorialButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Google it i aint explaining ts", "No", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        JButton exitButton = new JButton("Exit");
        exitButton.setBounds(300, 520, 600, 100);
        exitButton.setOpaque(true); 
        exitButton.setBackground(new Color(45, 45, 68));
        exitButton.setFont(new Font("Monospaced", Font.BOLD, 30)); 
        exitButton.setForeground(new Color(255, 255, 255));
        exitButton.setBorder(new LineBorder(new Color(74, 85, 162), 1));
        exitButton.setFocusPainted(false);
        frame.add(exitButton);
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        String[] playerTypes = {"Human", "Bot Lvl 1", "Bot Lvl 2", "Bot Lvl 3", "None", "Cheeseburger"};
        
        @SuppressWarnings("unchecked") //idk vscodium kept complaining about unchecked conversion to JComboBox[] and this was the quick fix it showed
        JComboBox<String>[] playerDropdowns = new JComboBox[4];

        int startY = 660; 
        int rowHeight = 50; 

        // build a row for each of the 4 player slots, the default is the first 2 to human and the rest to none
        for (int i = 0; i < 4; i++) {
            int currentY = startY + (i * rowHeight);
            final int playerNum = i + 1;
            JLabel pLabel = new JLabel("Player " + playerNum + ":");
            pLabel.setBounds(300, currentY, 150, 40);
            pLabel.setFont(new Font("Monospaced", Font.PLAIN, 22));
            pLabel.setForeground(new Color(255, 255, 255));
            frame.add(pLabel);
            JComboBox<String> pDropdown = new JComboBox<>(playerTypes);
            pDropdown.setUI(new javax.swing.plaf.basic.BasicComboBoxUI()); 
            pDropdown.setFocusable(false); 
            pDropdown.setBorder(new javax.swing.border.LineBorder(new Color(74, 85, 162), 1));
            pDropdown.setBounds(450, currentY, 450, 40);
            pDropdown.setFont(new Font("Monospaced", Font.PLAIN, 18));
            pDropdown.setBackground(new Color(45, 45, 68));
            pDropdown.setForeground(new Color(255, 255, 255));
            if (i > 1) {
                pDropdown.setSelectedItem("None");
            } else {
                pDropdown.setSelectedItem("Human");
            }
            
            frame.add(pDropdown);
            playerDropdowns[i] = pDropdown; 
        }
        JButton startButton = new JButton("Start");
        startButton.setBounds(300, 300, 600, 100);
        startButton.setOpaque(true); 
        startButton.setBackground(new Color(45, 45, 68));
        startButton.setFont(new Font("Monospaced", Font.BOLD, 30)); 
        startButton.setForeground(new Color(255, 255, 255));
        startButton.setBorder(new LineBorder(new Color(74, 85, 162), 1));
        startButton.setFocusPainted(false);
        frame.add(startButton);
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int[] playerTypes = {
                playerDropdowns[0].getSelectedIndex(), 
                playerDropdowns[1].getSelectedIndex(), 
                playerDropdowns[2].getSelectedIndex(), 
                playerDropdowns[3].getSelectedIndex()};
                scrabble.startGame(playerTypes);
            }
        });
        frame.setVisible(true);
        frame.repaint();
        frame.revalidate();
    }

    // builds the main game view stuff
    public void initialiseGameRender(Square[][] squares, GameManager gameManager){
        this.gameManager = gameManager;
        frame.getContentPane().removeAll();
        for (int x = 0; x < 15; x++) {
            for (int y = 0; y < 15; y++) {
                frame.add(squares[x][y].visual);
            }
        }
        
        Color baseBgColor = new Color(45, 45, 68);
        Color baseBorderColor = new Color(74, 85, 162);

        // build the 7 hand tile slots the same way Square builds its visual
        for (int i = 0; i<7; i++){
            handVisual[i] = new RoundedPanel(new BorderLayout(), 15, baseBorderColor);
            handVisual[i].setBackground(baseBgColor);
            handVisual[i].setBounds(360 + (i * 70), 820, 50, 50); 
            
            JLabel pointsLabel = new JLabel(String.valueOf(getLetterValue(' ')), JLabel.RIGHT); 
            pointsLabel.setFont(new Font("Monospaced", Font.PLAIN, 14)); 
            pointsLabel.setForeground(new Color(255, 180, 0)); 
            handVisual[i].add(pointsLabel, BorderLayout.NORTH);
            
            JLabel letterLabel = new JLabel(String.valueOf(' '), JLabel.CENTER);
            letterLabel.setFont(new Font("Monospaced", Font.BOLD, 18)); 
            letterLabel.setForeground(new Color(255, 255, 255));
            handVisual[i].add(letterLabel, BorderLayout.CENTER);
            
            JLabel multiplierLabel = new JLabel("", JLabel.CENTER); 
            multiplierLabel.setFont(new Font("Monospaced", Font.PLAIN, 10)); 
            multiplierLabel.setForeground(new Color(180, 180, 200)); 
            handVisual[i].add(multiplierLabel, BorderLayout.SOUTH);
            frame.add(handVisual[i]);

            final int handIndex = i; 
            handVisual[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    gameManager.selectFromHand(handIndex);
                }
            });
        }
        
        RoundedButton goButton = new RoundedButton("Go!", 15, baseBorderColor);
        goButton.setBounds(970, 500, 200, 50);
        goButton.setBackground(baseBgColor);
        goButton.setFont(new Font("Monospaced", Font.BOLD, 18)); 
        goButton.setForeground(new Color(255, 255, 255));
        goButton.setFocusPainted(false);
        goButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gameManager.handleGoButton();
            }
        });
        frame.add(goButton);
        
        gameStartTime = Instant.now().getEpochSecond();
        timer = new JLabel(String.valueOf((Instant.now().getEpochSecond()-gameStartTime)));
        timer.setBounds(970, 610, 200, 50);
        timer.setFont(new Font("Monospaced", Font.BOLD, 18)); 
        timer.setForeground(new Color(255, 255, 255));
        timer.setHorizontalAlignment(SwingConstants.CENTER);
        frame.add(timer);
        
        RoundedButton undoButton = new RoundedButton("Undo Letter", 15, baseBorderColor);
        undoButton.setBounds(970, 445, 200, 50);
        undoButton.setBackground(baseBgColor);
        undoButton.setFont(new Font("Monospaced", Font.PLAIN, 18)); 
        undoButton.setForeground(new Color(255, 255, 255));
        undoButton.setFocusPainted(false);
        frame.add(undoButton);
        undoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gameManager.undoLastLetter();
            }
        });

        // one score label per player, shown on the right side of the screen
        playerScoreLabels = new JLabel[gameManager.players.size()];

        for (int i = 0; i<playerScoreLabels.length; i++){
            RoundedPanel scorePanel = new RoundedPanel(new BorderLayout(), 15, baseBorderColor);
            scorePanel.setBounds(970, 40+i*55, 200, 50);
            scorePanel.setBackground(baseBgColor);
            
            playerScoreLabels[i] = new JLabel("Player " + String.valueOf(i+1) + ": 0", JLabel.CENTER);
            playerScoreLabels[i].setFont(new Font("Monospaced", Font.PLAIN, 22)); 
            playerScoreLabels[i].setForeground(new Color(255, 255, 255));
            
            scorePanel.add(playerScoreLabels[i], BorderLayout.CENTER);
            frame.add(scorePanel);
        }

        currentScoreLabel.setBounds(970, 560, 200, 50);
        currentScoreLabel.setFont(new Font("Monospaced", Font.PLAIN, 18)); 
        currentScoreLabel.setForeground(new Color(255, 255, 255));
        currentScoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        frame.add(currentScoreLabel);

        frame.revalidate(); 
        frame.repaint();    
        frame.setVisible(true);
    }


    // updates everything on the game screen to match the current game state, gets called every frame from the game manager loop
    public void renderGame(Square[][] squares, List<Character> hand, int selectedFromHand, int currentScore){
        if (currentScore == -1){
            currentScoreLabel.setText("Invalid Turn");
        }
        else{
            currentScoreLabel.setText("Current Score: " + String.valueOf(currentScore));
        }

        DecimalFormat df = new DecimalFormat("#.##");

        double timeSpent = (double)(Instant.now().getEpochSecond()-gameStartTime)+(double)(((double)Instant.now().getNano())/1000000000.0);

        timer.setText(df.format(timeSpent));
        
        // redraw every square on the board
        for (int x = 0; x < 15; x++) {
            for (int y = 0; y < 15; y++) {
                squares[x][y].updateVisual();
            }
        }

        // hide any hand slots past however many tiles the player actually has left
        for (int i = 0; i<7; i++){
            if (i>=hand.size()){
                handVisual[i].setVisible(false);
            }
        }

        // centre the hand tiles horizontally based on how many tiles there are
        int totalSize = hand.size()*70;
        int startPos = 500-totalSize/2;

        for (int i = 0; i<hand.size(); i++){
            handVisual[i].setVisible(true);
            if (selectedFromHand == i){
                handVisual[i].setBackground(new Color(60, 60, 130));
            }
            else{
                handVisual[i].setBackground(new Color(45, 45, 68));
            }
            JLabel letterLabel = (JLabel) handVisual[i].getComponent(1);
            letterLabel.setText(String.valueOf(hand.get(i)));
            JLabel pointsLabel = (JLabel) handVisual[i].getComponent(0);
            pointsLabel.setText(String.valueOf(getLetterValue(hand.get(i))));
            handVisual[i].setBounds(startPos+(i*70), 820, 50, 50);
        }

        // highlight whoevers turn it currently is on the scoreboard
        for (int i = 0; i<playerScoreLabels.length; i++){
            if (gameManager.currentPlayer == i){
                playerScoreLabels[i].setText("Player " + String.valueOf(i+1) + ": " + String.valueOf(gameManager.players.get(i).currentScore));
                playerScoreLabels[i].setBackground(new Color(60, 60, 130));
                playerScoreLabels[i].setFont(new Font("Monospaced", Font.BOLD, 22)); 
            }
            else{
                playerScoreLabels[i].setText("Player " + String.valueOf(i+1) + ": " + String.valueOf(gameManager.players.get(i).currentScore));
                playerScoreLabels[i].setBackground(new Color(45, 45, 68));
                playerScoreLabels[i].setFont(new Font("Monospaced", Font.PLAIN, 22)); 
            }
        }

        frame.revalidate();
        frame.repaint();
        frame.setVisible(true);
    }
}

// Custom Panel for hand tiles and scoreboard tracking
class RoundedPanel extends JPanel {
    private int radius;
    private Color borderColor;

    public RoundedPanel(LayoutManager layout, int radius, Color borderColor) {
        super(layout);
        this.radius = radius;
        this.borderColor = borderColor;
        setOpaque(false); // Disables default sharp rectangle drawing
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw background
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        
        // Draw border outline
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

// Custom Button for Go and Undo actions
class RoundedButton extends JButton {
    private int radius;
    private Color borderColor;

    public RoundedButton(String text, int radius, Color borderColor) {
        super(text);
        this.radius = radius;
        this.borderColor = borderColor;
        setOpaque(false);
        setContentAreaFilled(false); // Prevents default square click overlays
        setFocusPainted(false);
        setBorderPainted(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Dynamic click behavior matching your background colors
        if (getModel().isArmed()) {
            g2.setColor(getBackground().darker());
        } else {
            g2.setColor(getBackground());
        }
        
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
