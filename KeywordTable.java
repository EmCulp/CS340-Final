import java.util.HashMap;
import java.util.Map;

public class KeywordTable {

    private final Map<String, Integer> keywordMap;

    public KeywordTable() {
        keywordMap = new HashMap<>();
        initializeTable();
    }

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

    public Integer getTokenID(String keyword) {
        return keywordMap.get(keyword);
    }

    public void addKeyword(String keyword, int tokenID) {
        keywordMap.put(keyword, tokenID);
    }

    public boolean contains(String keyword) {
        return keywordMap.containsKey(keyword);
    }

    public Integer get(String keyword){
        return keywordMap.get(keyword);
    }
}
