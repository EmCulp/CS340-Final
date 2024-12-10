/*******************************************************************
 * Operator Table *
 * *
 * PROGRAMMER: Emily Culp*
 * COURSE: CS340 *
 * DATE: 12/10/2024 *
 * REQUIREMENT: Final - Compiler *
 * *
 * DESCRIPTION: *
 * The following program defines an OperatorTable class that manages *
 * operators and their corresponding token IDs for a programming language interpreter. *
 * It allows adding operators, retrieving operator IDs, and storing associated tokens. *
 * The operators are mapped to their respective IDs for easy retrieval during parsing. *
 * The class also supports adding and retrieving tokens related to operators, and checking *
 * if a specific operator is present in the table. *
 * *
 * COPYRIGHT: This code is copyright (C) 2024 Emily Culp*
 * and Professor Zeller. *
 * *
 * CREDITS: This code was written with the help of ChatGPT. *
 * *
 *******************************************************************/
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OperatorTable {

    private final Map<String, Integer> operatorMap;
    private Map<String, Token> tokens;

    public OperatorTable() {
        operatorMap = new HashMap<>();
        initializeTable();
        tokens = new HashMap<>();
    }

    /**********************************************************
     * METHOD: initializeTable() *
     * DESCRIPTION: Initializes the operator table with predefined operators and their token IDs. *
     * PARAMETERS: none *
     * RETURN VALUE: none *
     **********************************************************/
    private void initializeTable() {
        operatorMap.put("=", 200);  // Assignment operator
        operatorMap.put(";", 203);  // Semicolon
        operatorMap.put("(", 201);  // Open parenthesis
        operatorMap.put(")", 202);  // Close parenthesis
        operatorMap.put("+", 204);  // Addition operator
        operatorMap.put("-", 205);  // Subtraction operator
        operatorMap.put("*", 206);  // Multiplication operator
        operatorMap.put("/", 207);  // Division operator
        operatorMap.put("^", 208);  // Exponentiation operator
        operatorMap.put("==", 211); // Equality operator
        operatorMap.put("!=", 212); // Not equal operator
        operatorMap.put(">", 213);
        operatorMap.put("<", 214);
        operatorMap.put("<=", 215);
        operatorMap.put(">=", 216);
        operatorMap.put("{", 217);
        operatorMap.put("}", 218);
        operatorMap.put("\"", 219);
    }

    /**********************************************************
     * METHOD: getTokenID(String operator) *
     * DESCRIPTION: Retrieves the token ID corresponding to the provided operator. *
     * PARAMETERS: String operator - the operator whose token ID is to be retrieved. *
     * RETURN VALUE: Integer - the token ID of the operator, or null if not found. *
     **********************************************************/
    public Integer getTokenID(String operator) {
        return operatorMap.get(operator);
    }

    /**********************************************************
     * METHOD: addOperator(String operator, int tokenID) *
     * DESCRIPTION: Adds a new operator with the specified token ID to the operator table. *
     * PARAMETERS: String operator - the operator to be added. *
     *             int tokenID - the token ID associated with the operator. *
     * RETURN VALUE: none *
     **********************************************************/
    public void addOperator(String operator, int tokenID) {
        operatorMap.put(operator, tokenID);
    }

    /**********************************************************
     * METHOD: addToken(String key, Token token) *
     * DESCRIPTION: Adds a token to the tokens map with a specified key. *
     * PARAMETERS: String key - the key to associate with the token. *
     *             Token token - the token to be added. *
     * RETURN VALUE: none *
     **********************************************************/
    public void addToken(String key, Token token){
        tokens.put(key, token);
    }

    /**********************************************************
     * METHOD: getTokens() *
     * DESCRIPTION: Retrieves a collection of all tokens stored in the table. *
     * PARAMETERS: none *
     * RETURN VALUE: Collection<Token> - a collection of all tokens. *
     **********************************************************/
    public Collection<Token> getTokens(){
        return tokens.values();
    }

    /**********************************************************
     * METHOD: contains(String operator) *
     * DESCRIPTION: Checks if the operator table contains the specified operator. *
     * PARAMETERS: String operator - the operator to check for. *
     * RETURN VALUE: boolean - true if the operator exists in the table, false otherwise. *
     **********************************************************/
    public boolean contains(String operator) {
        return operatorMap.containsKey(operator);
    }

    /**********************************************************
     * METHOD: get(String keyword) *
     * DESCRIPTION: Retrieves the token ID for the specified keyword from the operator table. *
     * PARAMETERS: String keyword - the operator keyword whose token ID is to be retrieved. *
     * RETURN VALUE: Integer - the token ID associated with the keyword, or null if not found. *
     **********************************************************/
    public Integer get(String keyword){
        return operatorMap.get(keyword);
    }

    /**********************************************************
     * METHOD: getOperatorMap() *
     * DESCRIPTION: Retrieves the entire operator map. *
     * PARAMETERS: none *
     * RETURN VALUE: Map<String, Integer> - the map of operators and their token IDs. *
     **********************************************************/
    public Map<String, Integer> getOperatorMap(){
        return operatorMap;
    }
}
