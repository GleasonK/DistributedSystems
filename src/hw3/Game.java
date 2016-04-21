package hw3;

import java.io.Serializable;

/**
 * Created by GleasonK on 2/26/16.
 */
public class Game implements Serializable{
    private String word;
    private String displayWord;
    private boolean[] guessedLetters;
    private int guesses;
    private String message;
    private boolean gameOver;

    public Game(){
        this.guessedLetters = new boolean[26];
        this.guesses=0;
        this.gameOver = false;
    }

    public void guessLetter(char c){
        if (gameOver) return;
        c = Character.toLowerCase(c);
        if (word==null){
            this.message = "Wait for Player 1 to set the word.";
            return;
        } else if ( c < 'a' || c > 'z' ) { // Ignore non-letters
            this.message = "Guess a letter in a-z";
            return;
        } else if (isGuessed(c)) {
            this.message = String.format("Letter %c has been guessed!",c);
            return;
        }
        guesses++;
        int idx = index(c);
        guessedLetters[idx] = true;
        evaluateGuess(c);
        if (!displayWord.contains("-")){
            this.gameOver=true;
            this.message = "Game is over, Player 2 Wins!";
        } else if (guessesLeft() <= 0){
            this.gameOver = true;
            this.message = "Game is over, Player 1 Wins!";
        }
    }

    public void setWord(String word){
        this.word=word;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            sb.append("-");
        }
        this.displayWord = sb.toString();
        this.message = this.displayWord.length() + " letters";
    }

    public boolean isGameOver(){
        return this.gameOver;
    }

    public String getMessage(){
        return this.gameOver ? this.message : this.toString();
    }

    private boolean isGuessed(char c){
        c = Character.toLowerCase(c);
        if ( c < 'a' || c > 'z' ) return false;
        return this.guessedLetters[index(c)];
    }

    private void evaluateGuess(char c){
        int correct = 0;
        int idx = this.word.indexOf(c);
        while (idx != -1){
            correct++;
            this.displayWord = displayWord.substring(0,idx) + c + displayWord.substring(idx+1);
            int next = this.word.substring(idx+1).indexOf(c);
            idx = next==-1 ? -1 : idx + next + 1;
        }
        this.message= String.format("Guessed %c. Found %d times in word.", c, correct);
    }

    private int guessesLeft(){
        int remain = word.length()+5 - guesses;
        return (remain < 0) ? 0 : remain;
    }

    private int index(char c){
        c = Character.toLowerCase(c);
        return c - 'a';
    }

    @Override
    public String toString() {
        return this.displayWord + " - " + this.message + " - " + guessesLeft() + " remaining guesses.";
    }

//    public static void main(String[] args) {
//        Game gi = new Game();
//        gi.setWord("hi");
//        gi.guessLetter('a');
//        gi.guessLetter('b');
//        gi.guessLetter('c');
//        gi.guessLetter('d');
//        gi.guessLetter('e');
//        gi.guessLetter('f');
//        gi.guessLetter('g');
//        System.out.println(gi.toString());
//
//        gi.guessLetter('Z');
//        System.out.println(gi.toString());
//    }
}
