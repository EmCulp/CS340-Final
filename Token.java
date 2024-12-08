public class Token {
    private int tokenID;
    private String name;

    // Constructor
    public Token(int tokenID, String name) {
        this.tokenID = tokenID;
        this.name = name;
    }

    // Getters
    public int getTokenID() {
        return tokenID;
    }

    public String getName() {
        return name;
    }
}
