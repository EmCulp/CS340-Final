import java.util.HashMap;
import java.util.Map;

public class KeywordTable {
    private Map<String, Integer> keywords;

    public KeywordTable() {
        keywords = new HashMap<>();
        initializeKeywords();
    }

    private void initializeKeywords() {
        // Add keywords with their corresponding IDs
        keywords.put("integer", 1);
        keywords.put("input", 2);
        keywords.put("print", 3);
        // Add more keywords as necessary
    }

    public Integer getKeywordID(String keyword) {
        return keywords.get(keyword);
    }

    public boolean isKeyword(String token) {
        return keywords.containsKey(token);
    }

    public void displayKeywords() {
        System.out.println("Keywords in the table:");
        for (Map.Entry<String, Integer> entry : keywords.entrySet()) {
            System.out.println("Keyword: " + entry.getKey() + ", ID: " + entry.getValue());
        }
    }
}
