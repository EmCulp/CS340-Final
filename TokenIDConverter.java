/*******************************************************************
 * Tokenization Class *
 * *
 * PROGRAMMER: Emily Culp*
 * COURSE: CS340 - Programming Language Design*
 * DATE: 12/10/2024 *
 * REQUIREMENT: Tokenization for the interpreter *
 * *
 * DESCRIPTION: *
 * The TokenIDConverter class is used to convert token IDs from various token
 * tables (SymbolTable, LiteralTable, OperatorTable, KeywordTable) into their
 * binary string representations. The class allows you to print the token IDS
 * in binary format to a PrintWriter. It supports multiple tables for different
 * types of tokens, such as symbols, literals, operators, and keywords. The token
 * IDs are printed with their corresponding binary values.*
 * *
 * COPYRIGHT: This code is copyright (C) 2024 Emily Culp and Dean Zeller. *
 * *
 * CREDITS: This code was written with the help of ChatGPT. *
 * *
 *******************************************************************/

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

    /**********************************************************
     * METHOD: convertToBinary(int tokenID) *
     * DESCRIPTION: This method converts a given token ID into its binary string representation *
     * PARAMETERS: int tokenID - the token ID to be converted to binary *
     * RETURN VALUE: String - returns a String representing the binary value of the tokenID*
     **********************************************************/
    public static String convertToBinary(int tokenID){
        return Integer.toBinaryString(tokenID);
    }

    /**********************************************************
     * METHOD: printTokenIDsInBinary(Object table, PrintWriter writer) *
     * DESCRIPTION: This method takes a token table (either SymbolTable, LiteralTable,
     * OperatorTable, or KeywordTable) and prints the token IDs of that table in
     * binary format to a given PrintWriter*
     * PARAMETERS: Object table - The token table (instance of SymbolTable, LiteralTable,
     *          OperatorTable, or KeywordTable) whose token IDs are to be printed in
     *          binary format.
     *          PrintWriter writer - The PrintWriter to which the binary representations
     *          of the token IDs are written*
     * RETURN VALUE: This method does not return a value. It writes the binary
     *          representations directly to the PrintWriter*
     **********************************************************/
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

    /**********************************************************
     * METHOD: writeTokenIDInBinary(int tokenID, PrintWriter writer)*
     * DESCRIPTION: This method converts a given token ID to binary and writes it to
     *              the provided PrintWriter*
     * PARAMETERS: int tokenID - the token ID to be converted to binary
     *             PrinterWriter writer - the PrintWriter to which the binary
     *             representation is written*
     * RETURN VALUE: This method does not return a value. It writes the binary representation
     *              directly to the PrintWriter*
     **********************************************************/
    private void writeTokenIDInBinary(int tokenID, PrintWriter writer) {
        String binaryRepresentation = Integer.toBinaryString(tokenID);  // Convert token ID to binary
        writer.println("Token ID: " + tokenID + " => Binary: " + binaryRepresentation);
    }

}
