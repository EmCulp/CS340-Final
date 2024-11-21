/*******************************************************************
 * Tokenization Class *
 * *
 * PROGRAMMER: Emily Culp*
 * COURSE: CS340 - Programming Language Design*
 * DATE: 11/12/2024 *
 * REQUIREMENT: Tokenization for the interpreter *
 * *
 * DESCRIPTION: *
 * This class is responsible for tokenizing input commands into an array of tokens. *
 * It breaks down the command string based on predefined delimiters and returns *
 * an array of strings representing individual tokens. This functionality is essential *
 * for processing commands in the interpreter. *
 * *
 * COPYRIGHT: This code is copyright (C) 2024 Emily Culp and Dean Zeller. *
 * *
 * CREDITS: This code was written with the help of ChatGPT. *
 * *
 *******************************************************************/
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenization {

    private static final String TOKEN_REGEX = "\"[^\"]*\"|\\d+\\.\\d+|\\d+|\\w+|==|!=|<=|>=|\\+\\+|--|[+\\-*/=(){}^<>.,?!:\"'\\[\\]]|;";

    /**********************************************************
     * METHOD: tokenize(String command) *
     * DESCRIPTION: Tokenizes a given command string into an array of tokens. *
     * PARAMETERS: String command - the command string to tokenize *
     * RETURN VALUE: String[] - an array of tokens extracted from the command *
     **********************************************************/

    public static String[] tokenize(String command) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = Pattern.compile(TOKEN_REGEX).matcher(command);

        while (matcher.find()) {
            String token = matcher.group().trim();

            // Skip empty spaces and handle comments (if needed)
            if (!token.isEmpty() && !token.equals(" ")) {
                tokens.add(token);

                // Enhanced debugging output for tokens
                System.out.println("Matched token: " + token + " (type: " + getTokenType(token) + ")");
            }
        }

        System.out.println("Tokens: " + Arrays.toString(tokens.toArray()));
        return tokens.toArray(new String[0]);
    }

    /**********************************************************
     * METHOD: getTokenType(String token)                       *
     * DESCRIPTION: Determines the type of a given token.       *
     *              It checks if the token is a literal,        *
     *              operator, keyword, or identifier. The method *
     *              uses regular expressions to classify the    *
     *              token into one of these categories.         *
     * PARAMETERS: String token - The token whose type is to be determined. *
     * RETURN VALUE: String - A string representing the type of *
     *              the token. Possible return values include:  *
     *              "Literal", "Operator", "Keyword", "Identifier".  *
     * EXCEPTIONS: None                                           *
     **********************************************************/

    // Helper method to identify token types (keywords, literals, operators, etc.)
    private static String getTokenType(String token) {
        if (token.matches("\\d+\\.\\d+")) return "Literal (Double)";
        if (token.matches("\\d+")) return "Literal (Integer)";
        if (token.equals("true") || token.equals("false")) return "BooleanLiteral";
        if (token.matches("[+\\-*/=(){}^<>.,?!:\"'\\[\\]]")) return "Operator";
        if (token.matches("integer|input|print|boolean|double|string")) return "Keyword";
        return "Identifier";
    }

    public static String[] tokenizeFile(String filePath) throws IOException{
        List<String> tokenizedLines = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(filePath))){
            String line;
            while((line = reader.readLine()) != null){
                if(!line.trim().isEmpty() && !line.startsWith("#")){
                    String[] tokens = tokenize(line);
                    tokenizedLines.addAll(Arrays.asList(tokens));
                }
            }
        }

        return tokenizedLines.toArray(new String[0]);
    }
}
