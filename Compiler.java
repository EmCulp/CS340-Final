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
    private static String inputFile = "C:\\Users\\emily\\OneDrive\\Documents\\Year3\\CS340\\Final - Compiler\\input.txt";
    private static String outputFile = "C:\\Users\\emily\\OneDrive\\Documents\\Year3\\CS340\\Final - Compiler\\output.txt";
    private static int controlStructure = 0;
    private static MIPSGenerator mipsGenerator;
    private static boolean generateMips = true;
    private static TokenIDConverter converter;

    static{
        symbolTable =  new SymbolTable();
        literalTable = new LiteralTable();
        mipsGenerator = new MIPSGenerator(symbolTable);
        evaluator = new Evaluator(symbolTable, literalTable, mipsGenerator);
        keywordTable = new KeywordTable();
        operatorTable = new OperatorTable();
        tokenizer = new Tokenization();
        converter = new TokenIDConverter(symbolTable, literalTable, operatorTable, keywordTable);
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

    public static void main(String[] args) {
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
                            executeCommand(blockTokens); // Delegate to processBlock
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

            // Now call the printTokenIDsInBinary method to write output to the file
            if (writer != null) {
                writer.println();
              converter.printTokenIDsInBinary(symbolTable, writer); // Modify to call the correct instance
                writer.println();

                converter.printTokenIDsInBinary(literalTable, writer); // Modify to call the correct instance
                writer.println();

               converter.printTokenIDsInBinary(operatorTable, writer); // Modify to call the correct instance
                writer.println();

                converter.printTokenIDsInBinary(keywordTable, writer); // Modify to call the correct instance
                writer.println();
            }


        } catch (IOException e) {
            System.out.println("Error reading or writing files: " + e.getMessage());
        }

        // Display the symbol and literal tables at the end
        symbolTable.display();
        literalTable.printTable();
        mipsGenerator.generateDataSection();
        mipsGenerator.printMipsCode();
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
        if (tokens.length >= 3 && tokens[tokens.length - 1].trim().equals(";")) {
            if (tokens[0].equals("integer")) {
                if (tokens.length == 3) {
                    handleVariableDeclaration(tokens);  // Variable declaration
                } else if (tokens.length == 5 && tokens[2].equals("=")) {
                    handleAssignment(tokens);  // Variable assignment
                } else {
                    System.out.println("Syntax error: Invalid variable declaration.");
                }
            } else if (tokens.length >= 3 && tokens[1].equals("=")) {
                handleAssignment(tokens);  // Assignment
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

        if (!keywordTable.contains("integer")) {
            System.out.println("Syntax error: Invalid keyword 'integer'.");
            return;
        }

        if (!operatorTable.contains(";")) {
            System.out.println("Syntax error: Invalid operator ';'.");
            return;
        }

        if (symbolTable.containsVariable(variableName)) {
            System.out.println("Syntax error: Variable '" + variableName + "' already declared.");
            return;
        }

        String scope = isInsideControlStructure() ? "local" : "global";

        // Determine if we are inside a control structure (if-else, while, or for loop)
        if (isInsideControlStructure()) {
            String reg = mipsGenerator.allocateTempRegister();
            // Inside a control structure (local scope) - Add to stack
//            mipsGenerator.pushToStack(reg);  // Add to the stack (allocate space)
            symbolTable.addEntry(variableName, "int", 0, scope, reg);  // Add to symbol table as local

            System.out.println("Local variable declaration inside control structure: " + variableName);
        } else {
            // Global variable declaration - Add to .data section
            mipsGenerator.addToDataSection(variableName, "0", "int");
            symbolTable.addEntry(variableName, "int", 0, scope, null);

            System.out.println("Global variable declaration: " + variableName);
        }

        int variableID = symbolTable.getIdByName(variableName);
        int literalID = literalTable.addLiteral(0);  // Default value of 0
        System.out.println("Added literal: 0 with ID " + literalID + " to the Literal Table.");
    }

    private static void handleDouble(String[] tokens) {
        if (tokens.length == 3 && tokens[0].equals("double")) {
            String variableName = tokens[1].replace(";", "");  // Remove semicolon if present
            System.out.println("Checking if variable exists: " + variableName + " => " + symbolTable.containsVariable(variableName));

            if (!symbolTable.containsVariable(variableName)) {
                if (isInsideControlStructure()) {
                    // Inside control structure (local scope) - Allocate a temporary register
                    String register = mipsGenerator.allocateTempRegister();
//                    mipsGenerator.pushToStack(register); // Add to stack for local variable

                    // Add the double variable to the symbol table with the register (local scope)
                    symbolTable.addEntry(variableName, "double", 0.0, "local", register);  // Default value 0.0 for local
                    System.out.println("Local double variable declared inside control structure: " + variableName);
                } else {
                    // Global variable (add to .data section)
                    mipsGenerator.addToDataSection(variableName, "0.0", "double");

                    // Add the double literal to the literal table if not already added
                    addDoubleLiteralIfNotExist(0.0);  // Default to 0.0
                    // Add the double variable with default value to the symbol table
                    symbolTable.addEntry(variableName, "double", 0.0, "global", null);  // Default value for global
                    System.out.println("Global double variable declared: " + variableName);
                }
            } else {
                System.out.println("Error: Variable " + variableName + " is already declared.");
            }
        } else if (tokens.length == 5 && tokens[0].equals("double") && tokens[2].equals("=")) {
            String variableName = tokens[1];
            double value;  // Parse double value from token

            try {
                value = Double.parseDouble(tokens[3].replace(";", ""));
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid double value provided");
                return;
            }

            System.out.println("Checking if variable exists: " + variableName + " => " + symbolTable.containsVariable(variableName));

            if (!symbolTable.containsVariable(variableName)) {
                if (isInsideControlStructure()) {
                    // Inside control structure (local scope) - Allocate a temporary register
                    String register = mipsGenerator.allocateTempRegister();
//                    mipsGenerator.pushToStack(register); // Add to stack for local variable

                    // Add the double variable to the symbol table with the register (local scope)
                    symbolTable.addEntry(variableName, "double", value, "local", register);  // Add to symbol table with value
                    System.out.println("Local double variable with value declared inside control structure: " + variableName + " = " + value);
                } else {
                    // Global variable (add to .data section)
                    mipsGenerator.addToDataSection(variableName, String.valueOf(value), "double");

                    // Add the double literal to the literal table if not already added
                    addDoubleLiteralIfNotExist(value);  // Add the literal value
                    // Add the double variable with the parsed value to the symbol table (global)
                    symbolTable.addEntry(variableName, "double", value, "global", null);  // Add to symbol table with value
                    System.out.println("Global double variable with value declared: " + variableName + " = " + value);
                }
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
                mipsGenerator.addToDataSection(tokens[1], "false", "boolean");
                // Add the boolean variable with default value
                addBooleanLiteralIfNotExist("false");
                symbolTable.addEntry(variableName, "boolean", false, "global", null);  // Default to false
                System.out.println("Not in symbol table... Now added to Symbol Table: " + variableName);
            } else {
                System.out.println("Error: Variable " + variableName + " is already declared.");
            }
        } else if (tokens.length == 5 && tokens[0].equals("boolean") && tokens[2].equals("=")) {
            String variableName = tokens[1];
            boolean value = Boolean.parseBoolean(tokens[3].replace(";", ""));  // Parse boolean value from token
            System.out.println("Checking if variable exists: " + variableName + " => " + symbolTable.containsVariable(variableName));

            if (!symbolTable.containsVariable(variableName)) {
                mipsGenerator.addToDataSection(tokens[1], String.valueOf(value), "boolean");
                // Add the boolean literal to literal table if not already added
                addBooleanLiteralIfNotExist(value ? "true" : "false");
                symbolTable.addEntry(variableName, "boolean", value, "global", null);  // Add boolean value to symbol table
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
            mipsGenerator.addToDataSection(tokens[1], " ", "string");
            symbolTable.addEntry(variableName, type, "", scope, null); // Initialize with an empty string
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
                mipsGenerator.addToDataSection(tokens[1], value, type);

                String scope = "global";  // Default scope (adjust as necessary)
                symbolTable.addEntry(variableName, type, assignedValue, "global", null);
                System.out.println("Assigned string: " + assignedValue + " to variable: " + variableName);
//                System.out.println("Loaded string address into register: " +allocatedRegister);
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
        // Case 1: Handle "integer x = 5;" or "integer a = 2;"
        if (tokens[0].equals("integer")) {
            String variableName = tokens[1]; // The variable on the left-hand side
            String valueToken = tokens[3]; // The value to assign (e.g., "5")

            String scope = isInsideControlStructure() ? "local" : "global";

            // Check if the variable is already declared
            if (!symbolTable.containsVariable(variableName)) {
                String allocatedRegister = mipsGenerator.allocateSavedRegister();
                // Allocate space in the symbol table, but don't add to data section
                symbolTable.addEntry(variableName, "int", 0, scope, allocatedRegister);
                System.out.println("Encountered new symbol " + variableName + " with id " + symbolTable.getIdByName(variableName));
            }

            try {
                int value = Integer.parseInt(valueToken); // Convert the token to an integer

                // No need to store in memory, just update symbol table and work with registers
                String reg = mipsGenerator.allocateTempRegister();
                mipsGenerator.loadImmediate(reg, value); // Load the value into a temporary register
                symbolTable.updateValue(variableName, value); // Update the variable's value in the symbol table (no memory write)

                mipsGenerator.freeRegister(reg); // Free the register after use

                // Print TokenIDs for debugging
                Integer integerTokenID = keywordTable.get("integer");
                Integer assignTokenID = operatorTable.get("=");
                Integer semicolonTokenID = operatorTable.get(";");
                System.out.print("TokenIDs: " + integerTokenID + " " + symbolTable.getIdByName(variableName) + " " + assignTokenID + " " + literalTable.getLiteralID(value) + " " + semicolonTokenID + " ");
                System.out.println();
                System.out.println("Code Generators: " + CodeGenerator.START_DEFINE + " " + CodeGenerator.END_DEFINE);

            } catch (NumberFormatException e) {
                System.out.println("Syntax error: Invalid assignment value.");
            }
        } else {
            // Case 2: Handle assignments with expressions like "sum = a + b + c"
            String variableName = tokens[0]; // The variable on the left-hand side
            String valueExpression = String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length - 1)); // Get the right-hand side expression

            try {
                // Ensure the variable is declared
                if (!symbolTable.containsVariable(variableName)) {
                    String register = mipsGenerator.allocateSavedRegister();
                    String scope = isInsideControlStructure() ? "local" : "global";
                    symbolTable.addEntry(variableName, "int", 0, scope, register); // Declare it if not
                    System.out.println("Encountered new symbol " + variableName + " with id " + symbolTable.getIdByName(variableName));
                }

                // Evaluate the expression to get the value to assign
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
                        symbolTable.updateValue(variableName, (Integer) result); // Update the value in symbol table (no memory write)
                        System.out.println("Assigned integer value: " + result + " to variable: " + variableName);

                        // Add to literal table after computation (no need to store in memory)
                        int literalID = literalTable.addLiteral((Integer) result);
                        System.out.println("Encountered new literal " + result + " with id " + literalID);
                    } else {
                        throw new RuntimeException("Type mismatch: Unsupported value type.");
                    }
                } else {
                    // Handle other types (e.g., double) if needed
                    symbolTable.updateValue(variableName, result); // Update the value in symbol table
                }

                Integer assignTokenID = operatorTable.get("=");
                Integer semicolonTokenID = operatorTable.get(";");

                // Print TokenIDs (example, adjust according to your actual logic)
                System.out.print("TokenIDs: " + symbolTable.getIdByName(variableName) + " " + assignTokenID + " " + literalTable.getLiteralID(result) + " " + semicolonTokenID + " ");
                System.out.println();

                // Add code generators based on the operations performed
                List<CodeGenerator> codeGenerators = new ArrayList<>();

                // Split the expression into operands and operators for arithmetic
                String[] expressionTokens = valueExpression.split(" ");
                for (String token : expressionTokens) {
                    if (token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/")) {
                        // Handle arithmetic operator (no need to store anything in memory)
                        String operand1 = expressionTokens[0]; // First operand
                        String operand2 = expressionTokens[2]; // Second operand
                        // mipsGenerator.generateArithmeticOperation(token, operand1, operand2, variableName);
                    }
                }

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
        System.out.println("Handling while loop with condition: " + condition);
        System.out.println("Block tokens: " + blockTokens);

        // Ensure that blockTokens is not empty
        if (blockTokens.isEmpty()) {
            throw new IllegalArgumentException("Block tokens cannot be empty.");
        }

        // Generate MIPS code once before the loop starts
//        try {
            mipsGenerator.generateWhileLoop(condition, blockTokens);
            System.out.println("MIPS code for while loop generated successfully.");
//        } catch (Exception e) {
//            System.err.println("Error during MIPS generation: " + e.getMessage());
//            throw e;
//        }

        // Logical execution of the loop
        while (true) {
            System.out.println("\nRe-evaluating condition...");

            // Tokenize the condition to break it into individual tokens
            String[] conditionTokensArray = tokenizer.tokenize(condition);
            System.out.println("Condition tokens: " + Arrays.toString(conditionTokensArray));

            // Evaluate the condition
            boolean conditionResult = evaluator.evaluateCondition(conditionTokensArray);
            System.out.println("Condition evaluated to: " + (conditionResult ? "true" : "false"));

            if (!conditionResult) {
                System.out.println("Condition evaluated to false, exiting loop.");
                break; // Exit loop if the condition is false
            }

            // Execute the body of the loop
            StringBuilder statementBuilder = new StringBuilder();
            for (String token : blockTokens) {
                statementBuilder.append(token).append(" ");
            }

            String fullStatement = statementBuilder.toString().trim();
            if (!fullStatement.endsWith(";")) {
                fullStatement += ";";
            }

            System.out.println("Full loop body statement: " + fullStatement);

            // Tokenize the loop body
            String[] loopBodyTokens = tokenizer.tokenize(fullStatement);
            System.out.println("Loop body tokens: " + Arrays.toString(loopBodyTokens));

            // Check if loop body tokens are empty or invalid
            if (loopBodyTokens.length == 0) {
                throw new IllegalArgumentException("Loop body tokens cannot be empty.");
            }

            // Execute the loop body commands
            try {
                executeCommand(loopBodyTokens);
                System.out.println("Loop body executed successfully.");
            } catch (Exception e) {
                System.err.println("Error during loop body execution: " + e.getMessage());
                break; // Break out of the loop if execution fails
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

    public static void handleIfElse(String[] tokens, List<Integer> tokenIDs) throws Exception {
        System.out.println("Entered handleIfElse...");

        // Ensure the first token is 'if'
        if (!tokens[0].trim().equals("if")) {
            throw new Exception("Expected 'if' at the start of the if-else block, but found: " + tokens[0].trim());
        }

        // Validate and extract the condition
        int startCondition = findIndex(tokens, "(");
        int endCondition = findIndex(tokens, ")");
        if (startCondition < 0 || endCondition < 0 || endCondition <= startCondition) {
            throw new Exception("Invalid if condition syntax: missing or misplaced parentheses");
        }

        // Extract tokens for the condition
        String[] conditionTokens = Arrays.copyOfRange(tokens, startCondition + 1, endCondition);

        // Generate labels for the MIPS code
        String ifLabel = mipsGenerator.generateLabel("IF_BLOCK");
        String elseLabel = mipsGenerator.generateLabel("ELSE_BLOCK");
        String endLabel = mipsGenerator.generateLabel("END_IF_ELSE");

        // Evaluate the condition
        boolean conditionResult;
        try {
            conditionResult = evaluator.evaluateCondition(conditionTokens);
            System.out.println("Condition evaluated successfully: " + conditionResult);
        } catch (Exception e) {
            System.err.println("Exception during condition evaluation: " + e.getMessage());
            e.printStackTrace();
            return; // or handle the exception as appropriate
        }

        // Extract tokens for 'if' block
        String[] ifTokens = extractBlock(tokens, "if");

        // Extract tokens for 'else' block if it exists
        String[] elseTokens = new String[0];
        int elseIndex = findNextToken(tokens, "else", findMatchingBrace(tokens, findIndex(tokens, ")")) + 1);
        if (elseIndex >= 0) {
            int openElseBrace = findNextToken(tokens, "{", elseIndex);
            int closeElseBrace = findMatchingBrace(tokens, openElseBrace);
            if (openElseBrace < 0 || closeElseBrace < 0) {
                throw new Exception("Invalid else block structure: Missing braces");
            }
            elseTokens = Arrays.copyOfRange(tokens, openElseBrace + 1, closeElseBrace);
            System.out.println("Extracted elseTokens: " + Arrays.toString(elseTokens));
        }

        // Execute the 'if' or 'else' block based on the condition result
        if (conditionResult) {
            System.out.println("Executing If block...");
            mipsGenerator.addLabel(ifLabel);
            mipsGenerator.addComment("If block start");
            processBlock(ifTokens, 0, ifTokens.length - 1);
        } else if (elseTokens.length > 0) {
            System.out.println("Executing Else block...");
            mipsGenerator.addLabel(elseLabel);
            mipsGenerator.addComment("Else block start");
            processBlock(elseTokens, 0, elseTokens.length - 1);
        }

        // Add the end label
        mipsGenerator.addLabel(endLabel);
        System.out.println("If-Else MIPS Code Generation Complete");
    }

    private static String[] extractBlock(String[] tokens, String blockType) throws Exception {
        int startBrace = findNextToken(tokens, "{", 0);
        int endBrace = findMatchingBrace(tokens, startBrace);

        if (startBrace < 0 || endBrace < 0) {
            throw new Exception("Invalid " + blockType + " block structure: Missing braces");
        }

        return Arrays.copyOfRange(tokens, startBrace + 1, endBrace);
    }

    private static int findMatchingBrace(String[] tokens, int start) {
        int braceCount = 1;
        System.out.println("Finding matching brace starting at index: " + start);

        for (int i = start + 1; i < tokens.length; i++) {
            System.out.println("Checking token at index " + i + ": " + tokens[i]);
            if (tokens[i].equals("{")) {
                braceCount++;
            } else if (tokens[i].equals("}")) {
                braceCount--;
                System.out.println("Brace count: " + braceCount);
                if (braceCount == 0) {
                    System.out.println("Found matching brace at index: " + i);
                    return i; // Matching closing brace found
                }
            }
        }

        System.out.println("No matching brace found");
        return -1;
    }


    private static int findIndex(String[] tokens, String token) {
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].trim().equals(token)) {
                return i;
            }
        }
        return -1; // Token not found
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
    private static void processBlock(String[] tokens, int startBlock, int endBlock) throws Exception {
        System.out.println("Processing block from " +startBlock+ " to " +endBlock);

        // This method processes a block of commands
        int currentTokenStart = startBlock;
        System.out.println("CurrentTokenStart: " +currentTokenStart);
        for (int i = startBlock; i < endBlock+1; i++) {
            System.out.println("Token at index " +i+ ": " +tokens[i]);
            if (tokens[i].trim().equals(";")) {
                // Extract the command tokens from start to semicolon
                String[] commandTokens = Arrays.copyOfRange(tokens, currentTokenStart, i + 1);
                System.out.println("Executing command: " + Arrays.toString(commandTokens));
                executeCommand(commandTokens); // Execute the command
                currentTokenStart = i + 1; // Move to the next command
            }
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
            String reg = mipsGenerator.allocateTempRegister();
            symbolTable.addEntry(loopVar, "int", initValue, "global", reg);
//            String offset = symbolTable.getOffsetByName(loopVar);
        }

        // Step 5: Parse the condition
        String[] conditionTokens = condition.split(" ");

        List<String> tokensList = Arrays.asList(loopTokens);
        int start = tokensList.indexOf("{");
        int end = tokensList.lastIndexOf("}");
        String[] bodyTokens = tokensList.subList(start + 1, end).toArray(new String[0]);

        mipsGenerator.generateForLoop(initialization, condition, increment, Arrays.asList(bodyTokens));

        // Start the loop, continue to use the same registers
        boolean conditionResult = evaluator.evaluateCondition(conditionTokens);
        while (conditionResult) {
            // Debugging: Check the value of 'i' before executing the loop body
            System.out.println("Before loop body: i = " + symbolTable.getValueById(symbolTable.getIdByName(loopVar)));

            // Execute the loop body
            executeLoopBody(bodyTokens);

            // Handle the increment or decrement operation (e.g., i++, i--)

            // Reuse the same registers for incrementing
            if (increment.contains("++")) {
                evaluator.evaluateIncrementOrDecrement("++", loopVar);
            } else if (increment.contains("--")) {
                evaluator.evaluateIncrementOrDecrement("--", loopVar);
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
            String reg = mipsGenerator.allocateTempRegister();
            symbolTable.addEntry(variableName, "int", startValue, "global", reg);
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

    private static boolean isInsideControlStructure(){
        return controlStructure > 0;
    }

    private static void enterControlStructure(){
        controlStructure++;
    }

    private static void exitControlStructure(){
        if(controlStructure > 0){
            controlStructure--;
        }else{
            System.out.println("Error: Trying to exit a control structure when none are active.");
        }
    }

    private static String loadVariableIfNeeded(String variableName) {
        if (!symbolTable.containsVariable(variableName)) {
            throw new RuntimeException("Variable '" + variableName + "' is not declared.");
        }

        SymbolTable.Entry entry = symbolTable.getEntry(variableName);

        if (entry.getScope().equals("global")) {
            // For global variables, load from .data section
            String reg = mipsGenerator.allocateTempRegister();
            mipsGenerator.loadFromData(variableName, reg);  // Custom method for global variables
            return reg;
        } else if (entry.getScope().equals("local")) {
            // For local variables, load from the stack using your method
            String reg = mipsGenerator.allocateTempRegister();
            mipsGenerator.loadVariable(variableName, reg);  // Using your loadVariable method
            return reg;
        } else {
            throw new RuntimeException("Unknown variable scope for '" + variableName + "'.");
        }
    }

}
