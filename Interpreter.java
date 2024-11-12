import java.util.*;

public class Interpreter {
    private static final String KEYWORD_INTEGER = "integer";
    private static final String KEYWORD_INPUT = "input";
    private static final String KEYWORD_PRINT = "print";
    private static final String KEYWORD_IF = "if";
    private static final String KEYWORD_ELSE = "else";
    private static final String KEYWORD_WHILE = "while";

    // Token IDs for the keywords and commands
    public static final Map<String, Integer> TOKEN_IDS = new HashMap<>() {{
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
        put(KEYWORD_IF, 103);
        put(KEYWORD_ELSE, 104);
        put(KEYWORD_WHILE, 105);
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
        interpreter = new Interpreter();
        symbolTable =  new SymbolTable();
        literalTable = new LiteralTable();
        evaluator = new Evaluator(symbolTable, literalTable);
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
        // This method processes individual statements (e.g., assignments, print, etc.)
        List<String> tokenID = Arrays.asList(tokens);
        List<Integer> id = new ArrayList<>();

        for (String t : tokenID) {
            id.add(interpreter.getTokenID(t));
        }

        // Check for the 'if' or other statements
        if (tokens[0].equals("if")) {
            handleIfElse(tokenID, id);
            return;
        }

        // Handle variable declaration or assignment, input, print, etc.
        if (tokens.length >= 3 && tokens[tokens.length - 1].equals(";")) {
            if (tokens[0].equals(KEYWORD_INTEGER)) {
                if (tokens.length == 3) {
                    handleVariableDeclaration(tokens);  // Variable declaration
                } else if (tokens.length == 5 && tokens[2].equals("=")) {
                    interpreter.handleAssignment(tokens);  // Variable assignment
                } else {
                    System.out.println("Syntax error: Invalid variable declaration.");
                }
            } else if (tokens.length >= 3 && tokens[1].equals("=")) {
                interpreter.handleAssignment(tokens);  // Assignment
            } else if (tokens[0].equals(KEYWORD_INPUT)) {
                handleInput(tokens);  // Handle input
            } else if (tokens[0].equals(KEYWORD_PRINT)) {
                handlePrint(tokens);  // Handle print
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
        StringBuilder tokenIDs = new StringBuilder();
        StringBuilder values = new StringBuilder();

        // Process the print keyword and parentheses token IDs
        tokenIDs.append(TOKEN_IDS.get("print")).append(" ")
                .append(TOKEN_IDS.get("(")).append(" ");


        // Process each element inside the parentheses
        for (String element : elements) {
            Integer tokenId = symbolTable.getId(element); // Check if it's a variable

            if (tokenId != null) {
                // If it's a variable, get the variable's value and token ID
                Object variableValue = symbolTable.getValue(element); // Get the value of the symbol
                tokenIDs.append(tokenId).append(" "); // Append the token ID of the variable
                values.append(variableValue).append(" "); // Append the value of the variable
            } else {
                try {
                    // If it's not a variable, treat it as a literal (constant)
                    int literalValue = Integer.parseInt(element);
                    int literalID = literalTable.getLiteralID(literalValue);  // Get the token ID of the literal
                    tokenIDs.append(literalID).append(" "); // Append the literal token ID
                    values.append(literalValue).append(" ");  // Append the literal value
                } catch (NumberFormatException e) {
                    System.out.println("Error: '" + element + "' is not a valid variable or literal.");
                    return;
                }
            }
        }

        // Process the closing parenthesis and semicolon token IDs
        tokenIDs.append(TOKEN_IDS.get(")")).append(" ")
                .append(TOKEN_IDS.get(";"));

        // Print TokenIDs and Values in two separate lines
        System.out.println("TokenIDs: " + tokenIDs.toString().trim());
        System.out.println("Values: " + values.toString().trim());

        // Generate code for printing each element
        for (String element : elements) {
            Integer tokenId = symbolTable.getId(element);
            if (tokenId != null) {
                // Load the value of the variable and generate code to print it
                Object variableValue = symbolTable.getValue(element);
                System.out.println(CodeGenerator.LOAD + " " + tokenId); // Use token ID for the variable
            } else {
                try {
                    int literalValue = Integer.parseInt(element);
                    int literalID = literalTable.getLiteralID(literalValue);  // Get literal token ID
                    System.out.println(CodeGenerator.LOAD + " " + literalID); // Load literal ID
                } catch (NumberFormatException e) {
                    System.out.println("Error: Invalid literal '" + element + "'");
                    return;
                }
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

    public static void handleWhileLoop(String condition, List<String> blockTokens) throws Exception {
        // Loop until the condition evaluates to false
        while (evaluateCondition(condition)) {
            System.out.println("Condition evaluated to true, executing block...");

            // Process each statement within the block
            for (String statement : blockTokens) {
                statement = statement.trim();  // Trim any excess whitespace
                if (!statement.isEmpty()) {
                    System.out.println("Executing statement: " + statement + ";");
                    // Add the semicolon back to treat it as a full statement
                    String[] tokens = (statement + ";").trim().split("\\s+");  // Split with semicolon
                    executeCommand(tokens);  // Execute each statement inside the block
                }
            }

            // After executing the block, check the condition again
            System.out.println("Re-evaluating condition...");
            if (!evaluateCondition(condition)) {
                System.out.println("Condition evaluated to false, exiting loop.");
                break;  // Exit the loop if the condition is false
            }
        }
    }


    public static void handleIfElse(List<String> tokens, List<Integer> tokenIDs) throws Exception {
        // Find the index of the 'if' token
        int ifIndex = tokens.indexOf("if");

        // Validate the if-else structure
        validateIfElseStructure(tokens, tokenIDs);

        // Find indices for condition parentheses
        int startCondition = tokens.indexOf("(");
        int endCondition = tokens.indexOf(")");

        // Validate parentheses presence and order
        if (startCondition < 0 || endCondition < 0 || endCondition <= startCondition) {
            throw new Exception("Invalid if condition syntax: missing or misplaced parentheses");
        }

        // Extract condition tokens safely
        List<String> conditionTokens = tokens.subList(startCondition + 1, endCondition);
        List<Integer> conditionTokenIDs = tokenIDs.subList(startCondition + 1, endCondition);

        // Print debug info
        System.out.print("Condition TokenIDs: ");
        for (Integer tokenID : conditionTokenIDs) {
            System.out.print(tokenID + " ");
        }
        System.out.println();

        // Evaluate the condition
        Evaluator evaluator = new Evaluator(symbolTable, literalTable); // Assuming symbolTable is accessible
        boolean conditionResult = evaluator.evaluateCondition(conditionTokens, conditionTokenIDs);

        // Proceed with if-else logic
        int elseIndex = tokens.indexOf("else");

        // Find the opening and closing braces for the if block
        int openBraceIndex = tokens.indexOf("{");
        int closeBraceIndex = tokens.lastIndexOf("}");

        if (openBraceIndex < 0 || closeBraceIndex < 0 || closeBraceIndex <= openBraceIndex) {
            throw new Exception("Invalid block structure: missing or mismatched braces");
        }

        // Extract tokens for the if block
        List<String> ifTokens = tokens.subList(openBraceIndex + 1, closeBraceIndex);
        List<Integer> ifTokenIDs = tokenIDs.subList(openBraceIndex + 1, closeBraceIndex);

        // Handle the optional else block if present
        // Handle the optional else block if present
        List<String> elseTokens = new ArrayList<>();
        List<Integer> elseTokenIDs = new ArrayList<>();
        if (elseIndex != -1) {
            // Find the opening and closing braces specifically for the else block
            int elseOpenBraceIndex = -1;
            int elseCloseBraceIndex = -1;

            // Find the opening brace for the 'else' block
            for (int i = elseIndex; i < tokens.size(); i++) {
                if (tokens.get(i).equals("{")) {
                    elseOpenBraceIndex = i;
                    break;
                }
            }

            // Find the closing brace for the 'else' block, after the opening brace
            if (elseOpenBraceIndex != -1) {
                for (int i = elseOpenBraceIndex + 1; i < tokens.size(); i++) {
                    if (tokens.get(i).equals("}")) {
                        elseCloseBraceIndex = i;
                        break;
                    }
                }
            }

            // Extract tokens for the else block if the braces were found
            if (elseOpenBraceIndex != -1 && elseCloseBraceIndex != -1 && elseCloseBraceIndex > elseOpenBraceIndex) {
                elseTokens = tokens.subList(elseOpenBraceIndex + 1, elseCloseBraceIndex);
                elseTokenIDs = tokenIDs.subList(elseOpenBraceIndex + 1, elseCloseBraceIndex);
            }
        }


        // Execute the appropriate block based on the evaluated condition
        if (conditionResult) {
            List<String> currentCommandTokens = new ArrayList<>();
            String methodName = ""; // Variable to store the method name

            // Define the valid methods
            Set<String> methods = new HashSet<>(Arrays.asList("input", "print"));

            // Print the entire ifTokens to debug
            System.out.println("Tokens in 'if' block: " + ifTokens);

            // Check if the first token is a valid method name
            if (ifTokens.size() > 0) {
                String firstToken = ifTokens.get(0).trim();

                // Check if the first token is a valid method name from the 'methods' list
                if (methods.contains(firstToken)) {
                    methodName = firstToken;  // Capture the method name (e.g., "input" or "print")
                    System.out.println("Method detected: " + methodName);  // Debugging

                    // Add the method to the current command tokens
                    currentCommandTokens.add(methodName);

                    boolean parenthesisOpened = false;

                    // Iterate through the remaining tokens inside the 'if' block for arguments
                    for (int i = 1; i < ifTokens.size(); i++) {
                        String token = ifTokens.get(i).trim();  // Trim any extra spaces
                        System.out.println("Processing token: " + token);

                        // Only add the opening parenthesis once
                        if (token.equals("(") && !parenthesisOpened) {
                            currentCommandTokens.add(token);  // Add the opening parenthesis
                            parenthesisOpened = true;  // Flag to indicate parentheses are opened
                            System.out.println("Adding opening parenthesis: " + token);
                            continue;  // Skip the token here as it is already processed
                        }

                        // If parentheses are opened, add tokens to the list as part of the method's arguments
                        if (parenthesisOpened) {
                            if (!token.equals(")") && !token.equals(";")) {
                                currentCommandTokens.add(token);  // Add argument token
                                System.out.println("Adding argument token: " + token);
                            }
                        }

                        // Check for closing parenthesis, but only add it once
                        if (token.equals(")") && parenthesisOpened) {
                            currentCommandTokens.add(token);  // Add closing parenthesis
                            parenthesisOpened = false;  // Flag to close parentheses
                            System.out.println("Adding closing parenthesis: " + token);
                        }

                        // Check for semicolon indicating the end of the method call
                        if (token.equals(";")) {
                            // We add the semicolon only once, no extra semicolon
                            currentCommandTokens.add(token);  // Add semicolon to the list
                            System.out.println("Adding semicolon token: " + token);
                            break;  // End of the current command
                        }
                    }

                    // Now, execute the collected command (i.e., method and arguments)
                    String[] commandArray = currentCommandTokens.toArray(new String[0]);
                    executeCommand(commandArray);

                    // Reset flags for next method handling
                    currentCommandTokens.clear();
                } else {
                    // Check if the tokens represent a math operation (e.g., "x = x + 5;")
                    firstToken = ifTokens.get(0).trim();
                    if (firstToken.matches("[a-zA-Z_][a-zA-Z0-9_]*") && ifTokens.get(1).equals("=")) {
                        // It's an assignment: "x = x + 5"
                        String leftOperand = firstToken; // e.g., "x"
                        String operator = ifTokens.get(1); // "="
                        String rightOperand = String.join(" ", ifTokens.subList(2, ifTokens.size() - 1)); // e.g., "x + 5"
                        System.out.println("Processing math operation: " + leftOperand + " " + operator + " " + rightOperand);

                        // Evaluate the right-hand side expression (like "x + 5")
                        int rightValue = evaluator.evaluate(rightOperand); // Implement this method to evaluate the math

                        // Update the variable with the new value
                        symbolTable.updateValue(leftOperand, rightValue); // Implement this method to update the symbol table

                        System.out.println("Updated " + leftOperand + " to " + rightValue);
                    }
                }
            } else {
                System.out.println("No tokens to process.");
            }
        }else{
            List<String> currentCommandTokens = new ArrayList<>();
            String methodName = ""; // Variable to store the method name

            // Define the valid methods
            Set<String> methods = new HashSet<>(Arrays.asList("input", "print"));

            // Print the entire ifTokens to debug
            System.out.println("Tokens in 'else' block: " + elseTokens);

            // Check if the first token is a valid method name
            if (elseTokens.size() > 0) {
                String firstToken = elseTokens.get(0).trim();

                // Check if the first token is a valid method name from the 'methods' list
                if (methods.contains(firstToken)) {
                    methodName = firstToken;  // Capture the method name (e.g., "input" or "print")
                    System.out.println("Method detected: " + methodName);  // Debugging

                    // Add the method to the current command tokens
                    currentCommandTokens.add(methodName);

                    boolean parenthesisOpened = false;

                    // Iterate through the remaining tokens inside the 'if' block for arguments
                    for (int i = 1; i < elseTokens.size(); i++) {
                        String token = elseTokens.get(i).trim();  // Trim any extra spaces
                        System.out.println("Processing token: " + token);

                        // Only add the opening parenthesis once
                        if (token.equals("(") && !parenthesisOpened) {
                            currentCommandTokens.add(token);  // Add the opening parenthesis
                            parenthesisOpened = true;  // Flag to indicate parentheses are opened
                            System.out.println("Adding opening parenthesis: " + token);
                            continue;  // Skip the token here as it is already processed
                        }

                        // If parentheses are opened, add tokens to the list as part of the method's arguments
                        if (parenthesisOpened) {
                            if (!token.equals(")") && !token.equals(";")) {
                                currentCommandTokens.add(token);  // Add argument token
                                System.out.println("Adding argument token: " + token);
                            }
                        }

                        // Check for closing parenthesis, but only add it once
                        if (token.equals(")") && parenthesisOpened) {
                            currentCommandTokens.add(token);  // Add closing parenthesis
                            parenthesisOpened = false;  // Flag to close parentheses
                            System.out.println("Adding closing parenthesis: " + token);
                        }

                        // Check for semicolon indicating the end of the method call
                        if (token.equals(";")) {
                            // We add the semicolon only once, no extra semicolon
                            currentCommandTokens.add(token);  // Add semicolon to the list
                            System.out.println("Adding semicolon token: " + token);
                            break;  // End of the current command
                        }
                    }

                    // Now, execute the collected command (i.e., method and arguments)
                    String[] commandArray = currentCommandTokens.toArray(new String[0]);
                    executeCommand(commandArray);

                    // Reset flags for next method handling
                    currentCommandTokens.clear();
                } else {
                    // Check if the tokens represent a math operation (e.g., "x = x + 5;")
                    firstToken = elseTokens.get(0).trim();
                    if (firstToken.matches("[a-zA-Z_][a-zA-Z0-9_]*") && elseTokens.get(1).equals("=")) {
                        // It's an assignment: "x = x + 5"
                        String leftOperand = firstToken; // e.g., "x"
                        String operator = ifTokens.get(1); // "="
                        String rightOperand = String.join(" ", elseTokens.subList(2, elseTokens.size() - 1)); // e.g., "x + 5"
                        System.out.println("Processing math operation: " + leftOperand + " " + operator + " " + rightOperand);

                        // Evaluate the right-hand side expression (like "x + 5")
                        int rightValue = evaluator.evaluate(rightOperand); // Implement this method to evaluate the math

                        // Update the variable with the new value
                        symbolTable.updateValue(leftOperand, rightValue); // Implement this method to update the symbol table

                        System.out.println("Updated " + leftOperand + " to " + rightValue);
                    }
                }
            }
        }
    }

    private static int getTokenID(String token) {
        // Check if the token is a predefined keyword or operator (like "if", "else", "==", etc.)
        if (TOKEN_IDS.containsKey(token)) {
            // Print the token ID for keywords/operators
            System.out.println("Token: " + token + ", Token ID: " + TOKEN_IDS.get(token));
            return TOKEN_IDS.get(token);
        }
        // Check if the token is a variable (like 'a', 'b', etc.)
        else if (symbolTable.containsVariable(token)) {
            // Retrieve the token ID for the variable from the symbol table
            int tokenID = symbolTable.getId(token);
            // Print the token ID for the variable
            System.out.println("Token: " + token + ", Token ID (Variable): " + tokenID);

            // Retrieve the value of the variable for condition evaluation (use value in comparison)
            int variableValue = symbolTable.getValue(token);

            // Return the token ID to be used for condition evaluation (if needed)
            return tokenID;
        }
        // Check if the token is a numeric literal
        else if (isNumericLiteral(token)) {
            int literalValue = Integer.parseInt(token); // Convert numeric literals to int
            int literalID = literalTable.getLiteralID(literalValue); // Check if the literal already has an ID

            if (literalID == -1) {
                literalID = literalTable.addLiteral(literalValue); // Add literal if not already present
            }

            // Print the token ID for the literal
            System.out.println("Token: " + token + ", Token ID (Literal): " + literalID);
            return literalID;
        }

        // Return -1 if token is not found
        return -1;
    }

    // Helper method to check if a token is a numeric literal
    private static boolean isNumericLiteral(String token) {
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void validateIfElseStructure(List<String> tokens, List<Integer> tokenIDs) {
        int ifIndex = tokens.indexOf("if");
        int elseIndex = tokens.indexOf("else");

        // If there's an if but no else, it's still a valid structure
        if (ifIndex == -1) {
            throw new IllegalArgumentException("No if statement found.");
        }

        // If 'else' exists, ensure it comes after 'if'
        if (elseIndex != -1 && elseIndex < ifIndex) {
            throw new IllegalArgumentException("Invalid if-else structure.");
        }
    }

    public static List<String> processWhileBlock(String[] tokens) {
        // Assume block starts after the closing parenthesis of the while loop
        List<String> blockTokens = new ArrayList<>();
        boolean inBlock = false;

        // Iterate through tokens and capture everything after the 'while' loop condition
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals("{")) {
                inBlock = true;  // Start of the block
            } else if (tokens[i].equals("}")) {
                inBlock = false;  // End of the block
                break;  // Exit after the block ends
            }

            // Add tokens inside the block
            if (inBlock) {
                blockTokens.add(tokens[i]);
            }
        }

        return blockTokens;
    }

    public static int evaluateExpression(String expression) {
        // Implement logic to evaluate expressions like "a + 1", "5 * 3", etc.
        // This is a placeholder, you need to evaluate the expression properly based on your needs
        return Integer.parseInt(expression);  // Simplified evaluation for demonstration
    }

    public static boolean evaluateCondition(String condition) {
        // Example: "a < 5"
        String[] parts = condition.split("<");
        String variable = parts[0].trim();  // "a"
        int comparisonValue = Integer.parseInt(parts[1].trim());  // "5"

        // Retrieve the current value of 'a' from the symbol table
        Integer variableValue = symbolTable.getValue(variable);

        // Compare and return the result of the condition (e.g., a < 5)
        return variableValue != null && variableValue < comparisonValue;
    }
}
