import java.util.*;

public class MIPSGenerator {
    private Deque<String> tempRegisters;
    private Deque<String> savedRegisters;
    private Deque<String> tempFloatRegisters;
    private Deque<String> savedFloatRegisters;
    private Set<String> usedRegisters;
    private Deque<String> freeRegisters;
    private static List<String> mipsCode;  // List to store MIPS instructions
    private static int labelCounter = 0;
    private int stackPointer = 0x7fffe000;
    private Map<String, Integer> stackMap;
    private int stackOffset = -4;
    private Map<String, String> dataSection = new HashMap<>();
    private Set<String> usedSavedRegisters;
    private static SymbolTable symbolTable;
    private Queue<String> availableRegisters = new LinkedList<>(Arrays.asList("$t0", "$t1", "$t2", "$t3", "$t4"));


    public MIPSGenerator(SymbolTable symbolTable) {
        // Initialize register pools
        tempRegisters = new ArrayDeque<>(List.of("$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9"));
        savedRegisters = new ArrayDeque<>(List.of("$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7"));
        tempFloatRegisters = new ArrayDeque<>(List.of("$f0", "$f2", "$f4", "$f6", "$f8", "$f10", "$f12", "$f14", "$f16", "$f18"));
        savedFloatRegisters = new ArrayDeque<>(List.of("$f20", "$f22", "$f24", "$f26", "$f28", "$f30"));
        usedRegisters = new HashSet<>();
        freeRegisters = new ArrayDeque<>(List.of("$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9"));
        mipsCode = new ArrayList<>();  // Initialize the list to store MIPS code
        stackMap = new HashMap<>();
        stackPointer = 0;
        usedSavedRegisters = new HashSet<>();
        this.symbolTable = symbolTable;
    }


    public void addToDataSection(String variableName, String initValue, String dataType) {
        // Prevent duplicates
        if (!dataSection.containsKey(variableName)) {
            switch (dataType.toLowerCase()) {
                case "int":
                    dataSection.put(variableName, variableName + ": .word " + initValue);
                    break;
                case "string":
                    dataSection.put(variableName, variableName + ": .asciiz \"" + initValue + "\"");
                    break;
                case "boolean":
                    int boolValue = Boolean.parseBoolean(initValue) ? 1 : 0;
                    dataSection.put(variableName, variableName + ": .word " + boolValue);
                    break;
                case "double":
                    dataSection.put(variableName, variableName + ": .double " + initValue);
                    break;
                default:
                    System.out.println("Unknown data type: " + dataType);
                    break;
            }
        } else {
            System.out.println("Variable " + variableName + " already exists in the .data section.");
        }
    }

    //want to push one thing to the stack
    public void pushToStack(String register){
        addMipsInstruction("addi $sp, $sp, -4");
        addMipsInstruction("sw " +register+ ", 0($sp)");
    }

    public void popFromStack(String register){
        addMipsInstruction("lw " +register+ ", " +stackOffset+ "($sp)");
        addMipsInstruction("addi $sp, $sp, 4");
        stackOffset += 4;
    }

    // Allocate a variable on the stack
    public void allocateVariable(String variableName) {
        stackPointer -= 4;  // Move stack pointer for variable allocation
        stackMap.put(variableName, stackPointer); // Store the offset for the variable
        addMipsInstruction("# Allocating variable: " + variableName + " at offset " + stackPointer);
    }


    // Method to deallocate a variable and free the associated memory
    public void deallocateVariable(String variableName) {
        if (stackMap.containsKey(variableName)) {
            // Get the stack offset for the variable
            int offset = stackMap.get(variableName);

            // Generate MIPS instruction to free the variable's memory
            addMipsInstruction("# Deallocating variable: " + variableName + " at offset " + offset);

            // Remove the variable from the stack map
            stackMap.remove(variableName);
        } else {
            System.out.println("Variable " + variableName + " not found in the stack.");
        }
    }


    public void storeVariable(String variableName, String register){
        if(!stackMap.containsKey(variableName)){
            allocateVariable(variableName);
        }
        int offset = stackMap.get(variableName);
        addMipsInstruction("sw " +register+ ", " +offset+ "($sp)");
    }

    public void loadVariable(String variableName, String register){
        if(!stackMap.containsKey(variableName)){
            throw new IllegalArgumentException("Variable " +variableName+ " is not allocated on the stack");
        }
        int offset = stackMap.get(variableName);
        addMipsInstruction("lw " +register+ ", " +offset+"($sp)");
    }

    // Method to push only used saved registers onto the stack
    public void saveUsedSavedRegisters() {
        for (String reg : usedSavedRegisters) {
            addMipsInstruction("addi $sp, $sp, -4");  // Adjust the stack pointer
            addMipsInstruction("sw " + reg + ", 0($sp)");  // Store the register value to the stack
            addMipsInstruction("# Saved register " + reg + " to stack");
        }
    }

    // Method to pop only used saved registers from the stack
    public void restoreUsedSavedRegisters() {
        for (String reg : usedSavedRegisters) {
            addMipsInstruction("lw " + reg + ", 0($sp)");  // Load the register value from the stack
            addMipsInstruction("addi $sp, $sp, 4");  // Restore the stack pointer
            addMipsInstruction("# Restored register " + reg + " from stack");
        }
    }

    // Method to allocate a temporary register
    public String allocateTempRegister() {
        if (freeRegisters.isEmpty()) {
            addMipsInstruction("# No available temporary registers, resetting register pool.");
            resetRegisterPools();
        }

        if(freeRegisters.isEmpty()){
            throw  new RuntimeException("No available temporary registers even after reset.");
        }

        String reg = freeRegisters.poll(); // Get a free register
        usedRegisters.add(reg);            // Mark the register as used
        addMipsInstruction("# Allocating temporary register: " + reg);
//        printRegisterState();
        return reg;
    }

    // Method to allocate a saved register
    public String allocateSavedRegister() {
        for (String reg : savedRegisters) {
            if (!usedRegisters.contains(reg)) {
                usedRegisters.add(reg);
                usedSavedRegisters.add(reg);
                addMipsInstruction("# Allocating saved register: " + reg);
                return reg;
            }
        }

        addMipsInstruction("# No available saved registers, resetting register pool.");
        resetRegisterPools();

        for (String reg : savedRegisters) {
            if (!usedRegisters.contains(reg)) {
                usedRegisters.add(reg);
                usedSavedRegisters.add(reg);
                addMipsInstruction("# Allocating saved register after reset: " + reg);
                return reg;
            }
        }

        throw new RuntimeException("No available saved registers even after resetting.");
    }

    // Method to free a register
    public void freeRegister(String reg) {
        if (usedRegisters.contains(reg)) {
            usedRegisters.remove(reg);
            if(tempRegisters.contains(reg) && !freeRegisters.contains(reg)){
                freeRegisters.offerFirst(reg);
            }

            if(savedRegisters.contains(reg)){
                usedSavedRegisters.remove(reg);
            }

            addMipsInstruction("# Register freed: " + reg);
//            printRegisterState();
        } else {
            addMipsInstruction("# Warning: Attempted to free an unallocated register: " + reg);
        }
    }

    public void resetRegisterPools() {
        // Reset the register pools to their initial states
        tempRegisters = new ArrayDeque<>(List.of("$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9"));
        usedRegisters.clear();  // Clear the set of used registers
        freeRegisters = new ArrayDeque<>(tempRegisters);  // Reinitialize free registers from tempRegisters
        usedSavedRegisters.clear();

        // Optionally log or output a message for debugging
        addMipsInstruction("# Reset all register pools");
//        printRegisterState();
    }


    // Method to load a register with an operand
    public void loadRegister(String register, Object operand) {
        if (operand == null) {
            throw new IllegalArgumentException("Operand cannot be null");
        }

        if (operand instanceof Integer) {
            // If the operand is an integer, load it directly
            addMipsInstruction("li " + register + ", " + operand);  // li = Load immediate
        } else if (operand instanceof Double) {
            System.out.println("Double operand: " +operand);
            // If the operand is a double, load it directly from the data section
            // Assuming the double value is already stored in the data section
            String label = generateUniqueLabelForDouble((Double) operand);
            addMipsInstruction("l.d " + register + ", " + label);  // Load double into target register
        } else if (operand instanceof Boolean) {
            // If the operand is a boolean (true/false)
            addMipsInstruction("li " + register + ", " + ((Boolean) operand ? "1" : "0"));
        } else if (operand instanceof String) {
            // If the operand is a string (assuming it is a label)
            String addressRegister = allocateTempRegister();
            addMipsInstruction("la " + addressRegister + ", " + operand);  // Load address of the string
            addMipsInstruction("lw " + register + ", " + stackOffset + "(" + addressRegister + ")");  // Load string address into register
            freeRegister(addressRegister);
        } else {
            throw new IllegalArgumentException("Unsupported operand type: " + operand.getClass().getSimpleName());
        }
    }

    private String generateUniqueLabelForDouble(double value){
        return "double_" + value;
    }

    public static void loadImmediate(String reg, int value){
        addMipsInstruction("li " +reg+ ", " +value);
    }

    public static void storeToMemory(String variableName, String reg){
        String memoryLocation = getMemoryLocation(variableName);
        addMipsInstruction("sw " +reg+ ", " +memoryLocation);
    }

    private static String getMemoryLocation(String variableName) {
        // A placeholder that always returns the same memory location
        return "0($sp)"; // For example, this could point to the stack location at the top of the stack
    }

    public void generateAssignment(String variable, String expression) {
        String reg = allocateSavedRegister(); // Allocate a saved register for the variable

        // Assuming `evaluateExpression` generates MIPS code for the expression
        String resultRegister = evaluateExpression(expression);

        // Store the result in the allocated register
        addMipsInstruction("# Assigning result of expression to " + variable);
        addMipsInstruction("move " + reg + ", " + resultRegister);

        // Update the register for the variable in the symbol table
        symbolTable.addRegisterToVariable(variable, reg);

        // Free the temporary register if no longer needed
        freeRegister(resultRegister);
    }


    // Example evaluateExpression method (simplified)
    private String evaluateExpression(String expression) {
        // In a real implementation, this would generate MIPS code for the expression
        // Here we simulate it with a temporary register allocation
        String tempReg = allocateTempRegister();
        addMipsInstruction("# Evaluating expression: " + expression + " into " + tempReg);
        return tempReg;
    }


    public String generateConditional(String operator, Object operand1, Object operand2, String labelTrue, String labelFalse) {
        String reg1 = allocateTempRegister();  // Load operand1
        String reg2 = allocateTempRegister();  // Load operand2

        loadRegister(reg1, operand1);  // Load operand1 value
        loadRegister(reg2, operand2);  // Load operand2 value

        // Conditional checks for boolean or numeric operands
        if (operand1 instanceof Boolean || operand2 instanceof Boolean) {
            // Handle boolean conditionals (true/false)
            switch (operator) {
                case "==":
                    addMipsInstruction("beq " + reg1 + ", " + reg2 + ", " +labelTrue);
//                    addMipsInstruction("j " +labelFalse);
                    break;
                case "!=":
                    addMipsInstruction("bne " + reg1 + ", " + reg2 + ", " +labelTrue);
//                    addMipsInstruction("j " +labelFalse);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported boolean operator: " + operator);
            }
        } else if (operand1 instanceof Integer || operand1 instanceof Double) {
            // Handle integer or floating-point conditionals
            switch (operator) {
                case "==":
                    addMipsInstruction("beq " + reg1 + ", " + reg2 + ", " +labelTrue);
//                    addMipsInstruction("j " +labelFalse);
                    break;
                case "!=":
                    addMipsInstruction("bne " + reg1 + ", " + reg2 + ", " +labelTrue);
//                    addMipsInstruction("j " +labelFalse);
                    break;
                case "<":
                    addMipsInstruction("blt " + reg1 + ", " + reg2 + ", " +labelTrue);
//                    addMipsInstruction("j " +labelFalse);
                    break;
                case ">":
                    addMipsInstruction("bgt " + reg1 + ", " + reg2 + ", " +labelTrue);
//                    addMipsInstruction("j " +labelFalse);
                    break;
                case "<=":
                    addMipsInstruction("ble " + reg1 + ", " + reg2 + ", " +labelTrue);
//                    addMipsInstruction("j " +labelFalse);
                    break;
                case ">=":
                    addMipsInstruction("bge " + reg1 + ", " + reg2 + ", " +labelTrue);
//                    addMipsInstruction("j " +labelFalse);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported comparison operator: " + operator);
            }
        } else {
            throw new IllegalArgumentException("Invalid operand types for conditional comparison");
        }

        freeRegister(reg1);
        freeRegister(reg2);

        return mipsCode.toString();
    }

    public String convertConditionToMips(String condition, String loopVarRegister, SymbolTable symbolTable) {
        // Split the condition into the operator and the value
        String[] parts = condition.split("\\s*([<=>!]=?|==)\\s*");
        String leftOperand = parts[0].trim(); // This should be the loop variable (e.g., "i")
        String operator = condition.replaceAll("[^<=>!]+", "").trim(); // Extract operators like <, >, ==, etc.
        String rightOperand = parts[1].trim(); // This is the value (e.g., "5")

        System.out.println("Left operand: " + leftOperand + ", Operator: " + operator + ", Right operand: " + rightOperand);

        // Retrieve the register for the left operand (loop variable) from the SymbolTable
        String leftOperandRegister = symbolTable.getRegister(leftOperand);
        if (leftOperandRegister == null) {
            throw new IllegalArgumentException("The variable '" + leftOperand + "' does not have a register in the SymbolTable.");
        }

        // Determine the comparison value (whether it's a variable, integer, or double)
        String comparisonValue;
        if (rightOperand.matches("\\d+")) { // If it's an integer literal
            comparisonValue = rightOperand;
        } else if (rightOperand.matches("\\d*\\.\\d+")) { // If it's a double literal
            comparisonValue = rightOperand;
        } else {
            comparisonValue = "$" + rightOperand; // If it's a variable, use its register
        }

        // Generate MIPS based on the operator
        String mipsInstruction = "";
        switch (operator) {
            case "<":
                mipsInstruction = "blt " + leftOperandRegister + ", " + comparisonValue + ", label_9"; // Branch if less than
                break;
            case ">":
                mipsInstruction = "bgt " + leftOperandRegister + ", " + comparisonValue + ", label_9"; // Branch if greater than
                break;
            case "<=":
                mipsInstruction = "ble " + leftOperandRegister + ", " + comparisonValue + ", label_9"; // Branch if less than or equal
                break;
            case ">=":
                mipsInstruction = "bge " + leftOperandRegister + ", " + comparisonValue + ", label_9"; // Branch if greater than or equal
                break;
            case "==":
                mipsInstruction = "beq " + leftOperandRegister + ", " + comparisonValue + ", label_9"; // Branch if equal
                break;
            case "!=":
                mipsInstruction = "bne " + leftOperandRegister + ", " + comparisonValue + ", label_9"; // Branch if not equal
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }

        System.out.println("Generated MIPS instruction: " + mipsInstruction);
        return mipsInstruction;
    }


    public void generateIfElse(String condition, List<String> bodyTokens) {
        String labelTrue = generateLabel();
        String labelFalse = generateLabel();
        String labelEnd = generateLabel();

        addMipsInstruction("bnez " + condition + ", " + labelTrue);  // Branch if condition is true
        addMipsInstruction("j " + labelFalse);  // Else part
        addMipsInstruction(labelTrue + ":");

        // Process body of the if part
        for (String bodyToken : bodyTokens) {
            processBodyToken(bodyToken);
        }

        addMipsInstruction("j " + labelEnd);  // Jump to end
        addMipsInstruction(labelFalse + ":");

        // Handle else part (if any)
        addMipsInstruction(labelEnd + ":");
    }


    // Generate MIPS code for a 'while' loop
    // Method to generate MIPS code for a while loop
    public void generateWhileLoop(String condition, List<String> bodyTokens) {
        String labelStart = generateLabel();  // Generate labels for the loop
        String labelEnd = generateLabel();

        // Generate MIPS code for the loop start
        addMipsInstruction(labelStart + ":");
        addMipsInstruction("bnez " + condition + ", " + labelEnd);  // Exit loop if condition is false

        // Process the body of the loop
        for (String bodyToken : bodyTokens) {
            processBodyToken(bodyToken);
        }

        // Jump back to the start of the loop
        addMipsInstruction("j " + labelStart);

        // End of loop label
        addMipsInstruction(labelEnd + ":");
    }

    public String assignRegister(String variable) {
        if (availableRegisters.isEmpty()) {
            throw new RuntimeException("No available registers for variable: " + variable);
        }

        // Assign the next available register
        String register = availableRegisters.poll();

        // Add the register to the variable in the symbol table
        symbolTable.addRegisterToVariable(variable, register);

        // Initialize the variable in the register (e.g., load its initial value)
        addMipsInstruction("li " + register + ", 0"); // Default initialization
        return register;
    }


    // Method to generate the MIPS code for printing an integer (value in $a0)
    public void generatePrint(String register) {
        if (register != null) {
            addMipsInstruction("li $v0, 1");  // Load syscall number for print integer
            addMipsInstruction("move $a0, " + register);  // Move the value (register) to $a0
            addMipsInstruction("syscall");  // Perform the syscall to print the value
        } else {
            System.out.println("No valid register for print statement.");
        }
    }


    // Method to generate the MIPS code for incrementing a variable (value in $t0)
    public void generateIncrement(String variable) {
        // Check if the variable is already assigned a register
        String register = symbolTable.getRegister(variable);

        if (register == null) {
            // If the variable doesn't have a register, assign one
            register = allocateTempRegister();
        }

        // Generate MIPS instructions to increment the value in the register
        addMipsInstruction("# Increment variable " + variable);
        addMipsInstruction("addi " + register + ", " + register + ", 1");
    }


    // Method to handle the entire for loop with the print and increment functionality
    public void generateForLoop(String initialization, String condition, String increment, List<String> bodyTokens) {
        String loopVar = initialization.split("=")[0].trim();
        String initialValue = initialization.split("=")[1].trim();
        String register = assignRegister(loopVar);

        symbolTable.addRegisterToVariable(loopVar, register);

        addMipsInstruction("li " + register + ", " + initialValue);
        addMipsInstruction("label_8:");

        // Condition check
        addComment("Check condition for " + loopVar);
        String mipsCondition = convertConditionToMips(condition, register, symbolTable);
        addMipsInstruction(mipsCondition); // Add the MIPS condition here

        // Process the body of the loop
        for (int i = 0; i < bodyTokens.size(); i++) {
            String bodyToken = bodyTokens.get(i).trim();

            if (bodyToken.equals("print")) {
                // Check for the expected format: print ( variable )
                if (i + 2 < bodyTokens.size() && bodyTokens.get(i + 1).equals("(") && bodyTokens.get(i + 3).equals(")")) {
                    String value = bodyTokens.get(i + 2).trim(); // Variable inside parentheses

                    if (value.isEmpty()) {
                        System.out.println("Error: Malformed print statement near index " + i);
                        continue;
                    }

                    // Retrieve the register associated with the variable
                    String printRegister = symbolTable.getRegisterForVariable(value);

                    if (printRegister == null) {
                        System.out.println("Error: No register found for variable '" + value + "' in print statement.");
                    } else {
                        generatePrint(printRegister);
                    }

                    // Skip the processed tokens: 'print', '(', variable, ')'
                    i += 3;
                } else {
                    System.out.println("Error: Malformed print statement near index " + i);
                }
            } else {
                processBodyToken(bodyToken); // Process other tokens
            }
        }

        // Increment the loop variable
        generateIncrement(loopVar);

        // Jump back to the start of the loop
        addMipsInstruction("j label_8");

        // End of the loop
        addMipsInstruction("label_9:");
    }

    private void processBodyToken(String bodyToken) {
        bodyToken = bodyToken.trim();
        System.out.println("Processing body token: '" + bodyToken + "'");

        if(bodyToken.equals(";")){
            return;
        }else if (bodyToken.equals("print")) {
            // Edge case: print keyword without parentheses
            System.out.println("Invalid print statement: Missing parentheses or argument.");
        } else if (bodyToken.matches("int\\s+\\w+\\s*=\\s*\\d+")) {
            // Handle variable declarations
            String[] parts = bodyToken.split("\\s+");
            String variableName = parts[1];
            String value = parts[3];
            declareVariable(variableName, value);

        } else if (bodyToken.matches("\\w+\\s*=\\s*.+")) {
            // Handle assignments and arithmetic expressions
            String variableName = bodyToken.split("=")[0].trim();
            String expression = bodyToken.split("=")[1].trim();
            generateAssignment(variableName, expression);

        } else if (bodyToken.startsWith("if")) {
            // Handle if-else
            String condition = extractCondition(bodyToken);
            List<String> bodyTokens = extractBodyTokens(bodyToken);
            generateIfElse(condition, bodyTokens);

        } else if (bodyToken.startsWith("while")) {
            // Handle while loops
            String condition = extractCondition(bodyToken);
            List<String> bodyTokens = extractBodyTokens(bodyToken);
            generateWhileLoop(condition, bodyTokens);

        } else {
            throw new IllegalArgumentException("Unrecognized body token: " + bodyToken);
        }
    }
    public String getRegisterForExpression(String value) {
        System.out.println("Getting register for expression: " + value);  // Debug print

        if (value.matches("\\d+")) { // Check if it's a number
            return "$" + value;  // Return it as an immediate value (e.g., $5)
        } else {
            String variableName = value.trim();
            System.out.println("Looking up register for variable: " + variableName);  // Debug print

            // For variables, lookup the register from the symbol table
            String register = symbolTable.getRegisterForVariable(variableName);
            if (register != null) {
                return register;
            } else {
                System.out.println("No register found for variable: " + variableName);  // Debug print
                return null;
            }
        }
    }


    // Extract condition from an 'if' or 'while' statement
    private String extractCondition(String token) {
        int start = token.indexOf("(") + 1;
        int end = token.indexOf(")");
        if (start == -1 || end == -1) {
            throw new IllegalArgumentException("Condition not found in token: " + token);
        }
        return token.substring(start, end).trim();
    }

    // Extract body (statements inside curly braces) from an 'if' or 'while' statement
    private List<String> extractBodyTokens(String token) {
        int start = token.indexOf("{") + 1;
        int end = token.indexOf("}");
        if (start == -1 || end == -1) {
            throw new IllegalArgumentException("Body not found in token: " + token);
        }
        String body = token.substring(start, end).trim();
        return Arrays.asList(body.split(";")); // Assuming each statement in the body is separated by semicolons
    }

    public void declareVariable(String variableName, String value){
        String reg = symbolTable.getRegister(variableName);

        addMipsInstruction("li " +reg+ ", " +value);
    }


    private String generateLabel() {
        return "label_" + labelCounter++;  // Increment and return a unique label
    }

    // Add an instruction to the list of generated MIPS code
    public static void addMipsInstruction(String instruction) {
        mipsCode.add(instruction);
    }

    public String generateLabel(String baseName){
        return baseName + "_" +labelCounter++;
    }

    public void addLabel(String label){
        mipsCode.add(label+ ":");
    }

    public void addComment(String comment){
        mipsCode.add("# " +comment);
    }

    public void loadFromData(String variableName, String register){
        addMipsInstruction("lw " +register+ ", " +variableName+ "($gp)");
    }

    public void generateDataSection(){
        System.out.println();
        System.out.println(".data");
        for(String entry : dataSection.values()){
            System.out.println(entry);
        }
    }

    // Print the generated MIPS code
    public void printMipsCode() {
        System.out.println();
        System.out.println(".main");
        for (String instruction : mipsCode) {
            System.out.println(instruction);
        }
    }
}
