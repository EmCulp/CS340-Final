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

import java.io.*;
import java.util.*;

public class Compiler {
    private static SymbolTable symbolTable;
    private static LiteralTable literalTable;
    private static KeywordTable keywordTable;
    private static OperatorTable operatorTable;
    private static Evaluator evaluator;
    private static Tokenization tokenizer;
    private static Compiler compiler;
    private static String inputFile = "C:\\Users\\emily\\OneDrive\\Documents\\Year3\\CS340\\Final - Compiler\\input.txt";
    private static String outputFile = "C:\\Users\\emily\\OneDrive\\Documents\\Year3\\CS340\\Final - Compiler\\output.txt";

    private static MIPSGenerator mipsGenerator = new MIPSGenerator();
    private static Set<String> checkedVariables = new HashSet<>();

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
        StringBuilder statement = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             PrintWriter writer = outputFile != null ? new PrintWriter(new FileWriter(outputFile)) : null) {

            System.out.println("Processing commands from file: " + inputFile);
            if (writer != null) {
                System.out.println("Writing output to file: " + outputFile);
            }

            String commandLine;
            boolean isInBlock = false; // Tracks whether we are inside a block
            StringBuilder blockBuffer = new StringBuilder(); // Buffer for block content

            while ((commandLine = reader.readLine()) != null) {
                // Skip empty lines and comments
                if (commandLine.trim().isEmpty() || commandLine.startsWith("#")) {
                    continue;
                }

                // Check for block start (open brace)
                if (commandLine.contains("{")) {
                    isInBlock = true;
                    blockBuffer.append(commandLine.trim()).append(" ");
                    continue;
                }

                // Accumulate lines in blockBuffer if inside a block
                if (isInBlock) {
                    blockBuffer.append(commandLine.trim()).append(" ");

                    // Check for block end (close brace)
                    if (commandLine.contains("}")) {
                        isInBlock = false;

                        // Process the complete block
                        String blockContent = blockBuffer.toString().trim();
                        blockBuffer.setLength(0); // Clear the buffer for the next block

                        // Tokenize the block
                        String[] blockTokens = tokenizer.tokenize(blockContent);
                        String tokenString = "Tokens (block): " + String.join(" ", blockTokens);
                        System.out.println(tokenString);

                        if (writer != null) {
                            writer.println(tokenString);
                        }

                        // Execute the block
                        try {
                            processBlock(blockTokens); // Delegate to processBlock
                        } catch (Exception e) {
                            System.out.println("Error processing block: " + e.getMessage());
                        }
                    }

                    continue; // Skip further processing for block lines
                }

                // Accumulate non-block single-line statements
                statement.append(commandLine.trim()).append(" ");

                // Check if the statement is complete (ends with a semicolon)
                if (statement.toString().trim().endsWith(";")) {
                    String completeCommand = statement.toString().trim();
                    statement.setLength(0); // Clear the accumulator for the next statement

                    // Tokenize the complete command
                    String[] tokens = tokenizer.tokenize(completeCommand);
                    String tokenString = "Tokens (main): " + String.join(" ", tokens);
                    System.out.println(tokenString);

                    if (writer != null) {
                        writer.println(tokenString);
                    }

                    // Execute the single-line command
                    try {
                        executeCommand(tokens);
                    } catch (Exception e) {
                        System.out.println("Error executing command: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading or writing files: " + e.getMessage());
        }

        // Display the symbol and literal tables at the end
        symbolTable.display();
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

        if(tokens[0].equals("if")){
            System.out.println("handleIfElse");
            handleIfElse(tokens, id);
            return;
        }

        if(tokens[0].equals("while")){
            String condiiton = getConditionFromWhile(tokens);
            List<String> blockTokens = getBlockTokens(tokens);
            handleWhileLoop(condiiton, blockTokens);
            return;
        }

        if (tokens[0].equals("for") && tokens.length > 3 && !tokens[2].equals("integer")) {
            handleForLoop(tokens);
            return;
        }

        if(tokens[0].equals("for") && tokens.length > 3 && tokens[2].equals("integer")){
            handleForIntegerLoop(tokens);
            return;
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
            } else if(tokens[0].equals("boolean")) {
                handleBoolean(tokens);
            }else if(tokens[0].equals("double")) {
                handleDouble(tokens);
            }else if(tokens[0].equals("string")){
                handleString(tokens);
            }else{
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

    private static void handleDouble(String[] tokens) {
        if (tokens.length == 3 && tokens[0].equals("double")) {
            String variableName = tokens[1].replace(";", "");  // Remove semicolon if present
            System.out.println("Checking if variable exists: " + variableName + " => " + symbolTable.containsVariable(variableName));

            if (!symbolTable.containsVariable(variableName)) {
                // Add the double literal to the literal table if not already added
                addDoubleLiteralIfNotExist(0.0);  // Default to 0.0
                // Add the double variable with default value to the symbol table
                symbolTable.addEntry(variableName, "double", 0.0, "global");  // Default value
//                System.out.println("Not in symbol table... Now added to Symbol Table: " + variableName);
            } else {
                System.out.println("Error: Variable " + variableName + " is already declared.");
            }
        } else if (tokens.length == 5 && tokens[0].equals("double") && tokens[2].equals("=")) {
            String variableName = tokens[1];
            double value;  // Parse double value from token

            try{
                value = Double.parseDouble(tokens[3].replace(";", ""));
            }catch(NumberFormatException e){
                System.out.println("Error: Invalid double value provided");
                return;
            }

            System.out.println("Checking if variable exists: " + variableName + " => " + symbolTable.containsVariable(variableName));

            if (!symbolTable.containsVariable(variableName)) {
                // Add the double literal to the literal table if not already added
                addDoubleLiteralIfNotExist(value);  // Add the literal value
                // Add the double variable with the parsed value to the symbol table
                symbolTable.addEntry(variableName, "double", value, "global");  // Add to symbol table with value
                System.out.println("Added to Symbol Table with value: " + variableName + " = " + value);
            } else {
                System.out.println("Error: Variable " + variableName + " is already declared.");
            }
        } else {
            System.out.println("Syntax error: Invalid double declaration or assignment.");
        }
    }

    private static void addDoubleLiteralIfNotExist(double value) {
        // Check if the double literal is already in the table
        if (!literalTable.containsValue(value)) {
            // Add the double literal to the table if it doesn't already exist
            literalTable.addLiteral(value);  // Ensure this method is defined
            System.out.println("Double Literal Added");
        }
    }

    private static void handleBoolean(String[] tokens) {
        if (tokens.length == 3 && tokens[0].equals("boolean")) {
            String variableName = tokens[1].replace(";", "");  // Remove semicolon if present
            System.out.println("Checking if variable exists: " + variableName + " => " + symbolTable.containsVariable(variableName));

            if (!symbolTable.containsVariable(variableName)) {
                // Add the boolean variable with default value
                addBooleanLiteralIfNotExist("false");
                symbolTable.addEntry(variableName, "boolean", false, "global");  // Default to false
                System.out.println("Not in symbol table... Now added to Symbol Table: " + variableName);
            } else {
                System.out.println("Error: Variable " + variableName + " is already declared.");
            }
        } else if (tokens.length == 5 && tokens[0].equals("boolean") && tokens[2].equals("=")) {
            String variableName = tokens[1];
            boolean value = Boolean.parseBoolean(tokens[3].replace(";", ""));  // Parse boolean value from token
            System.out.println("Checking if variable exists: " + variableName + " => " + symbolTable.containsVariable(variableName));

            if (!symbolTable.containsVariable(variableName)) {
                // Add the boolean literal to literal table if not already added
                addBooleanLiteralIfNotExist(value ? "true" : "false");
                symbolTable.addEntry(variableName, "boolean", value, "global");  // Add boolean value to symbol table
                System.out.println("Added to Symbol Table with value: " + variableName + " = " + value);
            } else {
                System.out.println("Error: Variable " + variableName + " is already declared.");
            }
        } else {
            System.out.println("Syntax error: Invalid boolean declaration or assignment.");
        }
    }

    private static void addBooleanLiteralIfNotExist(String value){
        if(!literalTable.containsValue(value)){
            literalTable.addLiteral(value);
            System.out.println("Added to Boolean Literal Table: " + value);
        }
    }

    private static void handleString(String[] tokens) {
        // Check if it's a declaration (e.g., string name;)
        if (tokens.length == 2 && tokens[1].endsWith(";")) {
            String variableName = tokens[1].substring(0, tokens[1].length() - 1); // Remove the semicolon
            String type = "string";  // Type of the variable
            String scope = "global";  // Default scope (adjust as necessary)
            symbolTable.addEntry(variableName, type, "", scope); // Initialize with an empty string
            System.out.println("Declared string variable: " + variableName);
        }
        // Check if it's an assignment (e.g., string name = "Hello";)
        else if (tokens.length == 5 && tokens[2].equals("=") && tokens[4].equals(";")) {
            String variableName = tokens[1];
            String value = tokens[3];

            // Check if the value is a valid string (starts and ends with double quotes)
            if (value.matches("\"[^\"]*\"")) {
                String assignedValue = value.substring(1, value.length() - 1); // Remove the surrounding quotes

                literalTable.addLiteral(assignedValue);

                String type = "string";  // Type of the variable
                String scope = "global";  // Default scope (adjust as necessary)
                symbolTable.addEntry(variableName, type, assignedValue, scope);
                System.out.println("Assigned string: " + assignedValue + " to variable: " + variableName);
            } else {
                System.out.println("Syntax error: Invalid string value for variable " + variableName);
            }
        } else {
            System.out.println("Syntax error: Invalid string declaration or assignment.");
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
//        System.out.println("VariableName: " +variableName);
        Integer variableID = symbolTable.getIdByName(variableName); // Fetch variable ID
//        System.out.println("ID: " +variableID);

        if (variableID == null) {
            System.out.println("Error: Variable " + variableName + " is undeclared.");
            return;
        }

        String variableType = symbolTable.getTypeByName(variableName);
        if(variableType == null){
            System.out.println("Error: Type for variable " +variableName+ " is unknown");
            return;
        }

        // Prompt for user input
        Scanner scanner = new Scanner(System.in);
        System.out.print("=> ");
        Object value = null;

        try{
            switch(variableType){
                case "integer":
                    value = scanner.nextInt();
                    break;
                case "double":
                    value = scanner.nextDouble();
                    break;
                case "boolean":
                    value = scanner.nextBoolean();
                    break;
                case "string":
                    value = scanner.nextLine();
                    break;
                default:
                    System.out.println("Error: Unsupported variable type " + variableType);
                    return;
            }
        }catch(InputMismatchException e){
            System.out.println("Error: Invalid input for variable type " + variableType);
            return;
        }

        // Assign value to the variable
        symbolTable.updateValue(variableName, value);
        System.out.println("Assigned value " + value + " to variable " + variableName);

        int literalID = literalTable.addLiteral(value);
        System.out.println("Literal value " + value + " has been added with ID " + literalID);

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
                String variableType = symbolTable.getTypeByName(variableName);

                if ("int".equals(variableType)) {
                    if (result instanceof Double) {
                        double doubleResult = (Double) result;
                        if (doubleResult != Math.floor(doubleResult)) {
                            throw new RuntimeException("Type mismatch: Cannot assign non-integer value to integer variable.");
                        }
                        result = (int) doubleResult;
                    }
                    if (result instanceof Integer) {
                        symbolTable.updateValue(variableName, (Integer) result);
                        System.out.println("Assigned integer value: " + result + " to variable: " + variableName);
                    } else {
                        throw new RuntimeException("Type mismatch: Unsupported value type.");
                    }
                } else {
                    // Handle other types (e.g., double) if needed
                    symbolTable.updateValue(variableName, result);
                }

                Integer assignTokenID = operatorTable.get("=");
                Integer semicolonTokenID = operatorTable.get(";");

                // Print TokenIDs (example, adjust according to your actual logic)
                System.out.print("TokenIDs: " + symbolTable.getIdByName(variableName) + " " + assignTokenID + " " + literalTable.getLiteralID(result) + " " + semicolonTokenID + " ");
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

        // Create a list of token IDs for the condition
        List<Integer> conditionTokenIDs = new ArrayList<>();

        // Get the token ID for each token in the condition
        for (String token : conditionTokensArray) {
            int tokenID = getTokenID(token);  // Get the token ID for the token
            conditionTokenIDs.add(tokenID);   // Add the token ID to the list
        }

        // Loop until the condition evaluates to false
        while (evaluator.evaluateCondition(conditionTokensArray)) {
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

                        // MATH
                        if (fullStatement.contains("=")) {
                            String[] parts = fullStatement.split("=");  // Split by '='
                            String variableName = parts[0].trim();
                            String expression = parts[1].trim().replace(";", "");  // Extract the expression

                            // Evaluate the right-hand side expression and cast the result to Double
                            Object evaluatedValueObject = evaluator.evaluateExpression(expression); // Get the result as Object

                            // Cast Object to Double, then convert it to int
                            double evaluatedValue = (Double) evaluatedValueObject;  // Cast Object to Double
                            int newValue = (int) evaluatedValue;  // Convert Double to int

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

            conditionTokenIDs.clear();
            for (String token : conditionTokensArray) {
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

//    public static void handleIfElse(String[] tokens, List<Integer> tokenIDs) throws Exception {
//
//        String firstToken = tokens[0].trim();
//        if (!keywordTable.contains(firstToken)) {
//            throw new Exception("Expected 'if' at the start of the if-else block, but found: " + firstToken);
//        }
//
//        // Find the index of the 'if' token
//        int ifIndex = findIndex(tokens, "if");
//
//        // Validate the if-else structure
//        validateIfElseStructure(tokens, tokenIDs);
//
//        // Find indices for condition parentheses
//        int startCondition = findIndex(tokens, "(");
//        int endCondition = findIndex(tokens, ")");
//
//        System.out.println("startCondition: " + startCondition);
//        System.out.println("endCondition: " + endCondition);
//
//        // Validate parentheses presence and order
//        if (startCondition < 0 || endCondition < 0 || endCondition <= startCondition || endCondition - startCondition <= 1) {
//            throw new Exception("Invalid if condition syntax: missing or misplaced parentheses");
//        }
//
//        // Extract condition tokens safely
//        String[] conditionTokens = Arrays.copyOfRange(tokens, startCondition + 1, endCondition);
//        List<Integer> conditionTokenIDs = tokenIDs.subList(startCondition + 1, endCondition);
//
//        // Print debug info
//        System.out.print("Condition TokenIDs: ");
//        for (Integer tokenID : conditionTokenIDs) {
//            System.out.print(tokenID + " ");
//        }
//        System.out.println();
//
//        // Evaluate the condition
//        Evaluator evaluator = new Evaluator(symbolTable, literalTable); // Assuming symbolTable is accessible
//        boolean conditionResult = evaluator.evaluateCondition(conditionTokens);
//
//        // Proceed with if-else logic
//        int elseIndex = findIndex(tokens, "else");
//
//        // Find the opening and closing braces for the if block
//        int openBraceIndex = findIndex(tokens, "{");
//        int closeBraceIndex = findLastIndex(tokens, "}");
//
//        if (openBraceIndex < 0 || closeBraceIndex < 0 || closeBraceIndex <= openBraceIndex) {
//            throw new Exception("Invalid block structure: missing or mismatched braces");
//        }
//
//        // Extract tokens for the if block
//        String[] ifTokens = Arrays.copyOfRange(tokens, openBraceIndex + 1, closeBraceIndex);
//
//        // Handle the optional else block if present
//        String[] elseTokens = new String[0];
//        List<Integer> elseTokenIDs = new ArrayList<>();
//        if (elseIndex != -1) {
//            // Find the opening and closing braces specifically for the else block
//            int elseOpenBraceIndex = -1;
//            int elseCloseBraceIndex = -1;
//
//            // Find the opening brace for the 'else' block
//            for (int i = elseIndex; i < tokens.length; i++) {
//                if (tokens[i].equals("{")) {
//                    elseOpenBraceIndex = i;
//                    break;
//                }
//            }
//
//            // Find the closing brace for the 'else' block, after the opening brace
//            if (elseOpenBraceIndex != -1) {
//                for (int i = elseOpenBraceIndex + 1; i < tokens.length; i++) {
//                    if (tokens[i].equals("}")) {
//                        elseCloseBraceIndex = i;
//                        break;
//                    }
//                }
//            }
//
//            // Extract tokens for the else block if the braces were found
//            if (elseOpenBraceIndex != -1 && elseCloseBraceIndex != -1 && elseCloseBraceIndex > elseOpenBraceIndex) {
//                elseTokens = Arrays.copyOfRange(tokens, elseOpenBraceIndex + 1, elseCloseBraceIndex);
//                elseTokenIDs = tokenIDs.subList(elseOpenBraceIndex, elseCloseBraceIndex);
//            }
//        }
//
//        // Execute the appropriate block based on the evaluated condition
//        if (conditionResult) {
//            processBlock(ifTokens, tokenIDs, evaluator);
//        } else {
//            processBlock(elseTokens, tokenIDs, evaluator);
//        }
//    }

    // Helper method to find the index of a token in the array

    public static int findBraceIndex(String[] tokens, int startIndex) {
        int braceCount = 0;

        for (int i = startIndex; i < tokens.length; i++) {
            String token = tokens[i].trim();

            // Check for opening brace '{'
            if (token.equals("{")) {
                braceCount++;
            }

            // Check for closing brace '}'
            if (token.equals("}")) {
                braceCount--;

                // If braceCount reaches 0, we've found the matching closing brace
                if (braceCount == 0) {
                    return i;  // Return index of the closing brace
                }
            }
        }

        // If no matching closing brace is found, return -1
        return -1;
    }

    public static void handleIfElse(String[] tokens, List<Integer> tokenIDs) throws Exception {
        System.out.println("Entered HandleIfElse...");

        String firstToken = tokens[0].trim();

        // Ensure the first token is 'if'
        if (!firstToken.equals("if")) {
            throw new Exception("Expected 'if' at the start of the if-else block, but found: " + firstToken);
        }

        // Validate the if-else structure
        validateIfElseStructure(tokens, tokenIDs);

        System.out.println("Structure correct...");

        // Find the indices for the condition parentheses
        int startCondition = findIndex(tokens, "(");
        int endCondition = findIndex(tokens, ")");

        // Validate parentheses presence and order
        if (startCondition < 0 || endCondition < 0 || endCondition <= startCondition || endCondition - startCondition <= 1) {
            throw new Exception("Invalid if condition syntax: missing or misplaced parentheses");
        }

        // Extract condition tokens
        String[] conditionTokens = Arrays.copyOfRange(tokens, startCondition + 1, endCondition);
        List<Integer> conditionTokenIDs = tokenIDs.subList(startCondition + 1, endCondition);

        // Evaluate the condition (true or false)
        Evaluator evaluator = new Evaluator(symbolTable, literalTable);
        boolean conditionResult = evaluator.evaluateCondition(conditionTokens);

        // Find the opening and closing braces for the if block
        int openBraceIndex = findNextToken(tokens, "{", endCondition);
        int closeBraceIndex = findMatchingBrace(tokens, openBraceIndex+1);

        if(openBraceIndex == -1 || closeBraceIndex == -1){
            throw new Exception("Invalid if block structure: Missing braces");
        }

        // Extract tokens for the if block
        String[] ifTokens = Arrays.copyOfRange(tokens, openBraceIndex + 1, closeBraceIndex);

        // Extract tokens for the else block (if it exists)
        int elseIndex = findNextToken(tokens, "else", closeBraceIndex+1);
        String[] elseTokens = new String[0];
        if (elseIndex != -1) {
            int elseOpenBraceIndex = findBraceIndex(tokens, elseIndex + 1);
            int elseCloseBraceIndex = findBraceIndex(tokens, elseOpenBraceIndex + 1);
            if (elseOpenBraceIndex != -1 && elseCloseBraceIndex != -1) {
                throw new Exception("Invalid else block structure: Missing braces");
            }
            elseTokens = Arrays.copyOfRange(tokens, elseOpenBraceIndex+1, elseCloseBraceIndex);
        }

        System.out.println("Condition Result: " +conditionResult);

        // Execute the appropriate block based on the evaluated condition
        if (conditionResult) {
            System.out.println("Executing if block");
            // If condition is true, execute the 'if' block
            processBlock(ifTokens);
        } else if (elseTokens.length > 0) {
            System.out.println("Executing else block");
            // If condition is false, execute the 'else' block
            processBlock(elseTokens);
        }else{
            System.out.println("No ELSE block to execute");
        }
    }


    private static int findIndex(String[] tokens, String token) {
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals(token)) {
                return i;
            }
        }
        return -1; // Token not found
    }

    private static int findMatchingBrace(String[] tokens, int start) {
        int braceCount = 0;
        for (int i = start; i < tokens.length; i++) {
            if (tokens[i].equals("{")) {
                braceCount++;
            } else if (tokens[i].equals("}")) {
                braceCount--;
                if (braceCount == 0) {
                    return i; // Found the matching closing brace
                }
            }
        }
        throw new IllegalArgumentException("Unmatched braces in token array");
    }

    private static int findNextToken(String[] tokens, String target, int start) {
        for (int i = start; i < tokens.length; i++) {
            if (tokens[i].equals(target)) {
                return i;
            }
        }
        return -1; // Token not found
    }

    // Method to process a block of tokens (either if or else)
    private static void processBlock(String[] tokens) throws Exception {
        for (int i = 0; i < tokens.length; i++) {
            String currentToken = tokens[i];
            System.out.println("Processing Token: " + currentToken);

            // Handle variable assignment
            if (isVariable(currentToken)) {
                String variableName = currentToken;

                // Check for assignment operator
                if (i + 1 < tokens.length && tokens[i + 1].equals("=")) {
                    // Extract the right-hand side expression (up to semicolon)
                    int semicolonIndex = findIndex(tokens, ";");
                    if (semicolonIndex == -1) {
                        throw new Exception("Missing semicolon in assignment.");
                    }

                    String expression = String.join(" ", Arrays.copyOfRange(tokens, i + 2, semicolonIndex));
                    System.out.println("Expression to evaluate: " + expression);

                    // Evaluate the expression
                    Evaluator evaluator = new Evaluator(symbolTable, literalTable);
                    Object result = evaluator.evaluateExpression(expression);

                    String expectedType = symbolTable.getTypeByName(variableName);
                    if(expectedType == null){
                        throw new Exception("Variable '" +variableName+ "' not declared");
                    }

                    String resultType = getResultType(result);
                    if(!expectedType.equals(resultType)){
                        throw new Exception("Type mismatch: Expected " + expectedType+ " but got " +resultType);
                    }

                    if(!getResultType(result).equals("unknown")){
                        if(!literalTable.containsValue(result)){
                            literalTable.addLiteral(result);
                        }
                    }

                    // Update symbol table
                    if (symbolTable.containsVariable(variableName)) {
                        symbolTable.updateValue(variableName, result);
                    } else {
                        throw new Exception("Variable " + variableName + " not declared.");
                    }

                    // Skip processed tokens
                    i = semicolonIndex;
                }
            }

            // Handling the if-else condition
            if (currentToken.equals("if")) {
                // Extract the condition (tokens after 'if' and before opening parenthesis)
                int openParenIndex = i + 1;
                int closeParenIndex = findIndex(tokens, ")");
                if (openParenIndex == -1 || closeParenIndex == -1) {
                    throw new Exception("Missing parentheses for 'if' condition.");
                }

                // Extract the condition expression (tokens between '(' and ')')
                String[] condition = Arrays.copyOfRange(tokens, openParenIndex + 1, closeParenIndex);
                System.out.println("Evaluating 'if' condition: " + condition);

                // Evaluate the condition
                Evaluator evaluator = new Evaluator(symbolTable, literalTable);
                boolean conditionResult = evaluator.evaluateCondition(condition);

                // Process the corresponding block based on the condition's result
                if (conditionResult) {
                    // Process the block inside the 'if' statement
                    int blockStart = closeParenIndex + 1;
                    int blockEnd = findBlockEnd(tokens, blockStart);  // Implement findBlockEnd to find the end of the 'if' block
                    String[] ifBlock = Arrays.copyOfRange(tokens, blockStart, blockEnd);
                    processBlock(ifBlock);
                    i = blockEnd - 1; // Update index to the end of the 'if' block
                }

                // If you want to handle an 'else' block, you'll need additional logic here.
                break;
            }
        }
    }

    private static String getResultType(Object result){
        if(result instanceof Integer){
            return "int";
        }else if(result instanceof Double){
            return "double";
        }else if(result instanceof String){
            return "string";
        }else if(result instanceof Boolean){
            return "boolean";
        }
        return "unknown";
    }

    private static int findBlockEnd(String[] tokens, int startIndex) throws Exception {
        // Keep track of the number of opening braces to balance with closing braces
        int openBraces = 0;

        for (int i = startIndex; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.equals("{")) {
                openBraces++;
            } else if (token.equals("}")) {
                openBraces--;
                if (openBraces == 0) {
                    return i + 1;  // Found the closing brace, return the next token index
                }
            }
        }

        throw new Exception("Unmatched braces in block.");
    }

    public static boolean isVariable(String token){
        return token != null && !keywordTable.contains(token) && !isOperator(token);
    }

    public static boolean isOperator(String token) {
        // Check if the token is one of the common operators
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/") ||
                token.equals("=") || token.equals("==") || token.equals("!=") ||
                token.equals("<") || token.equals(">") || token.equals("<=") || token.equals(">=") ||
                token.equals("&&") || token.equals("||");
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

    public static void validateIfElseStructure(String[] tokens, List<Integer> tokenIDs) {
        List<String> tokenList = Arrays.asList(tokens);

        int ifIndex = tokenList.indexOf("if");
        int elseIndex = tokenList.indexOf("else");

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

    public static void handleForLoop(String[] loopTokens) throws Exception {
        // Step 1: Locate the parentheses
        int openParenIndex = Arrays.asList(loopTokens).indexOf("(");
        int closeParenIndex = Arrays.asList(loopTokens).indexOf(")");

        if (openParenIndex == -1 || closeParenIndex == -1) {
            throw new IllegalArgumentException("Malformed for loop: Missing parentheses.");
        }

        // Step 2: Extract the part inside the parentheses
        String[] insideParentheses = Arrays.copyOfRange(loopTokens, openParenIndex + 1, closeParenIndex);

        // Step 3: Parse initialization, condition, and increment
        String insideParenthesesString = String.join(" ", insideParentheses);
        String[] loopParts = insideParenthesesString.split(";");

        if (loopParts.length != 3) {
            throw new IllegalArgumentException("Malformed for loop: Expected initialization, condition, and increment.");
        }

        String initialization = loopParts[0].trim(); // Initialization: "i = 0"
        String condition = loopParts[1].trim();      // Condition: "i < 5"
        String increment = loopParts[2].trim();      // Increment: "i++"

        // Step 4: Initialize the loop variable
        String[] initParts = initialization.split("=");
        if (initParts.length != 2) {
            throw new IllegalArgumentException("Malformed initialization: " + initialization);
        }
        String loopVar = initParts[0].trim();  // e.g., "i"
        int initValue = Integer.parseInt(initParts[1].trim());  // e.g., 0

        // Add the loop variable to the symbol table
        if(!symbolTable.containsVariable(loopVar)){
            symbolTable.addEntry(loopVar, "int", initValue, "global");
        }

        // Step 5: Parse the condition
        String[] conditionTokens = condition.split(" ");

        // Step 6: Execute the loop
        boolean conditionResult = evaluator.evaluateCondition(conditionTokens);
        while (conditionResult) {
            // Debugging: Check the value of 'i' before executing the loop body
            System.out.println("Before loop body: i = " + symbolTable.getValueById(symbolTable.getIdByName(loopVar)));

            // Execute the loop body
            List<String> tokensList = Arrays.asList(loopTokens);
            int start = tokensList.indexOf("{");
            int end = tokensList.lastIndexOf("}");
            String[] bodyTokens = tokensList.subList(start + 1, end).toArray(new String[0]);
            executeLoopBody(bodyTokens);

            // Call evaluateIncrementOrDecrement method to handle "i++" or similar increments
            if (increment.contains("++")) {  // Check if it's an increment operation
                String variableName;
                if (increment.startsWith("++")) {
                    variableName = increment.substring(2).trim();  // Extract variable name for "++i"
                    evaluator.evaluateIncrementOrDecrement("++", variableName);
                } else if (increment.endsWith("++")) {
                    variableName = increment.substring(0, increment.length() - 2).trim();  // Extract variable name for "i++"
                    evaluator.evaluateIncrementOrDecrement("++", variableName);
                }
            } else if (increment.contains("--")) {  // Check if it's a decrement operation
                String variableName;
                if (increment.startsWith("--")) {
                    variableName = increment.substring(2).trim();  // Extract variable name for "--i"
                    evaluator.evaluateIncrementOrDecrement("--", variableName);
                } else if (increment.endsWith("--")) {
                    variableName = increment.substring(0, increment.length() - 2).trim();  // Extract variable name for "i--"
                    evaluator.evaluateIncrementOrDecrement("--", variableName);
                }
            } else {
                throw new IllegalArgumentException("Invalid increment/decrement operation: " + increment);
            }


            // Recheck the condition after incrementing
            conditionResult = evaluator.evaluateCondition(conditionTokens);
        }
    }

    public static void handleForIntegerLoop(String[] tokens) throws Exception {
        // Step 1: Ensure the tokens array has enough elements to parse a basic for loop
        if (tokens.length < 13) {
            throw new IllegalArgumentException("Invalid 'for' loop structure.");
        }

        // Step 2: Determine if the loop involves a new variable declaration or uses an existing one
        String variableType = tokens[2];  // "integer" or "int"
        String variableName = tokens[3];  // "i"
        int startValue = 0;  // Default value
        boolean isNewVariable = false;

        // Case 1: Variable declaration inside the for loop (e.g., for(integer i = 0; ...))
        if (tokens[2].equals("integer")) {
            startValue = Integer.parseInt(tokens[5]);  // "0" (initial value)
            symbolTable.addEntry(variableName, "int", startValue, "global");
            isNewVariable = true;
        }
        // Case 2: Using an existing variable (e.g., integer i; for(i = 0; ...))
        else if (symbolTable.containsVariable(variableName)) {
            startValue = (int) symbolTable.getValueById(symbolTable.getIdByName(variableName));  // Get initial value from the symbol table
        } else {
            throw new IllegalArgumentException("Variable not declared: " + variableName);
        }

        // Step 3: Parse the loop condition (assume it's in the form "i < 10")
        String leftOperand = tokens[7];  // "i"
        String operator = tokens[8];     // "<"
        int rightOperand = Integer.parseInt(tokens[9]); // "10"

        // Prepare the condition tokens for evaluation
        String[] conditionTokens = new String[] { leftOperand, operator, String.valueOf(rightOperand) };

        // Step 4: Parse the increment (assume it's in the format "i++")
        String incrementOperator = tokens[12];  // "i++"

        // Step 5: Start the loop, evaluate the condition, and execute the body
        for (int i = startValue; evaluator.evaluateCondition(conditionTokens); i++) {
            // Execute the loop body (you can customize this part to handle loop body statements)
            executeLoopBody(tokens);  // Replace with your method to process the loop body

            // Increment the variable (e.g., i++)
            evaluator.evaluateIncrementOrDecrement(incrementOperator, variableName);

            // If it's a new variable, update it in the symbol table for each iteration
            if (isNewVariable) {
                symbolTable.updateValue(variableName, i);
            }
        }
    }

    private static void executeLoopBody(String[] loopTokens) throws Exception {
        // The loopTokens array contains the body of the loop (e.g., "{ print(i); }")
        StringBuilder statementBuilder = new StringBuilder();
        boolean insideBrackets = false;

        // Loop through the tokens and collect statements
        for (String token : loopTokens) {
            token = token.trim(); // Trim any whitespace

            // Detect the opening bracket
            if (token.equals("{")) {
                insideBrackets = true;
                continue; // Skip the opening bracket
            }

            // Detect the closing bracket, indicating end of loop body
            if (token.equals("}")) {
                insideBrackets = false;
                break; // Exit loop body processing
            }

            // Only process tokens inside the loop body
            if (insideBrackets && !token.isEmpty()) {
                statementBuilder.append(token).append(" "); // Build the statement

                // When a semicolon is encountered, execute the statement
                if (token.equals(";")) {
                    String fullStatement = statementBuilder.toString().trim(); // Full statement ends with ';'

                    // Pass the statement to executeCommand
                    System.out.println("Executing statement: " + fullStatement);
                    executeCommand(fullStatement.split(" ")); // Split into tokens and execute

                    // Reset the statement builder for the next statement
                    statementBuilder.setLength(0);
                }
            }
        }
    }
}
