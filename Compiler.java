/*******************************************************************
 * Interpreter Class                                               *
 *                                                                 *
 * PROGRAMMER: Emily Culp                                          *
 * COURSE: CS340 - Programming Language Design                     *
 * DATE: 11/12/2024                                                *
 * REQUIREMENT: Implements an interpreter for basic language       *
 *              constructs such as variable declarations,         *
 *              assignments, input, print, if-else, and while.     *
 *                                                                 *
 * DESCRIPTION:                                                    *
 * The Interpreter class is responsible for interpreting and       *
 * executing commands based on a simple programming language. It   *
 * handles parsing, tokenization, execution of statements, and     *
 * interacting with symbol and literal tables. The interpreter    *
 * supports basic constructs like variable declaration, assignment,*
 * input, print, conditional statements (if-else), and loops. It  *
 * ensures that valid commands are executed and provides error    *
 * messages for invalid syntax.                                   *
 *                                                                 *
 * COPYRIGHT: This code is copyright (C) 2024 Emily Culp and Dean  *
 * Zeller.                                                         *
 *                                                                 *
 * CREDITS: This code was written with the help of ChatGPT.        *
 ******************************************************************/

import java.util.*;

public class Compiler {
    private static SymbolTable symbolTable;
    private static LiteralTable literalTable;
    private static KeywordTable keywordTable;
    private static OperatorTable operatorTable;
    private static Evaluator evaluator;
    private static Tokenization tokenizer;
    private static Compiler compiler;
    private static MIPSGenerator mipsGenerator = new MIPSGenerator();

    static{
        compiler = new Compiler();
        symbolTable =  new SymbolTable();
        literalTable = new LiteralTable();
        evaluator = new Evaluator(symbolTable, literalTable);
        keywordTable = new KeywordTable();
        operatorTable = new OperatorTable();
        tokenizer = new Tokenization();
    }

    /**********************************************************
     * METHOD: main(String[] args)                              *
     * DESCRIPTION: Main method to run the interpreter, allowing *
     *              users to input commands and execute them.   *
     *              The method tokenizes input commands and     *
     *              delegates execution to the appropriate      *
     *              method (e.g., handling assignments, print,  *
     *              input, etc.).                               *
     * PARAMETERS: String[] args - Command-line arguments (not *
     *              used in this implementation).               *
     * RETURN VALUE: None                                        *
     * EXCEPTIONS: Throws an Exception for invalid input or    *
     *             command errors.                               *
     **********************************************************/

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
            compiler.executeCommand(tokens);
        }

        scanner.close();
        symbolTable.display(); // Print the symbol table at the end
        literalTable.printTable();
    }

    /**********************************************************
     * METHOD: executeCommand(String[] tokens)                   *
     * DESCRIPTION: Executes individual statements by processing *
     *              the tokens and determining the appropriate  *
     *              action based on the statement type. Handles  *
     *              variable declaration, assignment, input,    *
     *              print, if-else, and while loop statements.  *
     * PARAMETERS: String[] tokens - An array of tokens that    *
     *              represent a command or statement to execute.*
     * RETURN VALUE: None                                         *
     * EXCEPTIONS: Throws an Exception for invalid commands or  *
     *             syntax errors in the tokens.                  *
     **********************************************************/

    public static void executeCommand(String[] tokens) throws Exception {
        // This method processes individual statements (e.g., assignments, print, etc.)
        List<String> tokenID = Arrays.asList(tokens);
        List<Integer> id = new ArrayList<>();

        if(tokens.length == 3 && tokens[0].equals("boolean")){
            String variableName = tokens[1].replace(";", "");
            System.out.println("Checking if variable exists: " +variableName+ " => " + symbolTable.containsVariable(variableName));

            if(!symbolTable.containsVariable(variableName)) {
                addBooleanLiteralIfNotExist("false");
                //adds variable with default value
                symbolTable.addOrUpdateBoolean(variableName, false);
                System.out.println("Not in symbol table... Now added to Symbol Table: " +variableName);
            }else{
                System.out.println("Error: Variable " +variableName+ " is already declared.");
            }
        }else if(tokens.length == 5 && tokens[0].equals("boolean") && tokens[2].equals("=")){
            String variableName = tokens[1];
            boolean value = Boolean.parseBoolean(tokens[3].replace(";", ""));
            System.out.println("Checking if variable exists: " +variableName+ " => " + symbolTable.containsVariable(variableName));

            if(!symbolTable.containsVariable(variableName)){
                addBooleanLiteralIfNotExist(value ? "true" : "false");
                symbolTable.addOrUpdateBoolean(variableName, value);
                System.out.println("Added to Symbol Table with value: " +variableName+ " = " +value);
            }else{
                System.out.println("Error: Variable " + variableName + " is already declared.");
            }
        }else{
//            System.out.println("Syntax error: Unrecognized command");

            for (String t : tokenID) {
                System.out.println("Processing token: " + t);
                Integer tokenIDValue = null;

                if (keywordTable.contains(t)) {
                    tokenIDValue = keywordTable.getTokenID(t);
                    System.out.println("Keyword: " + t);
                } else if (operatorTable.contains(t)) {
                    tokenIDValue = operatorTable.getTokenID(t);
                } else if (symbolTable.containsVariable(t)) {
                    tokenIDValue = symbolTable.getIdByName(t);
                    System.out.println("Found variable in symbol table: " + t);
                } else if (isNumericLiteral(t)) {
                    tokenIDValue = literalTable.getLiteralID(t);
                } else if (t.equals("true") || t.equals("false")) {
                    tokenIDValue = literalTable.getBooleanLiteralID(t);
                } else {
                    System.out.println("Unrecognized token... " + t);
                }

                if (tokenIDValue != null) {
                    id.add(tokenIDValue);
                } else {
                    System.out.println("Unrecognized token: " + t);
                }
            }
        }

        if(keywordTable.contains(tokens[0])){
            int tid = keywordTable.getTokenID(tokens[0]);

            if(tid == 103){
                handleIfElse(tokenID, id);
                return;
            }

            if(tid == 105){
                String condiiton = getConditionFromWhile(tokens);
                List<String> blockTokens = getBlockTokens(tokens);
                handleWhileLoop(condiiton, blockTokens);
                return;
            }
        }

        // Handle variable declaration or assignment, input, print, etc.
        if (tokens.length >= 3 && tokens[tokens.length - 1].equals(";")) {
            if (tokens[0].equals("integer")) {
                if (tokens.length == 3) {
                    handleVariableDeclaration(tokens);  // Variable declaration
                } else if (tokens.length == 5 && tokens[2].equals("=")) {
                    compiler.handleAssignment(tokens);  // Variable assignment
                } else {
                    System.out.println("Syntax error: Invalid variable declaration.");
                }
            } else if (tokens.length >= 3 && tokens[1].equals("=")) {
                compiler.handleAssignment(tokens);  // Assignment
            } else if (keywordTable.contains(tokens[0]) && keywordTable.getTokenID(tokens[0]) == 101) {
                handleInput(tokens);  // Handle input
            } else if (keywordTable.contains(tokens[0]) && keywordTable.getTokenID(tokens[0]) == 102) {
                handlePrint(tokens);  // Handle print
            } else {
                System.out.println("Syntax error: Unrecognized command");
            }
        } else {
            System.out.println("Syntax error: Command must end with a semicolon");
        }
    }

    /**********************************************************
     * METHOD: handleVariableDeclaration(String[] tokens)     *
     * DESCRIPTION: Handles variable declaration statements,   *
     *              such as "integer x;", by adding the         *
     *              variable to the symbol table with a default *
     *              value of 0. Also adds a corresponding literal*
     *              to the literal table with the value 0.      *
     *              Prints TokenIDs and code generation details.*
     * PARAMETERS: String[] tokens - An array of tokens representing*
     *              the variable declaration statement.        *
     * RETURN VALUE: None                                       *
     * EXCEPTIONS: Throws an Exception for invalid declarations.*
     **********************************************************/

    //only works with something like "integer x;"
    private static void handleVariableDeclaration(String[] tokens) {
        String variableName = tokens[1]; // The variable name (e.g., x)

        if(!keywordTable.contains("integer")){
//          boolean, double, float, String, blah blah blah
            System.out.println("Syntax error: Invalid keyword 'integer'.");
            return;
        }

        if(!operatorTable.contains(";")){
            System.out.println("Syntax error: Invalid operator ';'.");
            return;
        }

        if (symbolTable.containsVariable(variableName)) {
            System.out.println("Syntax error: Variable '" + variableName + "' already declared.");
            return;
        }

        // If the declaration is just "integer x;"
        if (tokens.length == 3 && tokens[2].equals(";")) {
            symbolTable.addEntry(variableName, "int", 0, "global"); // Default value 0 for uninitialized variables
            System.out.println("Variable declaration: " + variableName + " with ID: " + symbolTable.getIdByName(variableName));

            int variableID = symbolTable.getIdByName(variableName);

            int literalID = literalTable.addLiteral(0);
            System.out.println("Added literal: 0 with ID " + literalID + " to the Literal Table.");

            System.out.print("TokenIDs: ");
            System.out.print(keywordTable.get("integer")+ " " + variableID + " " + operatorTable.get(";")+ " ");
            System.out.println();

            System.out.println("Code Generators: " + CodeGenerator.END_DEFINE);
        } else {
            System.out.println("Syntax error: Invalid variable declaration.");
        }
    }

    private static void addBooleanLiteralIfNotExist(String value){
        if(!literalTable.containsBooleanLiteral(value)){
            int id = literalTable.getNextBooleanLiteralID();
            literalTable.addBooleanLiteral(value, id);
            System.out.println("Added to Boolean Literal Table: " + value + " with ID: " +id);
        }
    }

    /**********************************************************
     * METHOD: handleInput(String[] tokens)                    *
     * DESCRIPTION: Handles the input statement, such as       *
     *              "input(x);". It prompts the user for input,*
     *              assigns the value to the specified variable,*
     *              and prints TokenIDs and generated code.    *
     * PARAMETERS: String[] tokens - An array of tokens        *
     *              representing the input statement.          *
     * RETURN VALUE: None                                      *
     * EXCEPTIONS: Throws an Exception for invalid input       *
     *             statements or undeclared variables.         *
     **********************************************************/

    private static void handleInput(String[] tokens) {
        System.out.println("Debug: Tokens received -> " + String.join(" ", tokens));

        // Check for correct token count
        if (tokens.length != 5 || !tokens[0].equals("input") ||
                !tokens[1].equals("(") || !tokens[3].equals(")") || !tokens[4].equals(";")) {
            System.out.println("Syntax error: Invalid input statement.");
            return;
        }

        String variableName = tokens[2]; // Extract the variable name
        Integer variableID = symbolTable.getIdByName(variableName); // Fetch variable ID

        if (variableID == null) {
            System.out.println("Error: Variable " + variableName + " is undeclared.");
            return;
        }

        // Prompt for user input
        Scanner scanner = new Scanner(System.in);
        System.out.print("=> ");
        int value = scanner.nextInt();

        Integer literalID = literalTable.getLiteralID(value);
        if(literalID == null && literalID != -1){
            System.out.println("Literal value " + value + " already exists in the Literal table with the ID of " + literalID);
        }else{
            if(literalID == null || literalID == -1)  {
                literalID = literalTable.addLiteral(value);
                System.out.println("Added literal value " + value + " with ID " + literalID + " to the literal table.");
            }
        }

        // Assign value to the variable
        symbolTable.updateValue(variableName, value);
        System.out.println("Assigned value " + value + " to variable " + variableName);

        Integer inputID = keywordTable.get("input");
        Integer leftParenID = operatorTable.get("(");
        Integer rightParenID = operatorTable.get(")");
        Integer semicolonID = operatorTable.get(";");

        if (inputID == null || leftParenID == null || rightParenID == null || semicolonID == null) {
            System.out.println("Syntax error: Invalid tokens detected.");
            return;
        }

        // Print TokenIDs
        System.out.print("TokenIDs: ");
        System.out.println(inputID + " " + leftParenID + " " + variableID + " " + rightParenID + " " + semicolonID);
        System.out.println(CodeGenerator.START_DEFINE + " " +  CodeGenerator.END_DEFINE + " " + CodeGenerator.NO_OP);
    }

    /**********************************************************
     * METHOD: handlePrint(String[] tokens)                    *
     * DESCRIPTION: Handles the print statement, such as       *
     *              "print(x, y, 3);". It checks the validity  *
     *              of the syntax, processes each element      *
     *              inside the parentheses, and prints         *
     *              TokenIDs and corresponding values.         *
     * PARAMETERS: String[] tokens - An array of tokens        *
     *              representing the print statement.          *
     * RETURN VALUE: None                                      *
     * EXCEPTIONS: Throws an Exception for invalid print       *
     *             statements or undeclared variables.         *
     **********************************************************/

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

        Integer printTokenID = keywordTable.get("print");
        Integer leftParenTokenID = operatorTable.get("(");
        Integer rightParenTokenID = operatorTable.get(")");
        Integer semicolonTokenID = operatorTable.get(";");

        if (printTokenID == null || leftParenTokenID == null || rightParenTokenID == null || semicolonTokenID == null) {
            System.out.println("Syntax error: Invalid tokens detected.");
            return;
        }

        // Process the print keyword and parentheses token IDs
        tokenIDs.append(printTokenID).append(" ")
                .append(leftParenTokenID).append(" ");


        // Process each element inside the parentheses
        for (String element : elements) {
            Integer tokenId = symbolTable.getIdByName(element); // Check if it's a variable

            if (tokenId != null) {
                // If it's a variable, get the variable's value and token ID
                Object variableValue = symbolTable.getValueById(tokenId); // Get the value of the symbol
                tokenIDs.append(tokenId).append(" "); // Append the token ID of the variable
                values.append(variableValue).append(" "); // Append the value of the variable
            } else {
                try {
                    // If it's not a variable, treat it as a literal (constant)
                    int literalValue = Integer.parseInt(element);
                    int literalID = literalTable.getLiteralID(literalValue);  // Get the token ID of the literal
                    if(literalID == -1){
                        literalID = literalTable.addLiteral(literalValue);
                    }
                    tokenIDs.append(literalID).append(" "); // Append the literal token ID
                    values.append(literalValue).append(" ");  // Append the literal value

                } catch (NumberFormatException e) {
                    System.out.println("Error: '" + element + "' is not a valid variable or literal.");
                    return;
                }
            }
        }

        // Process the closing parenthesis and semicolon token IDs
        tokenIDs.append(rightParenTokenID).append(" ")
                .append(semicolonTokenID);

        // Print TokenIDs and Values in two separate lines
        System.out.println("TokenIDs: " + tokenIDs.toString().trim());
        System.out.println("Values: " + values.toString().trim());

        // Generate code for printing each element
        for (String element : elements) {
            Integer tokenId = symbolTable.getIdByName(element);
            if (tokenId != null) {
                // Load the value of the variable and generate code to print it
                Object variableValue = symbolTable.getValueById(tokenId);
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

    /**********************************************************
     * METHOD: handleAssignment(String[] tokens)
     * DESCRIPTION: Handles assignment commands like "x = 10;". *
     *              It checks if the variable is declared and   *
     *              assigns the value to the corresponding      *
     *              symbol table entry. Also handles invalid    *
     *              syntax or undeclared variables.             *
     * PARAMETERS: String[] tokens - An array of tokens         *
     *              representing the assignment command.        *
     * RETURN VALUE: None                                       *
     * EXCEPTIONS: Throws an Exception for invalid assignments  *
     *             or undeclared variables.                    *
     **********************************************************/
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
                symbolTable.addEntry(variableName, "int", 0, "global");
                System.out.println("Encountered new symbol " + variableName + " with id " + symbolTable.getIdByName(variableName));
            }

            // Parse and assign the initial value
            try {
                int value = Integer.parseInt(valueToken); // Convert the token to an integer
                symbolTable.updateValue(variableName, value); // Update the variable's value

                // Add to the literal table if necessary
                int literalID = literalTable.addLiteral(value);
                System.out.println("Encountered new literal " + value + " with id " + literalID);

                Integer integerTokenID = keywordTable.get("integer");
                Integer assignTokenID = operatorTable.get("=");
                Integer semicolonTokenID = operatorTable.get(";");

                // Print TokenIDs
                System.out.print("TokenIDs: " + integerTokenID + " " + symbolTable.getIdByName(variableName) + " " + assignTokenID + " " + literalID + " " + semicolonTokenID + " ");
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
                    symbolTable.addEntry(variableName, "int", 0, "global"); // Declare it if not
                    System.out.println("Encountered new symbol " + variableName + " with id " + symbolTable.getIdByName(variableName));
                }

                // Evaluate the expression to get the value to assign
                // Here, we assume you have a method in your Evaluator class that can evaluate the expression and generate code
                Object result = evaluator.evaluate(valueExpression); // Use the Evaluator to calculate the value
                int value = (Integer) result;

                // Check if the value is in the literal table, if not, add it
                int valueID = literalTable.addLiteral(value);
                System.out.println("Encountered new literal " + value + " with id " + valueID);

                // Update the variable's value in the symbol table
                symbolTable.updateValue(variableName, value);

                System.out.println("Assigned value: " + value + " to variable '" + variableName + "' with ID " + symbolTable.getIdByName(variableName));

                Integer assignTokenID = operatorTable.get("=");
                Integer semicolonTokenID = operatorTable.get(";");

                // Print TokenIDs (example, adjust according to your actual logic)
                System.out.print("TokenIDs: " + symbolTable.getIdByName(variableName) + " " + assignTokenID + " " + valueID + " " + semicolonTokenID + " ");
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

    /**********************************************************
     * METHOD: handleWhileLoop(String[] tokens)
     * DESCRIPTION: Handles while loop commands like "while (condition) { ... }".
     *              It evaluates the condition and, if true, executes the block of
     *              code inside the loop. The loop continues until the condition
     *              evaluates to false.
     * PARAMETERS: String[] tokens - An array of tokens representing the while loop.
     * RETURN VALUE: None
     * EXCEPTIONS: Throws an Exception if the while loop syntax is invalid or
     *             if there is an error evaluating the condition.
     **********************************************************/

    public static void handleWhileLoop(String condition, List<String> blockTokens) throws Exception {
        // Create an instance of Tokenization (if it's not already available)
        Tokenization tokenizer = new Tokenization();

        // Tokenize the condition using your Tokenization class, which returns a String[]
        String[] conditionTokensArray = tokenizer.tokenize(condition); // Assuming tokenize returns a String[]

        // Convert the String[] to a List<String>
        List<String> conditionTokens = new ArrayList<>(Arrays.asList(conditionTokensArray));

        // Create a list of token IDs for the condition
        List<Integer> conditionTokenIDs = new ArrayList<>();

        // Get the token ID for each token in the condition
        for (String token : conditionTokens) {
            int tokenID = getTokenID(token);  // Get the token ID for the token
            conditionTokenIDs.add(tokenID);   // Add the token ID to the list
        }

        // Loop until the condition evaluates to false
        while (evaluator.evaluateCondition(conditionTokens, conditionTokenIDs)) {
            System.out.println("Condition evaluated to true, executing block...");

            // Process the block of code (now as complete statements)
            StringBuilder statementBuilder = new StringBuilder();  // StringBuilder to concatenate tokens

            for (String token : blockTokens) {
                token = token.trim();  // Trim excess whitespace

                if (!token.isEmpty()) {
                    statementBuilder.append(token).append(" ");  // Concatenate tokens with space

                    // If the token is a semicolon, execute the full statement
                    if (token.equals(";")) {
                        String fullStatement = statementBuilder.toString().trim();  // Build the full statement
                        System.out.println("Executing statement: " + fullStatement + ";");

                        // Ensure the statement ends with a semicolon
                        if (!fullStatement.endsWith(";")) {
                            fullStatement += ";";
                        }

                        // Pass the complete statement to executeCommand
                        executeCommand(new String[]{fullStatement});

                        // Reset the StringBuilder for the next statement
                        statementBuilder.setLength(0);

                        if (fullStatement.contains("=")) {
                            String[] parts = fullStatement.split("=");  // Split by '='
                            String variableName = parts[0].trim();
                            String expression = parts[1].trim().replace(";", "");  // Extract the expression

                            // Evaluate the right-hand side expression
                            int newValue = (Integer) evaluator.evaluateExpression(expression);  // Evaluate expression like "a + 1"

                            // Update the symbol table with the new value
                            symbolTable.updateValue(variableName, newValue);
                        }
                    }
                }
            }

            // After executing the block, check the condition again
            System.out.println("Re-evaluating condition...");

            // Re-tokenize and re-evaluate the condition
            conditionTokensArray = tokenizer.tokenize(condition); // Re-tokenize the condition
            conditionTokens.clear();
            conditionTokens.addAll(Arrays.asList(conditionTokensArray)); // Update the List with the new tokens

            conditionTokenIDs.clear();
            for (String token : conditionTokens) {
                conditionTokenIDs.add(getTokenID(token)); // Update the token IDs
            }
        }
    }

    /**********************************************************
     * METHOD: handleIfElse(String[] tokens)
     * DESCRIPTION: Handles if-else commands like "if (condition) { ... } else { ... }".
     *              It evaluates the condition and, if true, executes the block of
     *              code inside the if statement; otherwise, it executes the code
     *              inside the else block. The else block is optional.
     * PARAMETERS: String[] tokens - An array of tokens representing the if-else statement.
     * RETURN VALUE: None
     * EXCEPTIONS: Throws an Exception if the if-else syntax is invalid or
     *             if there is an error evaluating the condition.
     **********************************************************/

    public static void handleIfElse(List<String> tokens, List<Integer> tokenIDs) throws Exception {

        String firstToken = tokens.get(0).trim();
        if(!keywordTable.contains(firstToken)){
            throw new Exception("Expected 'if' at the start of the if-else block, but found: " + firstToken);
        }

        // Find the index of the 'if' token
        int ifIndex = tokens.indexOf("if");

        // Validate the if-else structure
        validateIfElseStructure(tokens, tokenIDs);

        // Find indices for condition parentheses
        int startCondition = tokens.indexOf("(");
        int endCondition = tokens.indexOf(")");

        System.out.println("startCondition: " + startCondition);
        System.out.println("endCondition: " +endCondition);

        // Validate parentheses presence and order
        if (startCondition < 0 || endCondition < 0 || endCondition <= startCondition || endCondition - startCondition <= 1) {
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
                elseTokenIDs = tokenIDs.subList(elseOpenBraceIndex, elseCloseBraceIndex);
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
                firstToken = ifTokens.get(0).trim();

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
                        int rightValue = (Integer) evaluator.evaluate(rightOperand); // Implement this method to evaluate the math

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
                firstToken = elseTokens.get(0).trim();

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
                        String operator = elseTokens.get(1); // "="
                        String rightOperand = String.join(" ", elseTokens.subList(2, elseTokens.size() - 1)); // e.g., "x + 5"
                        System.out.println("Processing math operation: " + leftOperand + " " + operator + " " + rightOperand);

                        // Evaluate the right-hand side expression (like "x + 5")
                        int rightValue = (Integer) evaluator.evaluate(rightOperand); // Implement this method to evaluate the math

                        // Update the variable with the new value
                        symbolTable.updateValue(leftOperand, rightValue); // Implement this method to update the symbol table

                        System.out.println("Updated " + leftOperand + " to " + rightValue);
                    }
                }
            }
        }
    }

    /**********************************************************
     * METHOD: getTokenID(String token)
     * DESCRIPTION: This method checks the type of a given token and returns the corresponding
     *              token ID. The token can be a predefined keyword/operator, a variable, or
     *              a numeric literal. It checks each type in sequence and retrieves the
     *              appropriate token ID. It also handles printing the token and its ID
     *              for debugging purposes.
     * PARAMETERS: String token - The token whose ID is being retrieved.
     * RETURN VALUE: int - The token ID associated with the token, or -1 if the token is not found.
     * EXCEPTIONS: None
     **********************************************************/

    private static int getTokenID(String token) {
        // Check if the token is a predefined keyword or operator (like "if", "else", "==", etc.)
        if (keywordTable.contains(token)) {
            // Print the token ID for keywords/operators
            int tokenID = keywordTable.getTokenID(token);
            System.out.println("Token: " + token + ", Token ID: " + tokenID);
            return tokenID;
        }else if(operatorTable.contains(token)){
            int tokenID = operatorTable.getTokenID(token);
            System.out.println("Token: " +token+ ", Token ID (Operator): " + tokenID);
            return tokenID;
        }
        // Check if the token is a variable (like 'a', 'b', etc.)
        else if (symbolTable.containsVariable(token)) {
            // Retrieve the token ID for the variable from the symbol table
            int tokenID = symbolTable.getIdByName(token);
            // Print the token ID for the variable
            System.out.println("Token: " + token + ", Token ID (Variable): " + tokenID);

            // Retrieve the value of the variable for condition evaluation (use value in comparison)
//            Object variableValue = symbolTable.getValueById(tokenID);

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
        System.out.println("Unrecognized token: " +token);
        return -1;
    }

    /**********************************************************
     * METHOD: isNumericLiteral(String token)
     * DESCRIPTION: This helper method checks if a given token is a numeric literal. It tries
     *              to parse the token into an integer. If successful, it returns true; otherwise,
     *              it returns false.
     * PARAMETERS: String token - The token to check.
     * RETURN VALUE: boolean - True if the token is a numeric literal, false otherwise.
     * EXCEPTIONS: None
     **********************************************************/

    // Helper method to check if a token is a numeric literal
    private static boolean isNumericLiteral(String token) {
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**********************************************************
     * METHOD: validateIfElseStructure(List<String> tokens, List<Integer> tokenIDs)
     * DESCRIPTION: This method validates the structure of an "if-else" block. It checks for the presence
     *              of both an "if" statement and an optional "else" statement. If an "else" statement exists,
     *              it must appear after the "if" statement. If these conditions are violated, it throws an
     *              IllegalArgumentException.
     * PARAMETERS: List<String> tokens - The list of tokens to check for the structure.
     *             List<Integer> tokenIDs - The list of token IDs (not used in the method, but included
     *             for method signature consistency).
     * RETURN VALUE: None
     * EXCEPTIONS: Throws IllegalArgumentException if the structure is invalid, such as if "if" is
     *             missing or "else" appears before "if".
     **********************************************************/

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

    /**********************************************************
     * METHOD: getConditionFromWhile(String[] tokens)
     * DESCRIPTION: This method extracts the condition from a "while" loop, which is assumed to be
     *              enclosed between parentheses. The condition is expected to be between the "while" keyword
     *              and the first '{'. The method returns the condition as a string. If no valid condition
     *              is found, it returns an empty string.
     * PARAMETERS: String[] tokens - The array of tokens from which to extract the condition.
     * RETURN VALUE: String - The condition in string form, or an empty string if no valid condition is found.
     * EXCEPTIONS: None
     **********************************************************/

    private static String getConditionFromWhile(String[] tokens) {
        // The condition is typically between "while" and the first "{"
        int openParenIndex = Arrays.asList(tokens).indexOf("(");  // Find '('
        int closeParenIndex = Arrays.asList(tokens).indexOf(")");  // Find ')'

        // Extract the condition from the tokens between '(' and ')'
        if (openParenIndex != -1 && closeParenIndex != -1 && closeParenIndex > openParenIndex) {
            StringBuilder condition = new StringBuilder();
            for (int i = openParenIndex + 1; i < closeParenIndex; i++) {
                condition.append(tokens[i]).append(" ");  // Concatenate tokens inside parentheses
            }
            return condition.toString().trim();  // Return condition as a string
        }
        return "";  // Return an empty string if no valid condition
    }

    /**********************************************************
     * METHOD: getBlockTokens(String[] tokens)
     * DESCRIPTION: This method extracts the block of code inside curly braces ("{" and "}"). The block is
     *              assumed to begin after the opening brace and end before the closing brace. The method
     *              returns a list of tokens representing the block inside the braces. If no valid block is
     *              found, it returns an empty list.
     * PARAMETERS: String[] tokens - The array of tokens to extract the block from.
     * RETURN VALUE: List<String> - A list of tokens inside the braces, or an empty list if no block is found.
     * EXCEPTIONS: None
     **********************************************************/

    private static List<String> getBlockTokens(String[] tokens) {
        List<String> blockTokens = new ArrayList<>();
        int openBraceIndex = Arrays.asList(tokens).indexOf("{");  // Find '{'
        int closeBraceIndex = Arrays.asList(tokens).indexOf("}");  // Find '}'

        if (openBraceIndex != -1 && closeBraceIndex != -1 && closeBraceIndex > openBraceIndex) {
            for (int i = openBraceIndex + 1; i < closeBraceIndex; i++) {
                blockTokens.add(tokens[i]);  // Add tokens inside the braces
            }
        }
        return blockTokens;
    }

}
