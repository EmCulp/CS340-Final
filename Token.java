/*******************************************************************
 * Token Class *
 * *
 * PROGRAMMER: Emily Culp*
 * COURSE: CS340 *
 * DATE: 12/10/2024 *
 * REQUIREMENT: Final - Compiler *
 * *
 * DESCRIPTION: *
 * The Token class represents a token used in the interpreter. *
 * Each token has an associated token ID and a name. This class provides *
 * methods to retrieve the token's ID and name. Tokens are fundamental in *
 * parsing and interpreting the programming language's syntax and operators. *
 * *
 * COPYRIGHT: This code is copyright (C) 2024 Emily Culp*
 * and Professor Zeller. *
 * *
 * CREDITS: This code was written with the help of ChatGPT. *
 * *
 *******************************************************************/

public class Token {
    private int tokenID;
    private String name;

    /**********************************************************
     * METHOD: Token(int tokenID, String name) *
     * DESCRIPTION: Constructor that initializes a Token object with the given token ID and name. *
     * PARAMETERS: int tokenID - the ID associated with the token. *
     *             String name - the name of the token. *
     * RETURN VALUE: none *
     **********************************************************/
    public Token(int tokenID, String name) {
        this.tokenID = tokenID;
        this.name = name;
    }

    /**********************************************************
     * METHOD: getTokenID() *
     * DESCRIPTION: Retrieves the token ID of the token. *
     * PARAMETERS: none *
     * RETURN VALUE: int - the token ID associated with the token. *
     **********************************************************/
    public int getTokenID() {
        return tokenID;
    }

    /**********************************************************
     * METHOD: getName() *
     * DESCRIPTION: Retrieves the name of the token. *
     * PARAMETERS: none *
     * RETURN VALUE: String - the name of the token. *
     **********************************************************/
    public String getName() {
        return name;
    }
}
