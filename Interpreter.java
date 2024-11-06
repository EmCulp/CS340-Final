import java.util.*;

public class Interpreter {
    private static final String KEYWORD_INTEGER = "integer";
    private static final String KEYWORD_INPUT = "input";
    private static final String KEYWORD_PRINT = "print";

    // Token IDs for the keywords and commands
    private static final Map<String, Integer> TOKEN_IDS = new HashMap<>() {{
        put(KEYWORD_INTEGER, 100);
        put(KEYWORD_INPUT, 101);
        put(KEYWORD_PRINT, 102);
        put("=", 200);
        put(";", 203);
        put("(", 201);
        put(")", 202);
        put("+", 204);
        put("-", 205);
        put("*", 206);
        put("/", 207);
        put("^", 208);
        // Add more tokens as needed
        put("if", 103);
        put("else", 104);
        put("{", 209);
        put("}", 210);
        put("==", 211);
        put("!=", 212);
        put("<", 213);
        put(">", 214);
        put("<=", 215);
        put(">=", 216);
    }};


    private static SymbolTable symbolTable;
    private static LiteralTable literalTable;
    private static Evaluator evaluator;
    private static Tokenization tokenizer;
    private static Interpreter interpreter;

    static{
        symbolTable =  new SymbolTable();
        literalTable = new LiteralTable();
        evaluator = new Evaluator(symbolTable);
        tokenizer = new Tokenization();
    }

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Interpreter. Type '#' to quit.");

        while (true) {
            System.out.print(">>> ");
            String command = scanner.nextLine();

            // Exit condition
            if (command.equalsIgnoreCase("#")) {
                break;
            }

            // Tokenize the command
            String[] tokens = tokenizer.tokenize(command);
            System.out.println("Tokens (main): " + String.join(" ", tokens));

            // Execute the command based on tokens
            interpreter.executeCommand(tokens);
        }

        scanner.close();
        symbolTable.printTable(); // Print the symbol table at the end
        literalTable.printTable();
    }
    public static void executeCommand(String[] tokens) throws Exception {
        //handle the new 'if-else' structure
        if(tokens[0].equals("if")){
            handleIfElse(tokens);
            return;
        }

        // Check if the last token is a semicolon
        if (tokens.length >= 3 && tokens[tokens.length - 1].equals(";")) {
            // Handle variable declaration or assignment
            if (tokens[0].equals(KEYWORD_INTEGER)) {
                // Variable declaration without assignment (e.g., "integer a;")
                if (tokens.length == 3) {
                    handleVariableDeclaration(tokens);
                }
                // Variable declaration with assignment (e.g., "integer a = 15;")
                else if (tokens.length == 5 && tokens[2].equals("=")) {
//                    String input = String.join(" ", tokens);
                    interpreter.handleAssignment(tokens);
                } else {
                    System.out.println("Syntax error: Invalid variable declaration.");
                }
            }
            // Handle assignments (e.g., "sum = a + b + c;")
            else if (tokens.length >= 3 && tokens[1].equals("=")) {
//                String input = String.join(" ", tokens);
                interpreter.handleAssignment(tokens);
            }
            else if (tokens[0].equals(KEYWORD_INPUT)) {
                handleInput(tokens);
            } else if (tokens[0].equals(KEYWORD_PRINT)) {
                handlePrint(tokens);
            } else {
                System.out.println("Syntax error: Unrecognized command");
            }
        } else {
            System.out.println("Syntax error: Command must end with a semicolon");
        }
    }


    //only works with something like "integer x;"
    private static void handleVariableDeclaration(String[] tokens) {
        String variableName = tokens[1]; // The variable name (e.g., x)

        if (symbolTable.containsVariable(variableName)) {
            System.out.println("Syntax error: Variable '" + variableName + "' already declared.");
            return;
        }

        // If the declaration is just "integer x;"
        if (tokens.length == 3) {
            symbolTable.addVariable(variableName, 0); // Default value 0 for uninitialized variables
            System.out.println("Variable declaration: " + variableName + " with ID: " + symbolTable.getId(variableName));

            int variableID = symbolTable.getId(variableName);

            int literalID = literalTable.addLiteral(0);
            System.out.println("Added literal: 0 with ID " + literalID + " to the Literal Table.");

            System.out.print("TokenIDs: ");
            System.out.print(TOKEN_IDS.get("integer")+ " " + variableID + " " + TOKEN_IDS.get(";")+ " ");
            System.out.println();

            System.out.println("Code Generators: " + CodeGenerator.END_DEFINE);
        } else {
            System.out.println("Syntax error: Invalid variable declaration.");
        }
    }
    private static void handleInput(String[] tokens) {
        System.out.println("Debug: Tokens received -> " + String.join(" ", tokens));

        // Check for correct token count
        if (tokens.length != 5 || !tokens[0].equals("input") ||
                !tokens[1].equals("(") || !tokens[3].equals(")") || !tokens[4].equals(";")) {
            System.out.println("Syntax error: Invalid input statement.");
            return;
        }

        String variableName = tokens[2]; // Extract the variable name
        Integer variableID = symbolTable.getId(variableName); // Fetch variable ID

        if (variableID == null) {
            System.out.println("Error: Variable " + variableName + " is undeclared.");
            return;
        }

        // Prompt for user input
        Scanner scanner = new Scanner(System.in);
        System.out.print("=> ");
        int value = scanner.nextInt();

        // Assign value to the variable
        symbolTable.updateValue(variableName, value);
        System.out.println("Assigned value " + value + " to variable " + variableName);

        // Print TokenIDs
        System.out.print("TokenIDs: ");
        System.out.println(TOKEN_IDS.get("input") + " " + TOKEN_IDS.get("(") +
                " " + variableID + " " + TOKEN_IDS.get(")") + " " + TOKEN_IDS.get(";"));
        System.out.println(CodeGenerator.START_DEFINE + " " +  CodeGenerator.END_DEFINE + " " + CodeGenerator.NO_OP);
    }

    private static void handlePrint(String[] tokens) {
        System.out.println("Tokens: " + String.join(" ", tokens) + " ;");

        // Basic syntax check: print ( a , b , c ) ;
        if (!tokens[0].equals("print") || !tokens[1].equals("(") || !tokens[tokens.length - 2].equals(")") ||
                !tokens[tokens.length - 1].equals(";")) {
            System.out.println("Syntax error: Invalid print statement.");
            return;
        }

        // Extract the elements inside the parentheses (ignoring "print", "(", ")", and ";")
        List<String> elements = new ArrayList<>();
        for (int i = 2; i < tokens.length - 2; i += 2) {  // Step by 2 to skip commas
            elements.add(tokens[i]);
            if (i + 1 < tokens.length - 2 && !tokens[i + 1].equals(",")) {
                System.out.println("Syntax error: Expected ',' between elements.");
                return;
            }
        }

        // Collect TokenIDs for all elements in the print statement
        System.out.print("TokenIDs: ");
        System.out.print(TOKEN_IDS.get("print") + " "); // Print keyword token

        // Process each element inside the parentheses
        for (String element : elements) {
            Integer tokenId = symbolTable.getId(element); // Check if it's a variable

            if (tokenId != null) {
                System.out.print(tokenId + " "); // Print variable token ID
            } else {
                try {
                    // If not a variable, treat it as a literal
                    int literalValue = Integer.parseInt(element);
                    int literalID = literalTable.addLiteral(literalValue);
                    System.out.print(literalID + " "); // Print literal token ID
                } catch (NumberFormatException e) {
                    System.out.println("Error: '" + element + "' is not a valid variable or literal.");
                    return;
                }
            }
        }

        System.out.println(TOKEN_IDS.get(")") + " " + TOKEN_IDS.get(";"));

        // Generate code for printing each element
        for (String element : elements) {
            Integer tokenId = symbolTable.getId(element);
            if (tokenId != null) {
                System.out.println(CodeGenerator.LOAD + " " + tokenId);
            } else {
                int literalValue = Integer.parseInt(element);
                int literalID = literalTable.getLiteralID(literalValue);
                System.out.println(CodeGenerator.LOAD + " " + literalID);
            }
            System.out.println(CodeGenerator.NO_OP); // Add NO_OP to signify print operation
        }
        System.out.println(CodeGenerator.STORE + " output"); // Simulate storing the print output
    }

    // Handle assignment logic
    //works with "integer a = 15;"
    // Assume `evaluator` is capable of handling expressions properly with parentheses
    public static void handleAssignment(String[] tokens) {
        // Check if the first token indicates a declaration or an assignment
        if (tokens[0].equals("integer")) {
            String variableName = tokens[1]; // The variable on the left-hand side
            String valueToken = tokens[3]; // The value to assign

            // Declare the variable with a default value (0)
            if (!symbolTable.containsVariable(variableName)) {
                symbolTable.addVariable(variableName, 0);
                System.out.println("Encountered new symbol " + variableName + " with id " + symbolTable.getId(variableName));
            }

            // Parse and assign the initial value
            try {
                int value = Integer.parseInt(valueToken); // Convert the token to an integer
                symbolTable.updateValue(variableName, value); // Update the variable's value

                // Add to the literal table if necessary
                int literalID = literalTable.addLiteral(value);
                System.out.println("Encountered new literal " + value + " with id " + literalID);
                // Print TokenIDs
                System.out.print("TokenIDs: " + TOKEN_IDS.get("integer") + " " + symbolTable.getId(variableName) + " " + TOKEN_IDS.get("=") + " " + literalID + " " + TOKEN_IDS.get(";") + " ");
                System.out.println();
                System.out.println("Code Generators: " + CodeGenerator.START_DEFINE + " " + CodeGenerator.END_DEFINE);

            } catch (NumberFormatException e) {
                System.out.println("Syntax error: Invalid assignment value.");
            }

        } else {
            // Handle assignment for already declared variables (e.g., sum = a + b + c)
            String variableName = tokens[0]; // The variable on the left-hand side
            String valueExpression = String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length - 1)); // Get the right-hand side expression

            try {
                // Ensure the variable is declared
                if (!symbolTable.containsVariable(variableName)) {
                    symbolTable.addVariable(variableName, 0); // Declare it if not
                    System.out.println("Encountered new symbol " + variableName + " with id " + symbolTable.getId(variableName));
                }

                // Evaluate the expression to get the value to assign
                // Here, we assume you have a method in your Evaluator class that can evaluate the expression and generate code
                int value = evaluator.evaluate(valueExpression); // Use the Evaluator to calculate the value

                // Check if the value is in the literal table, if not, add it
                int valueID = literalTable.addLiteral(value);
                System.out.println("Encountered new literal " + value + " with id " + valueID);

                // Update the variable's value in the symbol table
                symbolTable.updateValue(variableName, value);

                System.out.println("Assigned value: " + value + " to variable '" + variableName + "' with ID " + symbolTable.getId(variableName));

                // Print TokenIDs (example, adjust according to your actual logic)
                System.out.print("TokenIDs: " + symbolTable.getId(variableName) + " " + TOKEN_IDS.get("=") + " " + valueID + " " + TOKEN_IDS.get(";") + " ");
                System.out.println();

                // Add code generators based on the operations performed
                List<CodeGenerator> codeGenerators = new ArrayList<>();

                // Simulate the math operation to demonstrate code generation
                String[] expressionTokens = valueExpression.split(" ");
                for (String token : expressionTokens) {
                    if (token.equals("+")) {
                        codeGenerators.add(CodeGenerator.ADD);
                    } else if (token.equals("-")) {
                        codeGenerators.add(CodeGenerator.SUB);
                    } else if (token.equals("*")) {
                        codeGenerators.add(CodeGenerator.MULT);
                    } else if (token.equals("/")) {
                        codeGenerators.add(CodeGenerator.DIV);
                    } else if (token.equals("(")) {
                        codeGenerators.add(CodeGenerator.START_PAREN);
                    } else if (token.equals(")")) {
                        codeGenerators.add(CodeGenerator.END_PAREN);
                    } else if (symbolTable.containsVariable(token)) {
                        codeGenerators.add(CodeGenerator.LOAD); // Load variable
                    } else {
                        try {
                            Integer.parseInt(token); // If it's a literal
                            codeGenerators.add(CodeGenerator.LOAD); // Load literal
                        } catch (NumberFormatException e) {
                            // Handle as needed
                        }
                    }
                }
                codeGenerators.add(CodeGenerator.STORE); // Finally store the computed value
                System.out.println("Code Generators: " + codeGenerators);

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    public static void handleIfElse(String[] tokens) throws Exception {
        // Step 1: Find the condition expression within parentheses
        int openParen = Arrays.asList(tokens).indexOf("(");
        int closeParen = Arrays.asList(tokens).indexOf(")");

        if (openParen == -1 || closeParen == -1 || closeParen <= openParen) {
            System.out.println("Syntax error: Missing or misformatted condition in if statement.");
            return;
        }

        // Extract condition as a sub-array of tokens and evaluate
        String[] conditionTokens = Arrays.copyOfRange(tokens, openParen + 1, closeParen);
        boolean conditionResult = evaluator.evaluateCondition(Arrays.asList(conditionTokens));

        // Step 2: Identify the true block (within '{' and '}')
        List<String> tokenList = Arrays.asList(tokens);

        // Find the trueBlockStart after closeParen
        int trueBlockStart = -1;
        for (int i = closeParen; i < tokenList.size(); i++) {
            if (tokenList.get(i).equals("{")) {
                trueBlockStart = i;
                break;
            }
        }

        // Find the trueBlockEnd after trueBlockStart
        int trueBlockEnd = -1;
        for (int i = trueBlockStart; i < tokenList.size(); i++) {
            if (tokenList.get(i).equals("}")) {
                trueBlockEnd = i;
                break;
            }
        }
        if (trueBlockStart == -1 || trueBlockEnd == -1 || trueBlockEnd <= trueBlockStart) {
            System.out.println("Syntax error: Missing or misformatted true block in if statement.");
            return;
        }

        // Extract true block as a sub-array of tokens
        String[] trueBlockTokens = Arrays.copyOfRange(tokens, trueBlockStart + 1, trueBlockEnd);

        // Step 3: Check if there is an 'else' block and extract its tokens if present
        boolean hasElse = (tokens.length > trueBlockEnd + 1) && tokens[trueBlockEnd + 1].equals("else");
        String[] falseBlockTokens = new String[0];

        if (hasElse) {
            List<String> token = Arrays.asList(tokens);
            int falseBlockStart = -1;
            for (int i = closeParen; i < token.size(); i++) {
                if (token.get(i).equals("{")) {
                    falseBlockStart = i;
                    break;
                }
            }
            int falseBlockEnd = -1;
            for (int i = falseBlockStart; i < token.size(); i++) {
                if (token.get(i).equals("}")) {
                    falseBlockEnd = i;
                    break;
                }
            }

            if (falseBlockStart == -1 || falseBlockEnd == -1 || falseBlockEnd <= falseBlockStart) {
                System.out.println("Syntax error: Missing or misformatted false block in else statement.");
                return;
            }

            falseBlockTokens = Arrays.copyOfRange(tokens, falseBlockStart + 1, falseBlockEnd);
        }

        // Step 4: Execute the appropriate block based on the condition result
        if (conditionResult) {
            executeBlock(trueBlockTokens);
        } else if (hasElse) {
            executeBlock(falseBlockTokens);
        }

        // **Tokens and Token IDs Display Logic**
        // Print Tokens
        System.out.print("Tokens: ");
        for (String token : tokens) {
            Integer tokenID = null;

            // Check if the token is a symbol (variable)
            if (SymbolTable.containsVariable(token)) {
                tokenID = SymbolTable.getId(token);  // Retrieve token ID from SymbolTable
            }
            // Check if the token is a literal
            else if (LiteralTable.containsLiteral(token)) {
                tokenID = LiteralTable.getLiteralID(token);  // Retrieve token ID from LiteralTable
            }

            // If no token ID found, print "N/A"
            System.out.print((tokenID != null ? tokenID : "N/A") + " ");
        }
        System.out.println();
    }


    private static int findClosingBrace(String[] tokens, int openingBraceIndex) {
        int braceCount = 1; // Start counting with the first opening brace

        for (int i = openingBraceIndex + 1; i < tokens.length; i++) {
            if (tokens[i].equals("{")) {
                braceCount++; // Increment for each opening brace
            } else if (tokens[i].equals("}")) {
                braceCount--; // Decrement for each closing brace
                if(braceCount == 0){
                    return i;
                }
            }

            if (braceCount == 0) { // When we reach zero, we found the matching closing brace
                return i; // Return the index of the closing brace
            }
        }

        return -1; // Return -1 if no matching closing brace is found
    }


    // Original method that takes two parameters
    private static int findIndexOf(String[] tokens, String target) {
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals(target)) {
                return i; // Return the index if found
            }
        }
        return -1; // Return -1 if not found
    }

    // Overloaded method that takes three parameters
    private static int findIndexOf(String[] tokens, String target, int startIndex) {
        for (int i = startIndex; i < tokens.length; i++) {
            if (tokens[i].equals(target)) {
                return i; // Return the index if found
            }
        }
        return -1; // Return -1 if not found
    }

    //Helper method to execute a block of statements
    private static void executeBlock(String[] blockTokens) throws Exception {
        // Assume that each statement in the block ends with a semicolon
        StringBuilder commandBuilder = new StringBuilder();

        for (String token : blockTokens) {
            commandBuilder.append(token).append(" ");
            if (token.equals(";")) {
                // Execute each command within the block
                String command = commandBuilder.toString().trim();
                String[] statementTokens = tokenizer.tokenize(command);
                executeCommand(statementTokens);
                commandBuilder.setLength(0); // Reset for the next command
            }
        }
    }
}
