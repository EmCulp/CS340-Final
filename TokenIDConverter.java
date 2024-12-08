import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class TokenIDConverter {
    private final SymbolTable symbolTable;
    private final LiteralTable literalTable;
    private final OperatorTable operatorTable;
    private final KeywordTable keywordTable;

    public TokenIDConverter(SymbolTable symbolTable, LiteralTable literalTable, OperatorTable operatorTable, KeywordTable keywordTable){
        this.symbolTable = symbolTable;
        this.literalTable = literalTable;
        this.operatorTable = operatorTable;
        this.keywordTable = keywordTable;
    }

    public static String convertToBinary(int tokenID){
        return Integer.toBinaryString(tokenID);
    }

    public void printTokenIDsInBinary(Object table, PrintWriter writer) {
        if (table instanceof SymbolTable) {
            SymbolTable symbolTable = (SymbolTable) table;
            writer.println("Token IDs in Binary for Symbol Table:");
            for (Map.Entry<Integer, SymbolTable.Entry> entry : symbolTable.getAllEntries().entrySet()) {
                writeTokenIDInBinary(entry.getKey(), writer); // entry.getKey() is the token ID
            }
        } else if (table instanceof LiteralTable) {
            LiteralTable literalTable = (LiteralTable) table;
            writer.println("Token IDs in Binary for Literal Table:");
            for (Map.Entry<Integer, Object> entry : literalTable.getLiteralTable().entrySet()) {
                writeTokenIDInBinary(entry.getKey(), writer); // entry.getKey() is the token ID
            }
        } else if (table instanceof OperatorTable) {
            OperatorTable operatorTable = (OperatorTable) table;
            writer.println("Token IDs in Binary for Operator Table:");
            for (Map.Entry<String, Integer> entry : operatorTable.getOperatorMap().entrySet()) {
                writeTokenIDInBinary(entry.getValue(), writer); // entry.getValue() is the token ID
            }
        } else if (table instanceof KeywordTable) {
            KeywordTable keywordTable = (KeywordTable) table;
            writer.println("Token IDs in Binary for Keyword Table:");
            for (Map.Entry<String, Integer> entry : keywordTable.getKeywordMap().entrySet()) {
                writeTokenIDInBinary(entry.getValue(), writer); // entry.getValue() is the token ID
            }
        }
        writer.println();  // Add a blank line for readability
    }

    private void writeTokenIDInBinary(int tokenID, PrintWriter writer) {
        String binaryRepresentation = Integer.toBinaryString(tokenID);  // Convert token ID to binary
        writer.println("Token ID: " + tokenID + " => Binary: " + binaryRepresentation);
    }



    private void printSymbolTableInBinary(SymbolTable symbolTable) {
        System.out.println("Token IDs in Binary for Symbol Table:");
        Map<Integer, SymbolTable.Entry> entries = symbolTable.getAllEntries();  // Get all entries from SymbolTable
        for (Map.Entry<Integer, SymbolTable.Entry> entry : entries.entrySet()) {
            Integer tokenID = entry.getKey();  // Token ID is the key in the map
            SymbolTable.Entry entryValue = entry.getValue();  // Get the Entry object

            // Assuming Entry has a method to get the token name, you might need to adjust this
            String tokenName = entryValue.getName();

            String binaryRepresentation = convertToBinary(tokenID);
            System.out.println("Token Name: " + tokenName + " => Token ID: " + tokenID + " => Binary: " + binaryRepresentation);
        }
        System.out.println();
    }

    // Print Literal Table in Binary
    private void printLiteralTableInBinary(LiteralTable literalTable) {
        System.out.println("Token IDs in Binary for Literal Table:");
        Map<Integer, Object> literals = literalTable.getLiteralTable();  // Assuming this method exists
        for (Map.Entry<Integer, Object> entry : literals.entrySet()) {
            Integer tokenID = entry.getKey();
            Object value = entry.getValue();  // Value associated with the literal (can be an Object)

            String binaryRepresentation = convertToBinary(tokenID);
            System.out.println("Literal Value: " + value + " => Token ID: " + tokenID + " => Binary: " + binaryRepresentation);
        }
        System.out.println();
    }

    private void printOperatorTableInBinary(OperatorTable operatorTable) {
        System.out.println("Token IDs in Binary for Operator Table:");
        Map<String, Integer> operators = operatorTable.getOperatorMap();  // Assuming this method exists
        for (Map.Entry<String, Integer> entry : operators.entrySet()) {
            String name = entry.getKey();
            Integer tokenID = entry.getValue();
            String binaryRepresentation = convertToBinary(tokenID);
            System.out.println("Operator Name: " + name + " => Token ID: " + tokenID + " => Binary: " + binaryRepresentation);
        }
        System.out.println();
    }

    private void printKeywordTableInBinary(KeywordTable keywordTable) {
        System.out.println("Token IDs in Binary for Keyword Table:");
        Map<String, Integer> keywords = keywordTable.getKeywordMap();  // Assuming this method exists
        for (Map.Entry<String, Integer> entry : keywords.entrySet()) {
            String name = entry.getKey();
            Integer tokenID = entry.getValue();
            String binaryRepresentation = convertToBinary(tokenID);
            System.out.println("Keyword Name: " + name + " => Token ID: " + tokenID + " => Binary: " + binaryRepresentation);
        }
        System.out.println();
    }
}
