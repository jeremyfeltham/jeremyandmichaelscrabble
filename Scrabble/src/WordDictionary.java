import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/* loads the words file and gives ways to check words and find
 possible words, used both to check if the player made a real word and to let
 the bot search for moves it could make */
public class WordDictionary {
    private HashSet<String> validWords; // hashset because w3 schools said so, also so searches are way faster

    public WordDictionary(String filePath) {
        validWords = new HashSet<>();
        loadWords(filePath);
    }

    // reads the word list file line by line and adds each word to the set
    private void loadWords(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                validWords.add(line.trim().toLowerCase());
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // checks if a word is valid. '*' means a blank tile so we have to try every letter in that spot and see if any combo makes a real word
    public boolean isValidWord(String word) {
        if (word == null) return false;
        int blankTileCount = (int) word.chars()
                                       .filter(ch -> ch == '*')
                                       .count();
        if (blankTileCount == 0){
            return validWords.contains(word.toLowerCase());
        }
        else if (blankTileCount == 1){
            for (int i = 0; i<26; i++){
                char charToTest = (char) ('a'+i);
                String replacedWord = replaceBlankInWord(word, 1, charToTest);
                if (validWords.contains(replacedWord.toLowerCase())){
                    return true;
                }
            }
        }
        else{
            //theres only 2 blank tiles in the game so we can just hardcode the count of nested for loops for this
            for (int i = 0; i<26; i++){
                for (int j = 0; j<26; j++){
                    char charToTest = (char) ('a'+i);
                    String replacedWord = replaceBlankInWord(word, 1, charToTest);
                    char charToTest2 = (char) ('a'+j);
                    String replacedWord2 = replaceBlankInWord(replacedWord, 2, charToTest2);
                    if (validWords.contains(replacedWord2.toLowerCase())){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /* finds every valid word that starts with "startOfWord" and can be finished
    using only the given letters, used when the bot is building off an existing tile */
    public List<String> getAllPossibleWords(String startOfWord, List<Character> letters) {
        List<String> wordList = new ArrayList<>();
        String prefix = startOfWord.toLowerCase();
        int prefixLength = prefix.length();
        // count how many of each letter we have, so the bot knows what is has to work with
        int[] availableCounts = new int[26];
        for (char c : letters) {
            int index = Character.toLowerCase(c) - 'a';
            if (index >= 0 && index < 26) {
                availableCounts[index]++;
            }
        }
        int maxPossibleLength = prefixLength + letters.size();
        int[] currentCounts = new int[26];
        for (String word : validWords) {
            // skip words that are too long already, saves checking every single word fully
            if (word.length() > maxPossibleLength || word.length() < prefixLength) {
                continue;
            }
            //if it starts with the specific starting letters, keep checking if its plausible
            if (word.startsWith(prefix)) {
                System.arraycopy(availableCounts, 0, currentCounts, 0, 26);
                boolean canMakeWord = true;
                // check if you actually have enough letters to spell the non prefix part of the board
                for (int i = prefixLength; i < word.length(); i++) {
                    int index = word.charAt(i) - 'a';
                    if (index < 0 || index >= 26 || currentCounts[index] <= 0) {
                        canMakeWord = false;
                        break; 
                    }
                    currentCounts[index]--;
                }
                if (canMakeWord) {
                    wordList.add(word);
                }
            }
        }
        return wordList;
    }

    // same idea as getAllPossibleWords but reversed for words that are built leftwards or upwards so it matches the end of the word instead of the start
    public List<String> getAllPossibleWordsEnd(String endOfWord, List<Character> letters) {
        List<String> wordList = new ArrayList<>();
        String suffix = endOfWord.toLowerCase();
        int suffixLength = suffix.length();
        
        int[] availableCounts = new int[26];
        for (char c : letters) {
            int index = Character.toLowerCase(c) - 'a';
            if (index >= 0 && index < 26) {
                availableCounts[index]++;
            }
        }
        
        int maxPossibleLength = suffixLength + letters.size();
        int[] currentCounts = new int[26];
        
        for (String word : validWords) {
            if (word.length() > maxPossibleLength || word.length() < suffixLength) {
                continue;
            }
            
            // use the end of the word 
            if (word.endsWith(suffix)) {
                System.arraycopy(availableCounts, 0, currentCounts, 0, 26);
                boolean canMakeWord = true;
                
                // count every letter, currentCounts is an array of how many of each letter has been used
                int remainingLength = word.length() - suffixLength;
                for (int i = 0; i < remainingLength; i++) {
                    int index = word.charAt(i) - 'a';
                    if (index < 0 || index >= 26 || currentCounts[index] <= 0) {
                        canMakeWord = false;
                        break; 
                    }
                    currentCounts[index]--;
                }
                
                if (canMakeWord) {
                    wordList.add(word);
                }
            }
        }
        return wordList;
    }

    // swaps a specific * in a string for an actual letter used to test out what a blank tile could be while checking word validity
    public static String replaceBlankInWord(String str, int indexToReplace, char replacement) {
        int index = -1;
        for (int i = 0; i < indexToReplace; i++) {
            index = str.indexOf('*', index + 1);
            if (index == -1) return str;
        }
        StringBuilder sb = new StringBuilder(str);
        sb.setCharAt(index, replacement);
        return sb.toString();
    }
}
