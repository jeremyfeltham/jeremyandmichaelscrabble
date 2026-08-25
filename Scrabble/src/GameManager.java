import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

// main class that does all of the game logic stuff
public class GameManager {

    Square[][] board = new Square[15][15];
    List<Player> players;
    List<Character> pool; // the letters to draw from
    GameRenderer gameRenderer;
    int currentPlayer = 0;
    int selectedFromHand = -1; // index of the tile the player has clicked on in their hand -1 is none selected
    List<Integer> tilesPlacedX, tilesPlacedY; // positions of the tiles placed so far this turn
    List<Integer> handIndexs; //used so undoing moves returns to correct position in hand
    WordDictionary dictionary;
    int currentScore; // score for the current move that isnt played yet
    boolean boardIsInteractable; /* false while a bot is processing so the human cant click during that, 
    kinda deprecated since bots aren't threaded so the game just freezes when they're thinking  */
    int lastBestScore = -1; 
    int skippedTurnsCount = 0; // if this gets too high the game ends 
    Scrabble scrabble; // access to main menu

    // point values for each letter same as real scrabble
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

    // sets up a new game 
    public void initialiseGame(GameRenderer gameRenderer, List<Player> players, WordDictionary dictionary, Scrabble scrabble){
        this.players = players;
        this.gameRenderer = gameRenderer;
        this.dictionary = dictionary;
        this.scrabble = scrabble;

        for (int x = 0; x < 15; x++) {
            for (int y = 0; y < 15; y++) {
                int posX = 70 + x * 53;
                int posY = 20 + y * 53;
                
                board[x][y] = new Square(' ', posX, posY, this, true);

                board[x][y].tileX = x;
                board[x][y].tileY = y;

                /* work out if this square is a multiplier. weird logic i obtained through some 
                random reddit post online, simply because less time-consuming then manually adding in all the multipliers */
                int multiplier = 1;
                boolean isWordMulti = false;
                int nx = (x <= 7) ? x : 14 - x;
                int ny = (y <= 7) ? y : 14 - y;
                if (nx > ny) {
                    int temp = nx;
                    nx = ny;
                    ny = temp;
                }
                if ((nx == 0 && ny == 0) || (nx == 0 && ny == 7)) {
                    multiplier = 3; 
                    isWordMulti = true;
                } else if (nx == ny && (nx == 1 || nx == 2 || nx == 3 || nx == 4 || nx == 7)) {
                    multiplier = 2; 
                    isWordMulti = true;
                } else if ((nx == 1 && ny == 5) || (nx == 5 && ny == 5)) {
                    multiplier = 3; 
                    isWordMulti = false;
                } else if ((nx == 0 && ny == 3) || (nx == 2 && ny == 6) || (nx == 3 && ny == 7) || (nx == 6 && ny == 6)) {
                    multiplier = 2;
                    isWordMulti = false;
                }
                board[x][y].multiplier = multiplier;
                board[x][y].isWordMulti = isWordMulti;
            }
        }

        setPool();
        // give everyone 7 starting letters
        for (int i = 0; i<players.size(); i++){
            for (int j = 0; j<7; j++){
                addTileFromPool(players.get(i), pool.get(0));
            }
        }


        tilesPlacedX = new ArrayList<>();
        tilesPlacedY = new ArrayList<>(); 
        handIndexs = new ArrayList<>();
        gameRenderer.initialiseGameRender(board, this);
        // thread the actual game so game renderer doesnt freeze
        Thread gameThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runGame(); 
            }
        });
        gameThread.start();
    }

    // undoes the most recently placed tile this turn
    public void undoLastLetter(){
        if (tilesPlacedX.size() == 0 || !boardIsInteractable) { return; }
        int mostRecentMove = tilesPlacedX.size()-1;
        //add the tile back to the players hand
        players.get(currentPlayer).hand.add(handIndexs.get(mostRecentMove), board[tilesPlacedX.get(mostRecentMove)][tilesPlacedY.get(mostRecentMove)].letter);
        //remove from board
        board[tilesPlacedX.get(mostRecentMove)][tilesPlacedY.get(mostRecentMove)].letter = ' ';
        //remove from letter placed history
        tilesPlacedX.remove(mostRecentMove);
        tilesPlacedY.remove(mostRecentMove);
        handIndexs.remove(mostRecentMove);
        //redoes the score for the current board
        currentScore = scoreTheBoard(board);
    }

    /* makes a copy of the board, used by the bot to test out moves without affecting
     up the real board, and sets isVisual to false here so the squares dont make jlabels */
    public Square[][] duplicateBoard(Square[][] boardToDuplicate){
        Square[][] duplicatedBoard = new Square[15][15];
        for (int x = 0; x<15; x++){
            for (int y = 0; y<15; y++){
                int posX = 70 + x * 58;
                int posY = 40 + y * 58;
                Square newSquare = new Square(boardToDuplicate[x][y].letter, posX, posY, this, false);
                newSquare.tileX = x;
                newSquare.tileY = y;
                newSquare.isNew = boardToDuplicate[x][y].isNew;
                duplicatedBoard[x][y] = newSquare;
            }
        }
        return duplicatedBoard;
    }

    // called when the human presses the button to end their turn
    public void handleGoButton(){
        if (currentScore == -1) { return; } // cant end turn on an invalid word
        // draw back to 7 tiles, or as many as are left in the pool
        int tilesToAdd = (7-players.get(currentPlayer).hand.size());
        tilesToAdd = Math.min(tilesToAdd, pool.size());
        for (int i = 0; i<tilesToAdd; i++){ 
            addTileFromPool(players.get(currentPlayer), pool.get(0));
        }
        players.get(currentPlayer).currentScore += currentScore;
        currentPlayer++;
        if (currentPlayer >= players.size()){
            currentPlayer = 0;
        }
        // track how many turns in a row have been skipped, so the game can be ended if everybody is stuck
        if (tilesPlacedX.size() == 0){
            skippedTurnsCount++;
        }
        else{
            skippedTurnsCount = 0;
        }
        tilesPlacedX = new ArrayList<>();
        tilesPlacedY = new ArrayList<>();
        handIndexs = new ArrayList<>();
        currentScore = 0;
        for (int x = 0; x<15; x++){
            for (int y = 0; y<15; y++){
                board[x][y].isNew = false;
            }
        }

    }

    // works out who has the highest square and shows the win screen
    private void endTheGame(){
        int winner = 0;
        int bestScore = -1;
        for (int i = 0; i<players.size(); i++){
            if (players.get(i).currentScore > bestScore){
                winner = i;
                bestScore = players.get(i).currentScore;
            }
        }
        scrabble.startWinScreen(winner, players.get(winner).currentScore);
    }

    /* works out the score for the current board and returns -1 if invalid */
    private int scoreTheBoard(Square[][] boardToScore){
        List<List<Square>> wordsToScore = new ArrayList<>();
        /*for all newly placed tiles, get all of the words that they are part of by
        checking horizontally, going left from each placed tile until the start of the word, using that as a start point,
        then adding to the words found list all the letters going right until an empty square or the side of the board */
        for (int placedLetterIndex = 0; placedLetterIndex<tilesPlacedX.size(); placedLetterIndex++){
            List<Square> wordToScore = new ArrayList<>();
            int letterPosX = tilesPlacedX.get(placedLetterIndex);
            while (boardToScore[letterPosX][tilesPlacedY.get(placedLetterIndex)].letter != ' '){
                if (letterPosX>0){
                    letterPosX--;
                }
                else{
                    letterPosX--;
                    break;
                }
            }
            letterPosX++;
            while (letterPosX < 15 && boardToScore[letterPosX][tilesPlacedY.get(placedLetterIndex)].letter != ' '){
                wordToScore.add(boardToScore[letterPosX][tilesPlacedY.get(placedLetterIndex)]);
                letterPosX++;
            }

            // only counts if its more than 1 letter long
            if (wordToScore.size() > 1){
                wordsToScore.add(wordToScore);
            }
        }

        // same thing again but vertically
        for (int placedLetterIndex = 0; placedLetterIndex<tilesPlacedX.size(); placedLetterIndex++){
            List<Square> wordToScore = new ArrayList<>();
            int letterPosY = tilesPlacedY.get(placedLetterIndex);
            while (boardToScore[tilesPlacedX.get(placedLetterIndex)][letterPosY].letter != ' '){
                if (letterPosY>0){
                    letterPosY--;
                }
                else{
                    letterPosY--;
                    break;
                }
            }
            letterPosY++;
            while (letterPosY < 15 && boardToScore[tilesPlacedX.get(placedLetterIndex)][letterPosY].letter != ' '){
                wordToScore.add(boardToScore[tilesPlacedX.get(placedLetterIndex)][letterPosY]);
                letterPosY++;
            }
            if (wordToScore.size() > 1){
                wordsToScore.add(wordToScore);
            }
        }
        
        /* the board finds a new word(s) for every letter placed, however words obviously have more than 1 letter, \
        so you end up getting duplicates of the same word equal to the words length, this just loops through, finds duplicates, deletes them
        i did this instead of just not adding duplicates in the first place because this is easier and im lazy */
        boolean hasRemoved = false;
        boolean hasFinished = false;
        while (!hasFinished){
            //this pairs every word with every single other word and sees if they are the same, if they are, delete it
            for (int i = 0; i< wordsToScore.size(); i++){
                for (int j = 0; j < wordsToScore.size(); j++){
                    if (i==j) { continue; }
                    if (wordsToScore.get(i).get(0).tileX==wordsToScore.get(j).get(0).tileX && 
                        wordsToScore.get(i).get(1).tileX==wordsToScore.get(j).get(1).tileX && 
                        wordsToScore.get(i).get(0).tileY==wordsToScore.get(j).get(0).tileY && 
                        wordsToScore.get(i).get(1).tileY==wordsToScore.get(j).get(1).tileY){
                            wordsToScore.remove(j);
                            hasRemoved = true;
                            break;
                        }
                }
                if (hasRemoved) { break; }
            }
            if (hasRemoved){
                hasRemoved = false;
            }
            else{
                hasFinished = true;
            }
        }

        int totalScore = 0; 


        //if they havent done anything, make it score 0 so they can still skip.
        if (wordsToScore.size() == 0){
            return 0;
        }

        // make sure every word formed is actually a real word before scoring anything, if a non word is found, return -1, which means invalid turn
        for (int i = 0; i<wordsToScore.size(); i++) {
            String wordToTest = "";
            for (int k = 0; k<wordsToScore.get(i).size(); k++) {
                wordToTest += wordsToScore.get(i).get(k).letter;
            }
            if (!dictionary.isValidWord(wordToTest)) {
                return -1; 
            }
        }

        /* add up the points for every word, applying letter/word multipliers only
         for tiles that were placed this turn, old tiles dont get bonus multipliers again */
        for (int i = 0; i<wordsToScore.size(); i++){
            int currentScore = 0;
            int currentWordMulti = 1;

            //keep a current score, for each letter in each word, add up there letter value multiplied by their letter multiplier if the square has a letter multiplier and is new
            for (int j = 0; j<wordsToScore.get(i).size(); j++){
                if (wordsToScore.get(i).get(j).isNew){
                    if (wordsToScore.get(i).get(j).isWordMulti) {
                        //if its a word multiplier, add it to the current word multiplier, and the letter score
                        currentWordMulti = currentWordMulti * wordsToScore.get(i).get(j).multiplier;
                        currentScore += getLetterValue(wordsToScore.get(i).get(j).letter);
                    }
                    else{
                        currentScore += getLetterValue(wordsToScore.get(i).get(j).letter) * wordsToScore.get(i).get(j).multiplier;
                    }
                }
                else{
                    currentScore += getLetterValue(wordsToScore.get(i).get(j).letter);
                }
            }
            // add the total score multiplied by the total word multiplier for the word
            totalScore += currentScore*currentWordMulti;
        }

        // bonus points for using your whole hand in one turn
        if (tilesPlacedX.size() == 7){
            totalScore += 50;
        }

        return totalScore;
    }

    // fills the pool with the right amount of each letter from real scrabble rules, then shuffles it
    private void setPool(){
        pool = new ArrayList<>();
        addTiles('E', 12);
        addTiles('A', 9);
        addTiles('I', 9);
        addTiles('O', 8);
        addTiles('N', 6);
        addTiles('R', 6);
        addTiles('T', 6);
        addTiles('D', 4);
        addTiles('L', 4);
        addTiles('S', 4);
        addTiles('U', 4);
        addTiles('G', 3);
        addTiles('B', 2);
        addTiles('C', 2);
        addTiles('F', 2);
        addTiles('H', 2);
        addTiles('M', 2);
        addTiles('P', 2);
        addTiles('V', 2);
        addTiles('W', 2);
        addTiles('Y', 2);
        addTiles('*', 0);
        addTiles('J', 1);
        addTiles('K', 1);
        addTiles('Q', 3);
        addTiles('X', 3);
        addTiles('Z', 3);
        Collections.shuffle(pool);
    }

    // takes a specific letter out of the pool and puts it in the players hand
    private void addTileFromPool(Player player, char charToAdd){
        player.hand.add(charToAdd);
        for (int i = 0; i<pool.size(); i++){
            if (pool.get(i) == charToAdd){
                pool.remove(i);
                return;
            }
        }
    }

    // called when you click a board square, places whatever tile they had selected in their hand if that square is placeable on
    public void handleSquareClick(int x, int y) {
        if (selectedFromHand != -1 && board[x][y].placeable && boardIsInteractable){
            board[x][y].letter = players.get(currentPlayer).hand.get(selectedFromHand);
            board[x][y].isNew = true;
            players.get(currentPlayer).hand.remove(selectedFromHand);
            handIndexs.add(selectedFromHand);
            selectedFromHand = -1;
            tilesPlacedX.add(x);
            tilesPlacedY.add(y);
            currentScore = scoreTheBoard(board);
        }
    }

    // does the bot move. it gets the bot move, and then it plays it
    public void makeBotMove(){
        List<Square> moveToMake = getBotMove();
        
        if (moveToMake == null) { 
            handleGoButton(); 
            System.out.println("skipped"); 
            return; 
        }

        //make the move on the board, adding the tiles from the bot's hand onto the board
        Player current = players.get(currentPlayer);
        for (int i = 0; i < moveToMake.size(); i++){
            char letterPlaced = moveToMake.get(i).letter;
            board[moveToMake.get(i).tileX][moveToMake.get(i).tileY].letter = letterPlaced;
            board[moveToMake.get(i).tileX][moveToMake.get(i).tileY].isNew = true;
            current.hand.remove((Character) letterPlaced); 
        }
        //sets what tiles need to be placed for the scoring thing to access
        setPlacedTilesForBot(moveToMake);
        currentScore = scoreTheBoard(board);
        //ends the turn
        handleGoButton();
    }

    /* picks a move for the bot to play out of all the possible moves it found.
    harder bots pick from a smaller pool of the top scoring moves (so theyre
    more likely to play the best one), easier bots pick more randomly */
    public List<Square> getBotMove() {
        //gets all the moves that it can find into a list
        HashMap<List<Square>, Integer> allMoves = new HashMap<>();
        addAllMoves(board, players.get(currentPlayer).hand, allMoves, new ArrayList<>());
        tilesPlacedX.clear();
        tilesPlacedY.clear();


        //if there wasnt a single move it found return null instead of just crashing
        if (allMoves.size() == 0) { return null; }
        

        // sort moves worst to best score and handles tiles by whichever move uses more tiles
        List<List<Square>> sortedMoves = allMoves.entrySet().stream()
                                                            .sorted(Map.Entry.<List<Square>, Integer>comparingByValue()
                                                            .thenComparing(entry -> entry.getKey().size())
                                                            .reversed()) 
                                                            .map(Map.Entry::getKey)
                                                            .collect(Collectors.toList());

        int botDifficulty = players.get(currentPlayer).botDifficulty;

        //the way the difficulties work is depending on the bot difficulty, it picks a random move from the top n moves, worst bot is random move from the 100 top scoring moves, etc
        int poolSize;
        if (botDifficulty == 0) {
            poolSize = 100;
        } else if (botDifficulty == 1) {
            poolSize = 50;
        } else {
            poolSize = 1; 
        }

        //makes  sure it cannot pick a number greater than the number of moves in the board
        int actualPoolRange = Math.min(poolSize, sortedMoves.size());
        Random rand = new Random();
        int randomIndex = rand.nextInt(actualPoolRange);
        return sortedMoves.get(randomIndex);
    }

    // helper to set the tiles placed lists to match the current board, needed because scoreTheBoard reads from those lists
    public void setPlacedTilesForBot(List<Square> currentMoves){
        tilesPlacedX = new ArrayList<>();
        tilesPlacedY = new ArrayList<>();
        for (int i = 0; i<currentMoves.size(); i++){
            tilesPlacedX.add(currentMoves.get(i).tileX);
            tilesPlacedY.add(currentMoves.get(i).tileY);
        }
    }

    /* brute forces possible moves the bot could make this turn and stores
    each one in a hashmap with the score of that move. Checks all 4 directions a word could
    be placed from */
    public void addAllMoves(Square[][] startingBoard, List<Character> startingHand, HashMap<List<Square>, Integer> moveList, List<Square> currentMoves){
        setPlacedTilesForBot(currentMoves);
        updateBoardPlaceables(startingBoard, true);
        
        List<Square> placeableSquares = new ArrayList<>();
        for (int x = 0; x < 15; x++){
            for (int y = 0; y < 15; y++){
                if (startingBoard[x][y].placeable){
                    placeableSquares.add(startingBoard[x][y]);
                }
            }
        }
        /* left-right words: for each placeable square that isnt already followed by a
         letter, work out whats already on the board before it, then ask the dictionary
         for every word that could be built from that prefix using the bots hand */
        for (int i = 0; i < placeableSquares.size(); i++){
            if (placeableSquares.get(i).tileX == 14) { continue; }
            if (startingBoard[placeableSquares.get(i).tileX+1][placeableSquares.get(i).tileY].letter != ' ') { continue; }
            String startOfWord = "";
            int currentX = placeableSquares.get(i).tileX-1;
            
            while (currentX >= 0 && startingBoard[currentX][placeableSquares.get(i).tileY].letter != ' ') {
                startOfWord = startingBoard[currentX][placeableSquares.get(i).tileY].letter + startOfWord;
                currentX--;
            }
            currentX++; 
            
            List<String> listOfWords = dictionary.getAllPossibleWords(startOfWord, startingHand);
            // try placing each possible word on a test board and see if it actually works and scores
            for (String wordToTest : listOfWords){
                List<Square> movesPlayed = new ArrayList<>();
                Square[][] testingBoard = duplicateBoard(startingBoard);
                int amountOfStartOfWordLeft = startOfWord.length();
                int tempX = currentX;
                boolean validPlacement = true;
                
                for (int j = 0; j < wordToTest.length(); j++){
                    if (tempX > 14) { validPlacement = false; break; }
                    
                    if (amountOfStartOfWordLeft <= 0) {
                        if (startingBoard[tempX][placeableSquares.get(i).tileY].letter == ' ') {
                            testingBoard[tempX][placeableSquares.get(i).tileY].letter = wordToTest.toUpperCase().charAt(j);
                            testingBoard[tempX][placeableSquares.get(i).tileY].isNew = true;
                            movesPlayed.add(testingBoard[tempX][placeableSquares.get(i).tileY]);
                        } else {
                            if (startingBoard[tempX][placeableSquares.get(i).tileY].letter != wordToTest.toUpperCase().charAt(j)) {
                                validPlacement = false;
                                break;
                            }
                            testingBoard[tempX][placeableSquares.get(i).tileY].letter = wordToTest.toUpperCase().charAt(j);
                            testingBoard[tempX][placeableSquares.get(i).tileY].isNew = false;
                        }
                    } else {
                        testingBoard[tempX][placeableSquares.get(i).tileY].letter = wordToTest.toUpperCase().charAt(j);
                    }
                    amountOfStartOfWordLeft--;
                    tempX++;
                }
                
                if (validPlacement && !movesPlayed.isEmpty()) {
                    setPlacedTilesForBot(movesPlayed);
                    int score = scoreTheBoard(testingBoard);
                    if (score > 0) {
                        moveList.put(movesPlayed, score);
                    }
                }
            }
        }

        // vertical words, exact same logic as above just swapping x and y
        for (int i = 0; i < placeableSquares.size(); i++){
            if (placeableSquares.get(i).tileY == 14) { continue; }
            if (startingBoard[placeableSquares.get(i).tileX][placeableSquares.get(i).tileY+1].letter != ' ') { continue; }
            String startOfWord = "";
            int currentY = placeableSquares.get(i).tileY-1;
            
            while (currentY >= 0 && startingBoard[placeableSquares.get(i).tileX][currentY].letter != ' ') {
                startOfWord = startingBoard[placeableSquares.get(i).tileX][currentY].letter + startOfWord;
                currentY--;
            }
            currentY++; 
            
            List<String> listOfWords = dictionary.getAllPossibleWords(startOfWord, startingHand);
            for (String wordToTest : listOfWords){
                List<Square> movesPlayed = new ArrayList<>();
                Square[][] testingBoard = duplicateBoard(startingBoard);
                int amountOfStartOfWordLeft = startOfWord.length();
                int tempY = currentY;
                boolean validPlacement = true;
                
                for (int j = 0; j < wordToTest.length(); j++){
                    if (tempY > 14) { validPlacement = false; break; }
                    
                    if (amountOfStartOfWordLeft <= 0) {
                        if (startingBoard[placeableSquares.get(i).tileX][tempY].letter == ' ') {
                            testingBoard[placeableSquares.get(i).tileX][tempY].letter = wordToTest.toUpperCase().charAt(j);
                            testingBoard[placeableSquares.get(i).tileX][tempY].isNew = true;
                            movesPlayed.add(testingBoard[placeableSquares.get(i).tileX][tempY]);
                        } else {
                            if (startingBoard[placeableSquares.get(i).tileX][tempY].letter != wordToTest.toUpperCase().charAt(j)) {
                                validPlacement = false;
                                break;
                            }
                            testingBoard[placeableSquares.get(i).tileX][tempY].letter = wordToTest.toUpperCase().charAt(j);
                            testingBoard[placeableSquares.get(i).tileX][tempY].isNew = false;
                        }
                    } else {
                        testingBoard[placeableSquares.get(i).tileX][tempY].letter = wordToTest.toUpperCase().charAt(j);
                    }
                    amountOfStartOfWordLeft--;
                    tempY++;
                }
                
                if (validPlacement && !movesPlayed.isEmpty()) {
                    setPlacedTilesForBot(movesPlayed);
                    int score = scoreTheBoard(testingBoard);
                    if (score > 0) {
                        moveList.put(movesPlayed, score);
                    }
                }
            }
        }

        //right-left, this time building backwards off the end of an existing word, using whats at the end of a word rather than whats at the start
        for (int i = 0; i < placeableSquares.size(); i++){
            if (placeableSquares.get(i).tileX == 0) { continue; }
            if (startingBoard[placeableSquares.get(i).tileX-1][placeableSquares.get(i).tileY].letter != ' ') { continue; }
            String endOfWord = "";
            int currentX = placeableSquares.get(i).tileX+1;
            
            while (currentX < 15 && startingBoard[currentX][placeableSquares.get(i).tileY].letter != ' ') {
                endOfWord = endOfWord + startingBoard[currentX][placeableSquares.get(i).tileY].letter;
                currentX++;
            }
            int suffixEndIdx = currentX - 1; 
            
            List<String> listOfWords = dictionary.getAllPossibleWordsEnd(endOfWord, startingHand);
            for (String wordToTest : listOfWords){
                List<Square> movesPlayed = new ArrayList<>();
                Square[][] testingBoard = duplicateBoard(startingBoard);
                int startX = suffixEndIdx - wordToTest.length() + 1;
                int tempX = startX;
                boolean validPlacement = true;
                
                if (startX < 0) { continue; }
                
                for (int j = 0; j < wordToTest.length(); j++){
                    if (tempX > 14) { validPlacement = false; break; }
                    
                    if (tempX <= placeableSquares.get(i).tileX) {
                        if (startingBoard[tempX][placeableSquares.get(i).tileY].letter == ' ') {
                            testingBoard[tempX][placeableSquares.get(i).tileY].letter = wordToTest.toUpperCase().charAt(j);
                            testingBoard[tempX][placeableSquares.get(i).tileY].isNew = true;
                            movesPlayed.add(testingBoard[tempX][placeableSquares.get(i).tileY]);
                        } else {
                            if (startingBoard[tempX][placeableSquares.get(i).tileY].letter != wordToTest.toUpperCase().charAt(j)) {
                                validPlacement = false;
                                break;
                            }
                            testingBoard[tempX][placeableSquares.get(i).tileY].letter = wordToTest.toUpperCase().charAt(j);
                            testingBoard[tempX][placeableSquares.get(i).tileY].isNew = false;
                        }
                    } else {
                        testingBoard[tempX][placeableSquares.get(i).tileY].letter = wordToTest.toUpperCase().charAt(j);
                    }
                    tempX++;
                }
                
                if (validPlacement && !movesPlayed.isEmpty()) {
                    setPlacedTilesForBot(movesPlayed);
                    int score = scoreTheBoard(testingBoard);
                    if (score > 0) {
                        moveList.put(movesPlayed, score);
                    }
                }
            }
        }

        //backwards vertical version
        for (int i = 0; i < placeableSquares.size(); i++){
            if (placeableSquares.get(i).tileY == 0) { continue; }
            if (startingBoard[placeableSquares.get(i).tileX][placeableSquares.get(i).tileY-1].letter != ' ') { continue; }
            String endOfWord = "";
            int currentY = placeableSquares.get(i).tileY+1;
            
            while (currentY < 15 && startingBoard[placeableSquares.get(i).tileX][currentY].letter != ' ') {
                endOfWord = endOfWord + startingBoard[placeableSquares.get(i).tileX][currentY].letter;
                currentY++;
            }
            int suffixEndIdx = currentY - 1; 
            
            List<String> listOfWords = dictionary.getAllPossibleWordsEnd(endOfWord, startingHand);
            for (String wordToTest : listOfWords){
                List<Square> movesPlayed = new ArrayList<>();
                Square[][] testingBoard = duplicateBoard(startingBoard);
                int startY = suffixEndIdx - wordToTest.length() + 1;
                int tempY = startY;
                boolean validPlacement = true;
                
                if (startY < 0) { continue; }
                
                for (int j = 0; j < wordToTest.length(); j++){
                    if (tempY > 14) { validPlacement = false; break; }
                    
                    if (tempY <= placeableSquares.get(i).tileY) {
                        if (startingBoard[placeableSquares.get(i).tileX][tempY].letter == ' ') {
                            testingBoard[placeableSquares.get(i).tileX][tempY].letter = wordToTest.toUpperCase().charAt(j);
                            testingBoard[placeableSquares.get(i).tileX][tempY].isNew = true;
                            movesPlayed.add(testingBoard[placeableSquares.get(i).tileX][tempY]);
                        } else {
                            if (startingBoard[placeableSquares.get(i).tileX][tempY].letter != wordToTest.toUpperCase().charAt(j)) {
                                validPlacement = false;
                                break;
                            }
                            testingBoard[placeableSquares.get(i).tileX][tempY].letter = wordToTest.toUpperCase().charAt(j);
                            testingBoard[placeableSquares.get(i).tileX][tempY].isNew = false;
                        }
                    } else {
                        testingBoard[placeableSquares.get(i).tileX][tempY].letter = wordToTest.toUpperCase().charAt(j);
                    }
                    tempY++;
                }
                
                if (validPlacement && !movesPlayed.isEmpty()) {
                    setPlacedTilesForBot(movesPlayed);
                    int score = scoreTheBoard(testingBoard);
                    if (score > 0) {
                        moveList.put(movesPlayed, score);
                    }
                }
            }
        }
    }

    // just adds "count" copies of a letter to the pool
    private void addTiles(char letter, int count) {
        for (int i = 0; i < count; i++) {
            pool.add(letter);
        }
    }

    /* works out which squares are placeable on depending on
     how many tiles have already been placed this turn since after
     the first tile you can only continue in a straight line */
    private void updateBoardPlaceables(Square[][] boardToUpdate, boolean isTestPlaceable){
        for (int x = 0; x<15; x++){
            for (int y = 0; y<15; y++){
                if (selectedFromHand == -1 && !isTestPlaceable){
                    boardToUpdate[x][y].placeable = false;
                }
                else if (tilesPlacedX.size() == 0){
                    // first tile of the turn: must be the centre square, or touching an existing tile
                    boardToUpdate[x][y].placeable = ((x==7 && y==7) || numberOfNeighbours(x, y)>0) && board[x][y].letter == ' ';
                }
                else if (tilesPlacedX.size() == 1){
                    // second tile has to be somewhere along the same row or column with no gaps or anything
                     
                    boolean hasFoundAdjacent = false;
                    int lastTileXPos = tilesPlacedX.get(0);
                    for (int tileXPos = tilesPlacedX.get(0); boardToUpdate[lastTileXPos][tilesPlacedY.get(0)].letter != ' '; tileXPos--){
                        if (x==tileXPos && y==tilesPlacedY.get(0)){
                            hasFoundAdjacent = true;
                        }
                        lastTileXPos = tileXPos;
                        if (lastTileXPos<0||lastTileXPos>14||tilesPlacedY.get(0)<0||tilesPlacedY.get(0)>14) {break;}
                    }
                    lastTileXPos = tilesPlacedX.get(0);
                    for (int tileXPos = tilesPlacedX.get(0); boardToUpdate[lastTileXPos][tilesPlacedY.get(0)].letter != ' '; tileXPos++){
                        if (x==tileXPos && y==tilesPlacedY.get(0)){
                            hasFoundAdjacent = true;
                        }
                        lastTileXPos = tileXPos;
                        if (lastTileXPos<0||lastTileXPos>14||tilesPlacedY.get(0)<0||tilesPlacedY.get(0)>14) {break;}
                    }
                    int lastTileYPos = tilesPlacedY.get(0);
                    for (int tileYPos = tilesPlacedY.get(0); boardToUpdate[tilesPlacedX.get(0)][lastTileYPos].letter != ' '; tileYPos--){
                        if (y==tileYPos && x==tilesPlacedX.get(0)){
                            hasFoundAdjacent = true;
                        }
                        lastTileYPos = tileYPos;
                        if (lastTileYPos<0||lastTileYPos>14||tilesPlacedX.get(0)<0||tilesPlacedX.get(0)>14) {break;}
                    }
                    lastTileYPos = tilesPlacedY.get(0);
                    for (int tileYPos = tilesPlacedY.get(0); boardToUpdate[tilesPlacedX.get(0)][lastTileYPos].letter != ' '; tileYPos++){
                        if (y==tileYPos && x==tilesPlacedX.get(0)){
                            hasFoundAdjacent = true;
                        }
                        lastTileYPos = tileYPos;
                        if (lastTileYPos<0||lastTileYPos>14||tilesPlacedX.get(0)<0||tilesPlacedX.get(0)>14) {break;}
                    }
                    boardToUpdate[x][y].placeable = hasFoundAdjacent && boardToUpdate[x][y].letter == ' ';
                }
                else {
                    // if 2 or more tiles are placed already only can place in the same row/column
                    
                    if (tilesPlacedX.get(0) == tilesPlacedX.get(1)) {
                        boolean hasFoundAdjacent = false;
                        
                        int lastTileYPos = tilesPlacedY.get(0);
                        for (int tileYPos = tilesPlacedY.get(0); 
                            lastTileYPos >= 0 && lastTileYPos < 15 && boardToUpdate[tilesPlacedX.get(0)][lastTileYPos].letter != ' '; 
                            tileYPos--) {
                            
                            if (y == tileYPos && x == tilesPlacedX.get(0)) {
                                hasFoundAdjacent = true;
                            }
                            lastTileYPos = tileYPos;
                        }

                        lastTileYPos = tilesPlacedY.get(0);
                        for (int tileYPos = tilesPlacedY.get(0); 
                            lastTileYPos >= 0 && lastTileYPos < 15 && boardToUpdate[tilesPlacedX.get(0)][lastTileYPos].letter != ' '; 
                            tileYPos++) {
                            
                            if (y == tileYPos && x == tilesPlacedX.get(0)) {
                                hasFoundAdjacent = true;
                            }
                            lastTileYPos = tileYPos;
                        }

                        boardToUpdate[x][y].placeable = (tilesPlacedX.get(0) == x && hasFoundAdjacent && boardToUpdate[x][y].letter == ' ');
                        
                    } else {
                        boolean hasFoundAdjacent = false;
                        
                        int lastTileXPos = tilesPlacedX.get(0);
                        for (int tileXPos = tilesPlacedX.get(0); 
                            lastTileXPos >= 0 && lastTileXPos < 15 && boardToUpdate[lastTileXPos][tilesPlacedY.get(0)].letter != ' '; 
                            tileXPos--) {
                            
                            if (x == tileXPos && y == tilesPlacedY.get(0)) {
                                hasFoundAdjacent = true;
                            }
                            lastTileXPos = tileXPos;
                        }

                        lastTileXPos = tilesPlacedX.get(0);
                        for (int tileXPos = tilesPlacedX.get(0); 
                            lastTileXPos >= 0 && lastTileXPos < 15 && boardToUpdate[lastTileXPos][tilesPlacedY.get(0)].letter != ' '; 
                            tileXPos++) {
                            
                            if (x == tileXPos && y == tilesPlacedY.get(0)) {
                                hasFoundAdjacent = true;
                            }
                            lastTileXPos = tileXPos;
                        }

                        boardToUpdate[x][y].placeable = (tilesPlacedY.get(0) == y && hasFoundAdjacent && boardToUpdate[x][y].letter == ' ');
                    }
                }
            }
        }
    }

    // counts how many of the 4 neighbouring squares already have a letter on them
    private int numberOfNeighbours(int x, int y){
        int count = 0;
        if (x>0){
            if (board[x-1][y].letter != ' '){
                count++;
            }
        }
        if (x<14){
            if (board[x+1][y].letter != ' '){
                count++;
            }
        }
        if (y>0){
            if (board[x][y-1].letter != ' '){
                count++;
            }
        }
        if (y<14){
            if (board[x][y+1].letter != ' '){
                count++;
            }
        }
        return count;
    }

    // main game loop, keeps looping forever until the game ends. plays the bots move and refreshes the rendered game at roughly 60 fps
    private void runGame(){
        while(true){
            // end the game if skipped turns is too high 
            if (skippedTurnsCount >= 8){
                for (int j = 0; j<players.size(); j++){
                    for (int k = 0; k<players.get(j).hand.size(); k++){
                        players.get(j).currentScore -= getLetterValue(players.get(j).hand.get(k));
                    }
                }
                endTheGame();
                break;
            }
            // if the pool runs out and someone empties their hand, thats also game over
            if (pool.size() == 0){
                for (int i = 0; i < players.size(); i++){
                    if (players.get(i).hand.size() == 0){
                        for (int j = 0; j<players.size(); j++){
                            if (i==j) { continue; }
                            for (int k = 0; k<players.get(j).hand.size(); k++){
                                players.get(i).currentScore += getLetterValue(players.get(j).hand.get(k));
                                players.get(j).currentScore -= getLetterValue(players.get(j).hand.get(k));
                            }
                        }
                        endTheGame();
                        break;
                    }
                }
            }
            updateBoardPlaceables(board, false);
            boardIsInteractable = !players.get(currentPlayer).isBot;
            gameRenderer.renderGame(board, players.get(currentPlayer).hand, selectedFromHand, currentScore);
            if (players.get(currentPlayer).isBot){
                System.out.println("Player has " + players.get(currentPlayer).hand.size());
                makeBotMove();
            }
            try {
                Thread.sleep(17);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); 
                break; 
            }
        }
    }

    // called when the human clicks a tile in their hand, toggles it selected/unselected
    public void selectFromHand(int handIndex){
        if (!boardIsInteractable) { return; }
        if (selectedFromHand==handIndex){
            selectedFromHand = -1;
        }
        else{
            selectedFromHand = handIndex;
        }
    }

}
