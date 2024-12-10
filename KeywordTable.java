/*******************************************************************
 * Keyword Table Class *
 * *
 * PROGRAMMER: Emily *
 * COURSE: CS340 *
 * DATE: 12/10/2024 *
 * REQUIREMENT: Assignment 4 *
 * *
 * DESCRIPTION: *
 * The KeywordTable class is used to store and manage keywords in a programming language. *
 * Each keyword is mapped to a unique token ID for identification during parsing and interpretation. *
 * The class also provides methods to add new keywords, retrieve the token ID for a keyword, *
 * and store tokens associated with the keywords. The class uses a HashMap to store keywords and their token IDs. *
 * *
 * COPYRIGHT: This code is copyright (C) 2024 Emily *
 * and Professor X. *
 * *
 * CREDITS: This code was written with the help of ChatGPT. *
 * *
 *******************************************************************/

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class KeywordTable {

    private final Map<String, Integer> keywordMap;  // Maps keyword to token ID
    private Map<String, Token> tokens;  // Stores tokens associated with keywords

    /**********************************************************
     * METHOD: KeywordTable() *
     * DESCRIPTION: Constructor that initializes the KeywordTable and the keyword map. *
     *              It also calls the initializeTable() method to populate the initial set of keywords. *
     * PARAMETERS: none *
     * RETURN VALUE: none *
     **********************************************************/
    public KeywordTable() {
        keywordMap = new HashMap<>();
        initializeTable();  // Populates the table with predefined keywords
        tokens = new HashMap<>();
    }

    /**********************************************************
     * METHOD: initializeTable() *
     * DESCRIPTION: Initializes the keyword map with predefined keywords and their token IDs. *
     * PARAMETERS: none *
     * RETURN VALUE: none *
     **********************************************************/
    private void initializeTable() {
        keywordMap.put("integer", 100);    // Example keyword
        keywordMap.put("input", 101);
        keywordMap.put("print", 102);
        keywordMap.put("if", 103);
        keywordMap.put("else", 104);
        keywordMap.put("while", 105);
        keywordMap.put("boolean", 106);
        keywordMap.put("double", 107);
        keywordMap.put("string", 108);
        keywordMap.put("float", 109);
        keywordMap.put("for", 110);
    }

    /**********************************************************
     * METHOD: getTokenID(String keyword) *
     * DESCRIPTION: Retrieves the token ID associated with the given keyword. *
     * PARAMETERS: String keyword - the keyword to look up. *
     * RETURN VALUE: Integer - the token ID associated with the keyword, or null if not found. *
     **********************************************************/
    public Integer getTokenID(String keyword) {
        return keywordMap.get(keyword);
    }

    /**********************************************************
     * METHOD: addKeyword(String keyword, int tokenID) *
     * DESCRIPTION: Adds a new keyword with a corresponding token ID to the keyword map. *
     * PARAMETERS: String keyword - the keyword to be added. *
     *             int tokenID - the token ID associated with the keyword. *
     * RETURN VALUE: none *
     **********************************************************/
    public void addKeyword(String keyword, int tokenID) {
        keywordMap.put(keyword, tokenID);
    }

    /**********************************************************
     * METHOD: addToken(String key, Token token) *
     * DESCRIPTION: Adds a token to the token map, associating it with a keyword. *
     * PARAMETERS: String key - the key (keyword) associated with the token. *
     *             Token token - the Token object to be added. *
     * RETURN VALUE: none *
     **********************************************************/
    public void addToken(String key, Token token) {
        tokens.put(key, token);
    }

    /**********************************************************
     * METHOD: getTokens() *
     * DESCRIPTION: Retrieves all the tokens stored in the token map. *
     * PARAMETERS: none *
     * RETURN VALUE: Collection<Token> - a collection of all the tokens in the token map. *
     **********************************************************/
    public Collection<Token> getTokens() {
        return tokens.values();
    }

    /**********************************************************
     * METHOD: contains(String keyword) *
     * DESCRIPTION: Checks if the keyword map contains the given keyword. *
     * PARAMETERS: String keyword - the keyword to check for. *
     * RETURN VALUE: boolean - true if the keyword exists in the map, false otherwise. *
     **********************************************************/
    public boolean contains(String keyword) {
        return keywordMap.containsKey(keyword);
    }

    /**********************************************************
     * METHOD: get(String keyword) *
     * DESCRIPTION: Retrieves the token ID for a given keyword from the keyword map. *
     * PARAMETERS: String keyword - the keyword to look up. *
     * RETURN VALUE: Integer - the token ID associated with the keyword, or null if not found. *
     **********************************************************/
    public Integer get(String keyword) {
        return keywordMap.get(keyword);
    }

    /**********************************************************
     * METHOD: getKeywordMap() *
     * DESCRIPTION: Retrieves the entire keyword map. *
     * PARAMETERS: none *
     * RETURN VALUE: Map<String, Integer> - the map of keywords to their token IDs. *
     **********************************************************/
    public Map<String, Integer> getKeywordMap() {
        return keywordMap;
    }
}
