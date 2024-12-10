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

    public void generateAssignment(String assignment) {
        // Split the assignment into the variable name and the expression
        String variableName = assignment.split("=")[0].trim();
        String expression = assignment.split("=")[1].trim();

        // Evaluate the expression to get the result register
        String resultRegister = evaluateExpression(expression);

        // Get the register for the variable being assigned
        String variableRegister = symbolTable.getRegisterForVariable(variableName);
        if (variableRegister == null) {
            throw new IllegalArgumentException("Variable '" + variableName + "' not found in SymbolTable.");
        }

        // Generate MIPS code to move the result into the variable's register
        System.out.println("Generated MIPS: move " + variableRegister + ", " + resultRegister);

        // Optionally add the MIPS instruction to your list/queue of instructions
        addMipsInstruction("move " + variableRegister + ", " + resultRegister);
    }


    // Example evaluateExpression method (simplified)
    public String evaluateExpression(String expression) {
        // Split the expression into operands and operator (assuming basic "operand operator operand" format)
        String[] tokens = expression.split(" ");
        if (tokens.length != 3) {
            throw new IllegalArgumentException("Invalid expression format.");
        }

        String operand1 = tokens[0]; // First operand (e.g., "a")
        String operator = tokens[1]; // Operator (e.g., "+")
        String operand2 = tokens[2]; // Second operand (e.g., "b")

        // Allocate registers for operands and result
        String reg1 = resolveToRegister(operand1);
        String reg2 = resolveToRegister(operand2);
        String regResult = allocateTempRegister();

        // Load operands into registers
//        loadRegister(reg1, operand1); // Load operand1 into reg1
//        loadRegister(reg2, operand2); // Load operand2 into reg2

        // Perform the operation based on the operator
        switch (operator) {
            case "+":
                mipsAdd(reg1, reg2, regResult); // Perform addition
                break;
            case "-":
                mipsSub(reg1, reg2, regResult); // Perform subtraction
                break;
            case "*":
                mipsMul(reg1, reg2, regResult); // Perform multiplication
                break;
            case "/":
                mipsDiv(reg1, reg2, regResult); // Perform division
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }

        // Return the register containing the result
        return regResult;
    }

    private void mipsAdd(String reg1, String reg2, String regResult) {
        // Check if reg2 is an immediate value (constant)
        if (isIntegerOperation(reg1, reg2) && isImmediateValue(reg2)) {
            // Use addi when reg2 is an immediate value (constant)
            addMipsInstruction("addi " + regResult + ", " + reg1 + ", " + reg2); // Integer add with immediate
        } else if (isIntegerOperation(reg1, reg2)) {
            // Use add when both operands are registers (integer add)
            addMipsInstruction("add " + regResult + ", " + reg1 + ", " + reg2); // Integer add
        } else {
            // Use add.s for floating-point addition
            addMipsInstruction("add.s " + regResult + ", " + reg1 + ", " + reg2); // Floating-point add
        }
    }


    private void mipsSub(String reg1, String reg2, String regResult) {
        // Check if reg2 is an immediate value (constant)
        if (isIntegerOperation(reg1, reg2) && isImmediateValue(reg2)) {
            // Use subi when reg2 is an immediate value (constant)
            addMipsInstruction("subi " + regResult + ", " + reg1 + ", " + reg2); // Integer sub with immediate
        } else if (isIntegerOperation(reg1, reg2)) {
            // Use sub when both operands are registers (integer sub)
            addMipsInstruction("sub " + regResult + ", " + reg1 + ", " + reg2); // Integer sub
        } else {
            // Use sub.s for floating-point subtraction
            addMipsInstruction("sub.s " + regResult + ", " + reg1 + ", " + reg2); // Floating-point sub
        }
    }


    private void mipsMul(String reg1, String reg2, String regResult) {
        // Check if reg2 is an immediate value (constant)
        if (isIntegerOperation(reg1, reg2) && isImmediateValue(reg2)) {
            // Use muli when reg2 is an immediate value (constant)
            addMipsInstruction("mul " + regResult + ", " + reg1 + ", " + reg2); // Integer mul with immediate
        } else if (isIntegerOperation(reg1, reg2)) {
            // Use mul when both operands are registers (integer mul)
            addMipsInstruction("mul " + regResult + ", " + reg1 + ", " + reg2); // Integer mul
        } else {
            // Use mul.s for floating-point multiplication
            addMipsInstruction("mul.s " + regResult + ", " + reg1 + ", " + reg2); // Floating-point mul
        }
    }


    private void mipsDiv(String reg1, String reg2, String regResult) {
        // Check if reg2 is an immediate value (constant)
        if (isIntegerOperation(reg1, reg2) && isImmediateValue(reg2)) {
            // Use divi when reg2 is an immediate value (constant)
            addMipsInstruction("div " + reg1 + ", " + reg2); // Integer div with immediate
            addMipsInstruction("mflo " + regResult); // Move the result to regResult
        } else if (isIntegerOperation(reg1, reg2)) {
            // Use div when both operands are registers (integer div)
            addMipsInstruction("div " + reg1 + ", " + reg2); // Integer div
            addMipsInstruction("mflo " + regResult); // Move the result to regResult
        } else {
            // Use div.s for floating-point division
            addMipsInstruction("div.s " + reg1 + ", " + reg2); // Floating-point div
            addMipsInstruction("mov.s " + regResult + ", $f0"); // Move result to regResult
        }
    }

    private String resolveToRegister(String operand) {
        if (isInteger(operand)) {
            // If operand is a literal, load it into a temporary register
            String tempRegister = allocateTempRegister();
            addMipsInstruction("li " + tempRegister + ", " + operand);
            return tempRegister;
        } else {
            // If operand is a variable, get its register from the SymbolTable
            String variableRegister = symbolTable.getRegisterForVariable(operand);
            if (variableRegister == null) {
                throw new IllegalArgumentException("Variable '" + operand + "' not found in SymbolTable.");
            }
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

    private boolean isImmediateValue(String operand) {
        try {
            Integer.parseInt(operand); // Try to parse the operand as an integer
            return true;
        } catch (NumberFormatException e) {
            return false; // Not an integer, so it's not an immediate value
        }
    }


    private boolean isIntegerOperation(String reg1, String reg2) {
        // Add logic to check if both operands are integers (this might involve checking register contents or types)
        return true;  // Assuming for simplicity that the operands are integers
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

    public String convertConditionToMips(String condition, String register, SymbolTable symbolTable) {
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
        String tempRegister = "$t1";  // We can use $t1 for storing the result

        // Generate MIPS code based on the operator
        String mipsCode = "";

        switch (operator) {
            case "<":
                mipsCode = "slt " + tempRegister + ", " + leftRegister + ", " + rightOperand; // $t1 = (leftOperand < rightOperand) ? 1 : 0
                break;
            case ">":
                mipsCode = "sgt " + tempRegister + ", " + leftRegister + ", " + rightOperand; // $t1 = (leftOperand > rightOperand) ? 1 : 0
                break;
            case "<=":
                mipsCode = "sle " + tempRegister + ", " + leftRegister + ", " + rightOperand; // $t1 = (leftOperand <= rightOperand) ? 1 : 0
                break;
            case ">=":
                mipsCode = "sge " + tempRegister + ", " + leftRegister + ", " + rightOperand; // $t1 = (leftOperand >= rightOperand) ? 1 : 0
                break;
            case "==":
                mipsCode = "beq " + tempRegister + ", " + leftRegister + ", " + rightOperand; // $t1 = (leftOperand == rightOperand) ? 1 : 0
                break;
            case "!=":
                mipsCode = "bne " + tempRegister + ", " + leftRegister + ", " + rightOperand; // $t1 = (leftOperand != rightOperand) ? 1 : 0
                break;
            default:
                System.out.println("Error: Unsupported operator '" + operator + "'");
                return "";
        }

        // Return the generated MIPS code
        return mipsCode;
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
        // Debugging: Print the body tokens before processing them
        // System.out.println("Block tokens: " + bodyTokens);

        // Ensure that bodyTokens is not empty
        if (bodyTokens == null || bodyTokens.isEmpty()) {
            throw new IllegalArgumentException("Loop body tokens are empty.");
        }

        // Extract loop variable and assign a register for the condition
        String loopVar = condition.split(" ")[0].trim();  // e.g., "y"
        String register = assignRegister(loopVar);  // e.g., $t0
        symbolTable.addRegisterToVariable(loopVar, register);  // Add to symbol table

        // Dynamically assign a register for storing the condition result
        String conditionRegister = assignRegisterForCondition();  // e.g., $t5

        // Add the condition register to the symbol table
        symbolTable.putConditionRegister(conditionRegister, condition);

        // Label for the start of the loop
        addMipsInstruction("label_6:");  // Label for start of the loop

        // Check the condition
        addComment("Check condition for " + loopVar);

        // Generate MIPS for the condition and store the result in the condition register
        generateWhileLoopCondition(condition, conditionRegister);  // Use the condition register here

        // Now the result of the condition should be stored in `conditionRegister`
        addMipsInstruction("beq $zero, " + conditionRegister + ", label_7");  // Exit if false

        // Process the body of the loop if the condition is true
        StringBuilder statementBuilder = new StringBuilder();
        for (String bodyToken : bodyTokens) {
            bodyToken = bodyToken.trim();

            // If the body token contains an assignment (e.g., "i = 10" or "i++")
            if (bodyToken.contains("=")) {
                handleAssignmentStatement(bodyToken);
            }
            // If the body token is an increment or decrement (e.g., "i++" or "i--")
            else if (bodyToken.equals("++") || bodyToken.equals("--")) {
                handleIncrementOrDecrement(bodyToken);
            }
            // If the body token is a semicolon or needs special handling
            else if (bodyToken.equals(";")) {
                String completeStatement = statementBuilder.toString().trim();
                statementBuilder.setLength(0);
                if (completeStatement.startsWith("print")) {
                    handlePrintStatement(completeStatement);  // Print statement
                } else {
                    processBodyToken(completeStatement);  // Process other tokens
                }
            } else {
                statementBuilder.append(bodyToken).append(" ");
            }
        }

        // Jump back to the start of the loop
        addMipsInstruction("j label_6");

        // Label for the end of the loop
        addMipsInstruction("label_7:");
    }


    public void processToken(String token) {
        // Example of how to process a token (e.g., 'y = y + 1')
        if (token.matches("\\w+\\s*=\\s*.+")) {
            String variableName = token.split("=")[0].trim(); // y
            String expression = token.split("=")[1].trim();  // y + 1

            // Evaluate the expression (e.g., 'y + 1')
            String resultRegister = evaluateExpression(expression);  // Evaluates the expression to a register

            // Get the register for the variable being assigned (y)
            String variableRegister = symbolTable.getRegisterForVariable(variableName);
            if (variableRegister == null) {
                throw new IllegalArgumentException("Variable '" + variableName + "' not found in SymbolTable.");
            }

            // Generate MIPS code to assign the result to the variable (y)
            addMipsInstruction("move " + variableRegister + ", " + resultRegister);
        }
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

    private void handleAssignmentStatement(String statement) {
        // Handle assignments like y = y + 1;
        // First, split the statement into left and right parts
        String[] parts = statement.split("=");
        String leftSide = parts[0].trim();  // "y"
        String rightSide = parts[1].trim(); // "y + 1"

        // Load the current value of y
        String register = symbolTable.getRegisterForVariable(leftSide);
        if (register == null) {
            throw new IllegalStateException("Variable '" + leftSide + "' not found in symbol table");
        }

        // Split the right-hand side into operands (e.g., y + 1)
        String[] rightParts = rightSide.split("\\+");
        String operand1 = rightParts[0].trim();  // "y"
        String operand2 = rightParts[1].trim();  // "1"

        // Load operand1 (y) into a register
        String regOperand1 = symbolTable.getRegisterForVariable(operand1);
        if (regOperand1 == null) {
            throw new IllegalStateException("Variable '" + operand1 + "' not found in symbol table");
        }

        // Load operand2 (1) into a register
        String regOperand2 = assignRegister("temp"); // Temporary register for 1
        addMipsInstruction("li " + regOperand2 + ", " + operand2);  // Load immediate value 1

        // Add y + 1 and store the result in y
        String resultRegister = register;  // Register for y
        addMipsInstruction("add " + resultRegister + ", " + regOperand1 + ", " + regOperand2);  // y = y + 1

        // Update the symbol table with the new value of y
        symbolTable.addRegisterToVariable(leftSide, resultRegister);
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

        String incrementTrimmed = increment.trim();  // Remove extra spaces
        if (incrementTrimmed.endsWith("++")) {
            // Handle increment "i++"
            handleIncrementOrDecrement(incrementTrimmed);  // Pass "i++"
        } else if (incrementTrimmed.endsWith("--")) {
            // Handle decrement "i--"
            handleIncrementOrDecrement(incrementTrimmed);  // Pass "i--"
        }


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
            generateAssignment(expression);

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
