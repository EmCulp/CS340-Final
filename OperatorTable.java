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

    public Integer getTokenID(String operator) {
        return operatorMap.get(operator);
    }

    public void addOperator(String operator, int tokenID) {
        operatorMap.put(operator, tokenID);
    }

    public void addToken(String key, Token token){
        tokens.put(key, token);
    }

    public Collection<Token> getTokens(){
        return tokens.values();
    }

    public boolean contains(String operator) {
        return operatorMap.containsKey(operator);
    }

    public Integer get(String keyword){
        return operatorMap.get(keyword);
    }

    public Map<String, Integer> getOperatorMap(){
        return operatorMap;
    }
}
