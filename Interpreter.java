import java.util.*;

public class Interpreter {
    private static final String KEYWORD_INTEGER = "integer";
    private static final String KEYWORD_INPUT = "input";
    private static final String KEYWORD_PRINT = "print";
    private static final String KEYWORD_IF = "if";
    private static final String KEYWORD_ELSE = "else";

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
        List<String> tokenID = Arrays.asList(tokens);
        List<Integer> id = new ArrayList<>();

        for(String t : tokenID){
            id.add(interpreter.getTokenID(t));
        }

        //handle the new 'if-else' structure
        if(tokens[0].equals("if")){
            handleIfElse(tokenID, id);
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

    public static void handleIfElse(List<String> tokens, List<Integer> tokenIDs) {
        // Validate the if-else structure
        validateIfElseStructure(tokens, tokenIDs);

        int ifIndex = tokens.indexOf(KEYWORD_IF);
        int elseIndex = tokens.indexOf(KEYWORD_ELSE);

        // Extract the condition tokens and their respective TokenIDs
        List<String> conditionTokens = extractConditionTokens(tokens, ifIndex, elseIndex);
        List<Integer> conditionTokenIDs = extractConditionTokenIDs(tokens, ifIndex, elseIndex);

        // Create new lists for tokens and tokenIDs, including the "if" keyword and its token ID
        List<String> modifiedTokens = new ArrayList<>(tokens);  // Make a copy of the original tokens
        List<Integer> modifiedTokenIDs = new ArrayList<>(tokenIDs);  // Make a copy of the original tokenIDs

        // Add the KEYWORD_IF token before the condition
        modifiedTokens.add(ifIndex, KEYWORD_IF);  // Inserting "if" keyword at the start of the condition
        modifiedTokenIDs.add(ifIndex, 103); // Token ID for "if" (103)


        // Print condition TokenIDs for debugging
        System.out.print("Condition TokenIDs: ");
        printTokenIDs(conditionTokenIDs);

        // Create Evaluator to evaluate the condition, using both tokens and tokenIDs
        Evaluator evaluator = new Evaluator(symbolTable);  // Assuming Evaluator can handle both tables
        boolean conditionResult = evaluator.evaluateCondition(conditionTokens, conditionTokenIDs);

        // Handle the if block
        List<String> ifTokens;
        List<Integer> ifTokenIDs;

        // If no `else` exists, process until the end of the tokens list
        if (elseIndex == -1) {
            ifTokens = modifiedTokens.subList(ifIndex + 1, modifiedTokens.size());
            ifTokenIDs = modifiedTokenIDs.subList(ifIndex + 1, modifiedTokenIDs.size());
        } else {
            // Otherwise, process until the `else` token
            ifTokens = modifiedTokens.subList(ifIndex + 1, elseIndex);
            ifTokenIDs = modifiedTokenIDs.subList(ifIndex + 1, elseIndex);
        }

        // Execute the appropriate block based on the evaluated condition
        if (conditionResult) {
            executeBlock(ifTokens, ifTokenIDs);
        } else {
            // Handle else block if exists
            if (elseIndex != -1) {
                List<String> elseTokens = modifiedTokens.subList(elseIndex + 1, modifiedTokens.size());
                List<Integer> elseTokenIDs = modifiedTokenIDs.subList(elseIndex + 1, modifiedTokenIDs.size());

                // Execute the else block
                executeBlock(elseTokens, elseTokenIDs);
            }
        }
    }


    public static void executeBlock(List<String> blockTokens, List<Integer> blockTokenIDs) {
        // Logic to execute the block
        System.out.print("Executing Block: ");
        for (String token : blockTokens) {
            System.out.print(token + " ");
        }
        System.out.println();
    }

    private int getTokenID(String token) {
        // This method should return the correct token ID for each token
        if (token.equals("if")) {
            return TOKEN_IDS.get("if");
        } else if (token.equals("else")) {
            return TOKEN_IDS.get("else");
        } else if (token.equals("=")) {
            return TOKEN_IDS.get("=");
        }
        // Add more token mappings as needed
        return -1; // Default case if token is not found
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

    public static List<String> extractConditionTokens(List<String> tokens, int ifIndex, int elseIndex) {
        if (elseIndex == -1) {
            // Extract condition from after "if" until the end of the tokens list
            return tokens.subList(ifIndex + 1, tokens.size());
        } else {
            // Extract condition between "if" and "else"
            return tokens.subList(ifIndex + 1, elseIndex);
        }
    }

    // This method processes the condition tokens and maps them to TokenIDs
    private static List<Integer> extractConditionTokenIDs(List<String> tokens, int ifIndex, int elseIndex) {
        List<Integer> conditionTokenIDs = new ArrayList<>();
        List<String> conditionTokens = extractConditionTokens(tokens, ifIndex, elseIndex);

        for (String token : conditionTokens) {
            if (isPredefinedToken(token)) {
                // If token is predefined, use its corresponding TokenID
                conditionTokenIDs.add(TOKEN_IDS.get(token));
            } else {
                // Otherwise, this could be a variable or literal; retrieve its value
                conditionTokenIDs.add(getTokenIDForVariableOrLiteral(token));
            }
        }
        return conditionTokenIDs;
    }

    private static int getTokenIDForVariableOrLiteral(String token) {
        // Retrieve the token ID for a variable or literal (this part depends on your symbol/literal table structure)
        if (symbolTable.containsVariable(token)) {
            return symbolTable.getId(token);  // Assuming method returns ID of the variable
        } else {
            return literalTable.getLiteralID(token);  // Assuming method returns ID of the literal
        }
    }

    private static boolean isPredefinedToken(String token) {
        return TOKEN_IDS.containsKey(token);
    }

    public static void printTokenIDs(List<Integer> tokenIDs) {
        for (Integer id : tokenIDs) {
            System.out.print(id + " ");
        }
        System.out.println();
    }


}
