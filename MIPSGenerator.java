import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private int registerCounter = 0;
    private int currentRegister = 0;

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

    private String generateUniqueLabel(String base) {
        return base + "_" + (labelCounter++);
    }

    public void addToDataSection(String variableName, String initValue, String dataType) {
        // Check if the variable is already in the data section
        if (!dataSection.containsKey(variableName)) {
            // If initValue contains an '=' sign, it's an assignment (e.g., "x = 5")
            if (initValue.contains("=")) {
                // Extract the value after '='
                String[] parts = initValue.split("=");
                initValue = parts[1].trim().replace(";", ""); // Remove any trailing semicolon
            }

            // Handle different data types and provide default initialization if necessary
            if (initValue.isEmpty()) {
                switch (dataType.toLowerCase()) {
                    case "int":
                        initValue = "0"; // Default to 0 for int
                        break;
                    case "string":
                        initValue = "\"\""; // Default to empty string for string
                        break;
                    case "boolean":
                        initValue = "0"; // Default to false (0) for boolean
                        break;
                    case "double":
                        initValue = "0.0"; // Default to 0.0 for double
                        break;
                    default:
                        System.out.println("Unknown data type: " + dataType);
                        return;
                }
            }

            // Now add the variable to the data section based on its type
            switch (dataType.toLowerCase()) {
                case "int":
                    dataSection.put(variableName, variableName + ": .word " + initValue);
                    break;
                case "string":
                    dataSection.put(variableName, variableName + ": .asciiz " + initValue);
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

    public boolean isVariableInDataSection(String variableName) {
        return dataSection.containsKey(variableName);
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

    public String getNextAvailableRegister() {
        String[] availableRegisters = {"$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9"};
        String register = availableRegisters[registerCounter];

        // Move the counter to the next register for the next call
        registerCounter = (registerCounter + 1) % availableRegisters.length;

        return register;
    }

    public String assignRegisterForCondition() {
        if (freeRegisters.isEmpty()) {
            throw new IllegalStateException("No more temporary registers available.");
        }

        // Get the next available register from the free register pool
        String register = freeRegisters.pop();
        usedRegisters.add(register);  // Mark it as in use
        return register;
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

//    public void generateAssignment(String assignment) {
//        // Split the assignment into the variable name and the expression
//        String variableName = assignment.split("=")[0].trim();
//        String expression = assignment.split("=")[1].trim();
//
//        // Evaluate the expression to get the result register
//        String resultRegister = evaluateExpression(expression);
//
//        // Get the register for the variable being assigned
//        String variableRegister = symbolTable.getRegisterForVariable(variableName);
//        if (variableRegister == null) {
//            throw new IllegalArgumentException("Variable '" + variableName + "' not found in SymbolTable.");
//        }
//
//        // Generate MIPS code to move the result into the variable's register
//        System.out.println("Generated MIPS: move " + variableRegister + ", " + resultRegister);
//
//        // Optionally add the MIPS instruction to your list/queue of instructions
//        addMipsInstruction("move " + variableRegister + ", " + resultRegister);
//    }


    // Example evaluateExpression method (simplified)
    public String evaluateExpression(String expression, String endLabel) {
        // Split the expression into operands and operator (assuming basic "operand operator operand" format)
        String[] tokens = expression.split(" ");
        if (tokens.length != 3) {
            throw new IllegalArgumentException("Invalid expression format.");
        }

        String operand1 = tokens[0]; // First operand (e.g., "y")
        String operator = tokens[1]; // Operator (e.g., "+")
        String operand2 = tokens[2]; // Second operand (e.g., "5")

        // Allocate registers for operands and result
        String reg1 = resolveToRegister(operand1);  // Resolve the register for operand1
        String reg2 = resolveToRegister(operand2);  // Resolve the register for operand2
        String regResult = allocateTempRegister();  // Allocate a temporary register for the result

        // Perform the operation based on the operator
        switch (operator) {
            case "+":
                mipsAdd(reg1, reg2, regResult);
                break;
            case "-":
                mipsSub(reg1, reg2, regResult);
                break;
            case "*":
                mipsMul(reg1, reg2, regResult);
                break;
            case "/":
                mipsDiv(reg1, reg2, regResult);
                break;
            case "<":
                addMipsInstruction("slt " + regResult + ", " + reg1 + ", " + reg2); // Set less than
                addMipsInstruction("beq " + regResult + ", $zero, " + endLabel); // Branch if equal
                break;
            case ">":
                addMipsInstruction("slt " + regResult + ", " + reg2 + ", " + reg1); // Set less than (reverse the operands)
                addMipsInstruction("beq " + regResult + ", $zero, " + endLabel); // Branch if equal
                break;
            case "==":
                addMipsInstruction("sub " + regResult + ", " + reg1 + ", " + reg2);
                addMipsInstruction("beq " + regResult + ", $zero, " + endLabel); // Branch if equal
                break;
            case "!=":
                addMipsInstruction("sub " + regResult + ", " + reg1 + ", " + reg2);
                addMipsInstruction("bne " + regResult + ", $zero, " + endLabel); // Branch if not equal
                break;
            case "<=":
                // For <=, check if greater than and jump if true
                addMipsInstruction("slt " + regResult + ", " + reg2 + ", " + reg1); // Set less than
                addMipsInstruction("beq " + regResult + ", $zero, " + endLabel); // Jump if greater (i.e., less than or equal)
                break;
            case ">=":
                // For >=, check if less than and jump if true
                addMipsInstruction("slt " + regResult + ", " + reg1 + ", " + reg2); // Set less than
                addMipsInstruction("beq " + regResult + ", $zero, " + endLabel); // Jump if less (i.e., greater than or equal)
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
        mipsCode.add(endLabel + ":");

        // Join the list into a single string with line breaks
        return String.join("\n", mipsCode);
    }

    public void mipsAdd(String reg1, String reg2, String regResult) {
        System.out.println("mipsAdd called with reg1=" + reg1 + ", reg2=" + reg2 + ", regResult=" + regResult);

        if (isRegister(reg1) && isRegister(reg2)) {
            // Case 1: Both operands are registers
            addMipsInstruction("add " + regResult + ", " + reg1 + ", " + reg2); // Integer addition
        } else if (isInteger(reg2)) {
            // Case 2: reg2 is an immediate value, so use addi
            addMipsInstruction("addi " + regResult + ", " + reg1 + ", " + reg2); // Integer addition with immediate
        } else {
            System.out.println("Invalid operands for mipsAdd: reg1=" + reg1 + ", reg2=" + reg2);
        }
    }


    public void mipsSub(String reg1, String reg2, String regResult) {
        if (isRegister(reg1) && isRegister(reg2)) {
            // Case 1: Both operands are registers (integers)
            addMipsInstruction("sub " + regResult + ", " + reg1 + ", " + reg2); // Integer subtraction
        } else if (isInteger(reg2)) {
            // Case 2: reg2 is an immediate value, so use subi
            addMipsInstruction("subi " + regResult + ", " + reg1 + ", " + reg2); // Integer subtraction with immediate
        } else {
            System.out.println("Invalid operands for mipsSub: reg1=" + reg1 + ", reg2=" + reg2);
        }
    }

    public void mipsMul(String reg1, String reg2, String regResult) {
        if (isRegister(reg1) && isRegister(reg2)) {
            // Case 1: Both operands are registers (integers)
            addMipsInstruction("mul " + regResult + ", " + reg1 + ", " + reg2); // Integer multiplication
        } else if (isInteger(reg2)) {
            // Case 2: reg2 is an immediate value, so use muli
            addMipsInstruction("muli " + regResult + ", " + reg1 + ", " + reg2); // Integer multiplication with immediate
        } else {
            System.out.println("Invalid operands for mipsMul: reg1=" + reg1 + ", reg2=" + reg2);
        }
    }


    public void mipsDiv(String reg1, String reg2, String regResult) {
        if(Integer.parseInt(reg2) == 0){
            throw new ArithmeticException("Division by 0");
        }

        if (isRegister(reg1) && isRegister(reg2)) {
            // Case 1: Both operands are registers (integers)
            addMipsInstruction("div " + reg1 + ", " + reg2);  // Perform integer division
            addMipsInstruction("mflo " + regResult); // Move result to regResult (quotient)
        } else if (isInteger(reg2)) {
            // Case 2: reg2 is an immediate value, so perform division with immediate
            addMipsInstruction("div " + reg1 + ", " + reg2);  // Perform integer division
            addMipsInstruction("mflo " + regResult); // Move result to regResult (quotient)
        } else {
            System.out.println("Invalid operands for mipsDiv: reg1=" + reg1 + ", reg2=" + reg2);
        }
    }

    private boolean isRegister(String operand) {
        return operand.startsWith("$"); // Check if the operand is a register (starts with '$')
    }


    private String resolveToRegister(String operand) {
        // Check if operand is an integer literal
        if (isInteger(operand)) {
            // If operand is a literal, return the literal value instead of a register
            System.out.println("Using immediate value: " + operand);
            return operand;  // Return the literal as a string
        } else {
            // If operand is a variable, get its register from the SymbolTable
            String variableRegister = symbolTable.getRegisterForVariable(operand);

            if (variableRegister == null) {
                System.out.println("Error: Variable '" + operand + "' not found in SymbolTable.");
                throw new IllegalArgumentException("Variable '" + operand + "' not found in SymbolTable.");
            }

            System.out.println("Resolved variable '" + operand + "' to register " + variableRegister);
            return variableRegister;
        }
    }

    public boolean isInteger(String token) {
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDouble(String operand) {
        try {
            Double.parseDouble(operand);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isImmediateValue(String operand) {
        try {
            Integer.parseInt(operand); // Try to parse the operand as an integer
            return true;
        } catch (NumberFormatException e) {
            return false; // Not an integer, so it's not an immediate value
        }
    }


    public String convertConditionToMips(String condition, String label, SymbolTable symbolTable) {
        // Split the condition into parts (e.g., "x < 5" -> ["x", "<", "5"])
        String[] conditionParts = condition.split(" ");
        String operator = conditionParts[1]; // Get the operator
        String leftOperand = conditionParts[0]; // Left side of the condition
        String rightOperand = conditionParts[2]; // Right side of the condition

        // Get the register for the left operand (e.g., "x" -> $t0)
        String leftRegister = symbolTable.getRegisterForVariable(leftOperand);

        if (leftRegister == null) {
            System.out.println("Error: No register found for variable '" + leftOperand + "'");
            return "";
        }

        // We need a temporary register to store the comparison result
        String tempRegister = "$t1"; // Temporary register for condition result
        StringBuilder mipsCode = new StringBuilder();

        // Generate MIPS code based on the operator
        switch (operator) {
            case "<":
                mipsCode.append("slt ").append(tempRegister).append(", ").append(leftRegister).append(", ").append(rightOperand).append("\n");
                mipsCode.append("bne ").append(tempRegister).append(", $zero, ").append(label).append("\n"); // Branch if true
                break;
            case ">":
                mipsCode.append("sgt ").append(tempRegister).append(", ").append(leftRegister).append(", ").append(rightOperand).append("\n");
                mipsCode.append("bne ").append(tempRegister).append(", $zero, ").append(label).append("\n"); // Branch if true
                break;
            case "<=":
                mipsCode.append("sle ").append(tempRegister).append(", ").append(leftRegister).append(", ").append(rightOperand).append("\n");
                mipsCode.append("bne ").append(tempRegister).append(", $zero, ").append(label).append("\n"); // Branch if true
                break;
            case ">=":
                mipsCode.append("sge ").append(tempRegister).append(", ").append(leftRegister).append(", ").append(rightOperand).append("\n");
                mipsCode.append("bne ").append(tempRegister).append(", $zero, ").append(label).append("\n"); // Branch if true
                break;
            case "==":
                mipsCode.append("beq ").append(leftRegister).append(", ").append(rightOperand).append(", ").append(label).append("\n"); // Branch if equal
                break;
            case "!=":
                mipsCode.append("bne ").append(leftRegister).append(", ").append(rightOperand).append(", ").append(label).append("\n"); // Branch if not equal
                break;
            default:
                System.out.println("Error: Unsupported operator '" + operator + "'");
                return "";
        }

        // Return the generated MIPS code
        return mipsCode.toString();
    }


    public void generateIfElse(String condition, List<String> ifBodyTokens, List<String> elseBodyTokens) {
        addMipsInstruction(" ");
        // Generate labels for the if-else structure
        String ifLabel = "IF_BLOCK";
        String elseLabel = "ELSE_BLOCK";
        String endLabel = "END_IF_ELSE";

        // Start generating MIPS code
        addMipsInstruction("# If-Else Structure");

        // Evaluate the condition expression
        String conditionRegister = evaluateExpression(condition, endLabel);  // Get the condition register

        // Process the conditional logic based on operator (e.g., <, >, ==, !=)
//        String operator = extractOperatorFromCondition(condition); // Extract operator from the condition
//        String dataRegister = getDataRegisterForCondition(condition); // Get data register for condition evaluation
//        String constantRegister = getConstantRegisterForCondition(condition); // Get constant register (if applicable)


        // If the condition is false, jump to the else block
//        addMipsInstruction("beq " + conditionRegister + ", $zero, " + elseLabel);

        // If block
        addMipsInstruction(ifLabel + ":");
        processBodyTokens(ifBodyTokens); // Generate MIPS for if block

        // Jump to the end label after if block
        addMipsInstruction("j " + endLabel);

        // Else block (only if else body tokens are not empty)
        if (!elseBodyTokens.isEmpty()) {
            addMipsInstruction(elseLabel + ":");
            processBodyTokens(elseBodyTokens); // Generate MIPS for else block
        }

        // End of if-else structure
        addMipsInstruction(endLabel + ":");
    }

    // Helper method to extract the operator from the condition
    private String extractOperatorFromCondition(String condition) {
        // Simple example, could be extended based on your actual condition format
        if (condition.contains("<")) {
            return "<";
        } else if (condition.contains(">")) {
            return ">";
        } else if (condition.contains("==")) {
            return "==";
        } else if (condition.contains("!=")) {
            return "!=";
        } else {
            throw new IllegalArgumentException("Invalid operator in condition");
        }
    }

    // Helper methods to extract data and constant registers (dummy implementations, replace with actual logic)
    private String getDataRegisterForCondition(String condition) {
        // Logic to return the data register associated with the condition (for now, dummy return)
        return "$t0";  // Example, replace with actual register logic
    }

    private String getConstantRegisterForCondition(String condition) {
        // Logic to return the constant register (for now, dummy return)
        return "$t1";  // Example, replace with actual register logic
    }

    public void processBodyTokens(List<String> bodyTokens) {
        for (String token : bodyTokens) {
            if (token.startsWith("print")) {
                // Example: print("Hello")
                String message = extractPrintMessage(token);
                addMipsInstruction("# Printing message: " + message);
                generatePrint(message);
            } else if (token.contains("=")) {
                // Example: x = 10
                String[] parts = token.split("=");
                String variable = parts[0].trim();
                String value = parts[1].trim();
                addMipsInstruction("# Assigning value to variable: " + variable);
                generateAssignmentInstruction(variable, value);
            } else if (token.startsWith("if")) {
                // Nested if-else handling
                String condition = extractCondition(token);
                List<String> nestedIfBodyTokens = extractIfBodyTokens(token);
                List<String> nestedElseBodyTokens = extractElseBodyTokens(token);
                generateIfElse(condition, nestedIfBodyTokens, nestedElseBodyTokens);
            } else {
                addMipsInstruction("# Unknown token: " + token);
            }
        }
    }

    private String extractPrintMessage(String token) {
        // Assuming format: print("message")
        int startIndex = token.indexOf("\"") + 1;
        int endIndex = token.lastIndexOf("\"");
        return token.substring(startIndex, endIndex);
    }

    private void generateAssignmentInstruction(String variable, String value) {
        // Assuming integer assignment
        addMipsInstruction("li $t0, " + value); // Load immediate value into a temporary register
        addMipsInstruction("sw $t0, " + variable); // Store the value into the variable's memory address
    }

    private List<String> extractIfBodyTokens(String token) {
        // Extracts tokens between `{` and `}` of the `if` block
        int start = token.indexOf("{") + 1;
        int end = token.indexOf("}");
        String body = token.substring(start, end).trim();
        return Arrays.asList(body.split(";")); // Assuming semicolon-separated statements
    }

    private List<String> extractElseBodyTokens(String token) {
        // Extracts tokens for the `else` block, if it exists
        int elseIndex = token.indexOf("else");
        if (elseIndex == -1) return new ArrayList<>();
        int start = token.indexOf("{", elseIndex) + 1;
        int end = token.indexOf("}", elseIndex);
        String body = token.substring(start, end).trim();
        return Arrays.asList(body.split(";")); // Assuming semicolon-separated statements
    }


    public void generateWhileLoopCondition(String condition, String conditionRegister) {
        if(condition == null || condition.trim().isEmpty()){
            throw new IllegalArgumentException("Condition cannot be null or empty.");
        }

        // Assuming the condition is in the format "y < 5"
        String[] conditionTokens = condition.split(" ");
        if (conditionTokens.length != 3) {
            throw new IllegalArgumentException("Invalid condition format.");
        }

        String variable = conditionTokens[0].trim();  // "y"
        String operator = conditionTokens[1].trim();  // "<"
        String value = conditionTokens[2].trim();     // "5"

        // Resolve the variable to its register
        String register = resolveToRegister(variable);

        // Allocate a temporary register for the constant value (5)
        String constantRegister = allocateTempRegister();
        addMipsInstruction("li " + constantRegister + ", " + value);  // Load 5 into a temporary register

        // Generate the comparison MIPS instruction based on the operator
        switch (operator) {
            case "<":
                addMipsInstruction("slt " + conditionRegister + ", " + register + ", " + constantRegister);
                break;
            case "<=":
                addMipsInstruction("sle " + conditionRegister + ", " + register + ", " + constantRegister);
                break;
            case ">":
                addMipsInstruction("sgt " + conditionRegister + ", " + register + ", " + constantRegister);
                break;
            case ">=":
                addMipsInstruction("sge " + conditionRegister + ", " + register + ", " + constantRegister);
                break;
            case "==":
                addMipsInstruction("beq " + conditionRegister + ", " + register + ", " + constantRegister);
                break;
            case "!=":
                addMipsInstruction("bne " + conditionRegister + ", " + register + ", " + constantRegister);
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }

        // Debug output: check the condition register after the comparison
        // Add any conditional jump logic as needed, e.g., `bnez`
    }


    // Generate MIPS code for a 'while' loop
    // Method to generate MIPS code for a while loop
    public void generateWhileLoop(String condition, List<String> bodyTokens) {
        addMipsInstruction(" ");
        if (bodyTokens == null || bodyTokens.isEmpty()) {
            throw new IllegalArgumentException("Loop body tokens are empty.");
        }

        // Extract the loop variable and constant from the condition (e.g., "y < 5")
        String loopVar = condition.split(" ")[0].trim(); // Loop variable (e.g., "y")
        String operator = condition.split(" ")[1].trim(); // Condition operator (e.g., "<")
        String constant = condition.split(" ")[2].trim(); // Constant value (e.g., "5")

        // Retrieve the register for the loop variable (e.g., "y")
        String dataRegister = assignRegister(loopVar);

        // Handle the constant as a value (assign to a new register if necessary)
        String constantRegister = assignRegister(constant); // Register for constant value
        String conditionRegister = assignRegisterForCondition(); // Register for condition result

        // Start of the loop
        String startLabel = "label_6";
        String endLabel = "label_7";
        addMipsInstruction(startLabel + ":");

        // Condition check
        addComment("Check condition for " + loopVar + " " + operator + " " + constant);
        switch (operator) {
            case "<":
                addMipsInstruction("slt " + conditionRegister + ", " + dataRegister + ", " + constantRegister);
                break;
            case ">":
                addMipsInstruction("slt " + conditionRegister + ", " + constantRegister + ", " + dataRegister);
                break;
            case "==":
                addMipsInstruction("sub " + conditionRegister + ", " + dataRegister + ", " + constantRegister);
                addMipsInstruction("beq " + conditionRegister + ", $zero, " + endLabel);
                break;
            case "!=":
                addMipsInstruction("sub " + conditionRegister + ", " + dataRegister + ", " + constantRegister);
                addMipsInstruction("bne " + conditionRegister + ", $zero, " + endLabel);
                break;
            case "<=":
                // For <=, check if the value is greater than the constant and jump if true
                addMipsInstruction("slt " + conditionRegister + ", " + constantRegister + ", " + dataRegister); // less than
                addMipsInstruction("beq " + conditionRegister + ", $zero, " + endLabel); // if true, jump to end (greater than)
                break;
            case ">=":
                // For >=, check if the value is less than the constant and jump if true
                addMipsInstruction("slt " + conditionRegister + ", " + dataRegister + ", " + constantRegister); // less than
                addMipsInstruction("beq " + conditionRegister + ", $zero, " + endLabel); // if true, jump to end (less than)
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }


        // If the condition is false, jump to the end of the loop
        addMipsInstruction("beq " + conditionRegister + ", $zero, " + endLabel);

        // Loop body
        for (String bodyToken : bodyTokens) {
            bodyToken = bodyToken.trim();

            if (bodyToken.contains("=")) {
                // Assignment statement with an arithmetic expression
                String[] statementParts = bodyToken.split("=");
                String leftSide = statementParts[0].trim();
                String rightSide = statementParts[1].trim().replace(";", ""); // Remove semicolon

                // Resolve the register for the left-hand side variable
                String leftRegister = symbolTable.getRegisterForVariable(leftSide);
                if (leftRegister == null) {
                    throw new IllegalStateException("Variable '" + leftSide + "' not found in symbol table");
                }

                // Evaluate the right-hand side expression
                String resultRegister = evaluateExpression(rightSide, endLabel); // Evaluate the arithmetic expression

                // Store the result in the left-hand side variable's register
                addMipsInstruction("move " + leftRegister + ", " + resultRegister);
            } else if (bodyToken.equals("++") || bodyToken.equals("--")) {
                handleIncrementOrDecrement(bodyToken);
            } else if (bodyToken.startsWith("print")) {
                handlePrintStatement(bodyToken);
            } else {
                processBodyToken(bodyToken);
            }
        }

        // Store the updated value of the loop variable back to memory (if applicable)
        addMipsInstruction("sw " + dataRegister + ", " + loopVar);

        // Jump back to the start of the loop
        addMipsInstruction("j " + startLabel);

        // End of the loop
        addMipsInstruction(endLabel + ":");
    }

    private void processVariableToken(String varToken) {
        // This method can be used to handle cases where the token is a variable
        // For now, let's just log it for clarity
        addComment("Processing variable token: " + varToken);
    }

    // Process an assignment statement like "y = y + 1;"
    public void processAssignmentStatement(String bodyToken) {
        String[] statementParts = bodyToken.split("=");
        String leftSide = statementParts[0].trim();
        String rightSide = statementParts[1].trim().replace(";", ""); // Remove semicolon

        // Resolve the register for the left-hand side variable
        String leftRegister = symbolTable.getRegisterForVariable(leftSide);
        if (leftRegister == null) {
            throw new IllegalStateException("Variable '" + leftSide + "' not found in symbol table");
        }

        // Now we need to process the right-hand side of the equation (e.g., y + 1)
        String resultRegister = null;

        // Check if the right-hand side contains arithmetic operations
        if (rightSide.contains("+")) {
            String[] operands = rightSide.split("\\+");
            String leftOperand = operands[0].trim();
            String rightOperand = operands[1].trim();

            // Call mipsAdd for addition
            mipsAdd(leftOperand, rightOperand, leftRegister);
        } else if (rightSide.contains("-")) {
            String[] operands = rightSide.split("-");
            String leftOperand = operands[0].trim();
            String rightOperand = operands[1].trim();

            // Call mipsSub for subtraction
            mipsSub(leftOperand, rightOperand, leftRegister);
        } else if (rightSide.contains("*")) {
            String[] operands = rightSide.split("\\*");
            String leftOperand = operands[0].trim();
            String rightOperand = operands[1].trim();

            // Call mipsMul for multiplication
            mipsMul(leftOperand, rightOperand, leftRegister);
        } else if (rightSide.contains("/")) {
            String[] operands = rightSide.split("/");
            String leftOperand = operands[0].trim();
            String rightOperand = operands[1].trim();

            // Call mipsDiv for division
            mipsDiv(leftOperand, rightOperand, leftRegister);
        } else {
            // If there's no arithmetic operation, evaluate the right side normally
            resultRegister = evaluateExpression(rightSide, generateLabel());
        }

        // Store the result in the left-hand side variable's register
        addMipsInstruction("move " + leftRegister + ", " + resultRegister);
    }


    private void handlePrintStatement(String statement) {
        // Parse the print statement
        String regex = "print\\s*\\(\\s*(\\w+)\\s*\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(statement);

        if (matcher.find()) {
            String variableName = matcher.group(1);
            String printRegister = symbolTable.getRegisterForVariable(variableName);

            if (printRegister != null) {
                generatePrint(printRegister);
            } else {
                System.out.println("Error: No register found for variable '" + variableName + "' in print statement.");
            }
        } else {
            System.out.println("Error: Malformed print statement: " + statement);
        }
    }

    private void handleAssignmentStatement(String[] statement) {
        // Handle assignments like y = y + 1 or y = z + 1;
        // statement[0] is the left side ("y"), statement[1] is the right side ("y + 1")
        String leftSide = statement[0].trim();
        String rightSide = statement[1].trim();

        // Load the current value of the left-side variable (e.g., y)
        String register = symbolTable.getRegisterForVariable(leftSide);
        if (register == null) {
            throw new IllegalStateException("Variable '" + leftSide + "' not found in symbol table");
        }

        // Parse and evaluate the right-hand side (e.g., "y + 1")
        String[] operandsAndOperator = parseExpression(rightSide);

        // Load operands into registers
        String regOperand1 = getOperandRegister(operandsAndOperator[0].trim());  // Get register for operand1 (e.g., "y")
        String regOperand2 = getOperandRegister(operandsAndOperator[1].trim());  // Get register for operand2 (e.g., "1")

        addMipsInstruction("add " + register + ", " + regOperand1 + ", " + regOperand2);

        // Update the symbol table with the new value of the left-side variable
        symbolTable.addRegisterToVariable(leftSide, register);
    }

    private String[] parseExpression(String expression) {
        // This method parses the right-hand side expression (e.g., "y + 1")
        // and returns the operands and operator in an array of size 2.
        String[] operandsAndOperator;

        // Check for operators and split accordingly (can be extended for other operators)
        if (expression.contains("+")) {
            operandsAndOperator = expression.split("\\+");
        } else if (expression.contains("-")) {
            operandsAndOperator = expression.split("-");
        } else if (expression.contains("*")) {
            operandsAndOperator = expression.split("\\*");
        } else if (expression.contains("/")) {
            operandsAndOperator = expression.split("/");
        } else {
            // If no operator is found, treat it as a single operand (e.g., just a number or variable)
            operandsAndOperator = new String[]{expression};
        }

        return operandsAndOperator;
    }

    private String getOperandRegister(String operand) {
        // Check if the operand is a variable
        if (symbolTable.containsVariable(operand)) {
            return symbolTable.getRegisterForVariable(operand);
        } else {
            // If it's a constant, load it into a temporary register
            String tempRegister = assignRegister(operand);
            addMipsInstruction("li " + tempRegister + ", " + operand); // Load immediate value
            return tempRegister;
        }
    }


    public String assignRegister(String variable) {
        // Check if the variable is a number or an immediate constant (this includes literal values)
        boolean isConstant = isConstant(variable); // Helper method to check if the variable is a constant value

        // If the variable is constant, we handle it differently
        if (isConstant) {
            // Assign register and load immediate value (constant)
            String register = availableRegisters.poll();  // Get and remove the first available register
            if (register == null) {
                throw new RuntimeException("No available registers for constant: " + variable);
            }

            addMipsInstruction("li " + register + ", " + variable);  // Load immediate value into register
            return register;
        }

        // For regular variables, check if a register already exists
        String register = symbolTable.getRegister(variable);  // Retrieve register if it exists
        if (register != null) {
            return register;  // Return the existing register
        }

        // Determine if the variable is stored in the data section
        boolean isInDataSection = isVariableInDataSection(variable);
        register = availableRegisters.poll();  // Get and remove the first available register
        if (register == null) {
            throw new RuntimeException("No available registers for variable: " + variable);
        }

        // Add the register to the symbol table
        symbolTable.addRegisterToVariable(variable, register);

        // If the variable is in the data section, load its value from memory
        if (isInDataSection) {
            addMipsInstruction("lw " + register + ", " + variable);  // Load word for data section variable
        } else {
            // Otherwise, assume it's an immediate value
            addMipsInstruction("li " + register + ", " + variable);  // Load immediate value
        }

        return register;
    }

    // Helper method to check if a variable is a constant (numeric value)
    private boolean isConstant(String variable) {
        try {
            Integer.parseInt(variable);  // Try to parse the variable as an integer
            return true;  // It's a constant if it parses successfully
        } catch (NumberFormatException e) {
            return false;  // Not a constant if it throws an exception
        }
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

    public void handleIncrementOrDecrement(String expression) {
        // Check if the expression contains '++' or '--'
        if (expression.contains("++")) {
            // Extract the variable part before '++' (e.g., "i" from "i++")
            String variable = expression.split("\\+\\+")[0].trim();
            if (!variable.isEmpty()) {
                // If the variable is valid, treat it as an increment
                generateIncrementOrDecrement(variable, true);  // True for increment
            } else {
                System.out.println("Error: Invalid increment expression: " + expression);
            }
        } else if (expression.contains("--")) {
            // Extract the variable part before '--' (e.g., "i" from "i--")
            String variable = expression.split("--")[0].trim();
            if (!variable.isEmpty()) {
                // If the variable is valid, treat it as a decrement
                generateIncrementOrDecrement(variable, false);  // False for decrement
            } else {
                System.out.println("Error: Invalid decrement expression: " + expression);
            }
        } else {
            System.out.println("Invalid increment/decrement operation: " + expression);
        }
    }



    public void generateIncrementOrDecrement(String variable, boolean isIncrement) {
        // Check if the variable is already assigned a register
        String register = symbolTable.getRegister(variable);

        if (register == null) {
            // If the variable doesn't have a register, assign one
            register = allocateTempRegister();
            symbolTable.addRegisterToVariable(variable, register);
            System.out.println("Allocated new register for " + variable + ": " + register);
        } else {
            System.out.println("Register for " + variable + ": " + register);
        }

        // Determine the operation type (increment or decrement)
        if (isIncrement) {
            // Debugging message to confirm the increment is being used
            System.out.println("Incrementing variable " + variable);  // Debug message
            addMipsInstruction("# Increment variable " + variable);
            addMipsInstruction("addi " + register + ", " + register + ", 1");  // This should print the instruction
        } else {
            // Debugging message to confirm the decrement is being used
            System.out.println("Decrementing variable " + variable);  // Debug message
            addMipsInstruction("# Decrement variable " + variable);
            addMipsInstruction("subi " + register + ", " + register + ", 1");  // This should print the instruction
        }
    }



    // Method to handle the entire for loop with the print and increment functionality
    public void generateForLoop(String initialization, String condition, String increment, List<String> bodyTokens) {
        addMipsInstruction(" ");
        // Initialize loop variable
        String loopVar = initialization.split("=")[0].trim();
        String initialValue = initialization.split("=")[1].trim();
        String register = assignRegister(loopVar);

        // Add the loop variable to the symbol table
        symbolTable.addRegisterToVariable(loopVar, register);
//        addMipsInstruction("li " + register + ", " + initialValue); // Load initial value into the register

        String startLabel = "label_8"; // Start of the loop
        String endLabel = "label_9";   // End of the loop

        // Start of the loop
        addMipsInstruction(startLabel + ":");

        // Condition check
        addComment("Check condition for " + loopVar);
        String mipsCondition = convertConditionToMips(condition, endLabel, symbolTable); // Pass the endLabel for conditional branching
        addMipsInstruction(mipsCondition); // Add the condition check MIPS instructions

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

        // Handle the increment or decrement statement
        String incrementTrimmed = increment.trim(); // Remove extra spaces
        if (incrementTrimmed.endsWith("++")) {
            handleIncrementOrDecrement(incrementTrimmed); // Handle "i++"
        } else if (incrementTrimmed.endsWith("--")) {
            handleIncrementOrDecrement(incrementTrimmed); // Handle "i--"
        }

        // Jump back to the start of the loop
        addMipsInstruction("j " + startLabel);

        // End of the loop
        addMipsInstruction(endLabel + ":");
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
            declareVariable(variableName, value, false);

        } else if (bodyToken.matches("\\w+\\s*=\\s*.+")) {
            // Handle assignments and arithmetic expressions
            String variableName = bodyToken.split("=")[0].trim();
            String expression = bodyToken.split("=")[1].trim();

            // Evaluate the expression and get the result register
            String resultRegister = evaluateExpression(expression, generateLabel());

            // Resolve the variable's register
            String variableRegister = resolveToRegister(variableName);

            // Move the result from the evaluated expression to the variable's register
            generateMove(variableRegister, resultRegister);  // Assuming this method generates a move instruction

            // Optional: Update the symbol table with the new value (if necessary)
//            symbolTable.updateVariable(variableName, variableRegister);
        }else if (bodyToken.startsWith("if")) {
            // Handle if-else
            String condition = extractCondition(bodyToken);
            List<String> ifTokens = extractIfTokens(bodyToken); // Extract tokens for the if block
            List<String> elseTokens = extractElseTokens(bodyToken); // Extract tokens for the else block (if exists)

            // Call the method with condition, ifTokens, and elseTokens
            generateIfElse(condition, ifTokens, elseTokens);
        }
        else if (bodyToken.startsWith("while")) {
            // Handle while loops
            String condition = extractCondition(bodyToken);
            List<String> bodyTokens = extractBodyTokens(bodyToken);
            generateWhileLoop(condition, bodyTokens);

        } else {
            throw new IllegalArgumentException("Unrecognized body token: " + bodyToken);
        }
    }

    public void generateMove(String destinationRegister, String sourceRegister) {
        // Check if sourceRegister is a literal (immediate value)
        if (sourceRegister.matches("-?\\d+")) {  // If it's a literal (integer)
            // If the source is an immediate value, use the "li" instruction (load immediate)
            System.out.println("li " + destinationRegister + ", " + sourceRegister);  // li destination, immediate
        } else {
            // If both are registers, use the "move" instruction
            System.out.println("move " + destinationRegister + ", " + sourceRegister);  // move destination, source
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

    private List<String> extractIfTokens(String bodyToken) {
        // Extract tokens for the if block
        // Assumes the format "if(condition) { ... } else { ... }"
        int startIndex = bodyToken.indexOf("{") + 1;
        int elseIndex = bodyToken.indexOf("else");
        if (elseIndex == -1) {
            elseIndex = bodyToken.lastIndexOf("}");
        }
        String ifBody = bodyToken.substring(startIndex, elseIndex).trim();
        return Arrays.asList(ifBody.split(";")); // Split into individual statements
    }

    private List<String> extractElseTokens(String bodyToken) {
        // Extract tokens for the else block (if it exists)
        int elseIndex = bodyToken.indexOf("else {");
        if (elseIndex == -1) {
            return new ArrayList<>(); // No else block
        }
        int startIndex = elseIndex + "else {".length();
        int endIndex = bodyToken.lastIndexOf("}");
        String elseBody = bodyToken.substring(startIndex, endIndex).trim();
        return Arrays.asList(elseBody.split(";")); // Split into individual statements
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

    public void declareVariable(String variableName, String value, boolean inMainMethod) {
        String reg = symbolTable.getRegister(variableName);

        if (inMainMethod) {
            // If the variable should be in the main method, use the li instruction
            addMipsInstruction("li " + reg + ", " + value);  // Load immediate value to the register
        }
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
