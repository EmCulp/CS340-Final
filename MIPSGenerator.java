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

//    public static void generateLoop(String[] conditionTokens, String loopStart, String loopEnd, List<String> blockTokens) throws Exception {
//        // Generate the MIPS code for the condition (evaluate only once at the start)
//        if (generateMips && mipsGenerator != null) {
//            System.out.println("Generating MIPS for condition (once)...");
//
//            evaluator.evaluateCondition(conditionTokens, loopStart, loopEnd, false); // First-time condition evaluation
//        }
//
//        // Now execute the loop
//        while (true) {
//            // Re-evaluate the condition for each iteration
//            System.out.println("\nRe-evaluating condition...");
//            boolean conditionResult = evaluator.evaluateCondition(conditionTokens, loopStart, loopEnd, true);
//
//            // Print condition evaluation result
//            System.out.println("Condition evaluated to " + (conditionResult ? "true" : "false"));
//
//            if (!conditionResult) {
//                System.out.println("Condition evaluated to false, exiting loop.");
//                break; // Exit the loop if condition evaluates to false
//            }
//
//            // Execute the loop body (statements inside the loop)
//            System.out.println("Generating MIPS for loop body...");
//            StringBuilder statementBuilder = new StringBuilder();  // StringBuilder to concatenate tokens
//
//            for (String token : blockTokens) {
//                token = token.trim(); // Trim whitespace
//
//                if (!token.isEmpty()) {
//                    statementBuilder.append(token).append(" ");  // Concatenate tokens with space
//
//                    // If the token is a semicolon, execute the full statement
//                    if (token.equals(";")) {
//                        String fullStatement = statementBuilder.toString().trim();  // Build the full statement
//                        System.out.println("Executing statement: " + fullStatement);
//
//                        // Ensure the statement ends with a semicolon
//                        if (!fullStatement.endsWith(";")) {
//                            fullStatement += ";";
//                        }
//
//                        String[] fullStatementTokens = tokenizer.tokenize(fullStatement);
//                        System.out.println("Tokenized statement: " + Arrays.toString(fullStatementTokens));
//
//                        // Pass the complete statement to executeCommand
//                        executeCommand(fullStatementTokens);
//
//                        // Reset the StringBuilder for the next statement
//                        statementBuilder.setLength(0);
//                    }
//                }
//            }
//
//        }
//
//        // Jump back to the start of the loop to re-evaluate the condition
//        mipsGenerator.addMipsInstruction("j " + loopStart); // No need to generate it every iteration
//
//        // Generate the loop end label (after the loop exits)
//        mipsGenerator.addMipsInstruction(loopEnd + ":");
//    }

    public void generateIfElse(boolean conditionResult, List<String> ifInstructions, List<String> elseInstructions) {
        String conditionReg = allocateTempRegister();
        loadImmediate(conditionReg, conditionResult ? 1 : 0); // Load condition result (1 or 0)

        String ifLabel = generateLabel("IF_BLOCK");
        String elseLabel = generateLabel("ELSE_BLOCK");
        String endLabel = generateLabel("END_IF_ELSE");

        addMipsInstruction("bne " + conditionReg + ", $zero, " + ifLabel);
        addMipsInstruction("j " + elseLabel);

        addLabel(ifLabel);
        for (String instr : ifInstructions) {
            addMipsInstruction(instr);
        }
        addMipsInstruction("j " + endLabel);

        addLabel(elseLabel);
        for (String instr : elseInstructions) {
            addMipsInstruction(instr);
        }

        addLabel(endLabel);
        freeRegister(conditionReg);
    }


    // Generate MIPS code for a 'while' loop
    public void generateWhileLoop(String condition, List<String> loopBody) {
        // Generate a unique label for the start and end of the loop
        String startLabel = generateLabel();
        String endLabel = generateLabel();

        // Start of the while loop
        addMipsInstruction(startLabel + ":");

        // Evaluate condition (you need to generate MIPS code for condition checking)
        String conditionResult = evaluateExpression(condition);
        addMipsInstruction("beq " + conditionResult + ", $zero, " + endLabel); // If condition is false, jump to end of loop

        // Loop body
        for (String instruction : loopBody) {
            addMipsInstruction(instruction);  // Add instructions for the body of the loop
        }

        // Loop iteration (if necessary, for example, incrementing a counter)
        addMipsInstruction("j " + startLabel);  // Jump back to start of the loop

        // End of the while loop
        addMipsInstruction(endLabel + ":");
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
    public void generatePrint(String variable) {
        // Assuming the variable is located at the top of the stack
        // Load the value of 'variable' into $a0
        String reg = symbolTable.getRegister(variable);
        addMipsInstruction("move $a0, " +reg);
        addMipsInstruction("li $v0, 1");         // Syscall code for print integer
        addMipsInstruction("syscall");           // Perform the syscall to print the integer
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
        // Initialize the loop variable
        String loopVar = initialization.split("=")[0].trim(); // Extract variable (e.g., "i")
        String initialValue = initialization.split("=")[1].trim(); // Extract initial value (e.g., "0")
        String register = assignRegister(loopVar);

        // Load the initial value into the assigned register
        addMipsInstruction("li " + register + ", " + initialValue);

        // Start of the loop
        addMipsInstruction("label_8:");

        // Condition check
        addMipsInstruction("# Check condition for " + loopVar);
        addMipsInstruction(condition);

        // Loop body
        for (String bodyToken : bodyTokens) {
            if (bodyToken.equals("print")) {
                generatePrint(loopVar); // Use loop variable name
            }
        }

        // Increment the loop variable
        generateIncrement(loopVar);

        // Jump back to the start of the loop
        addMipsInstruction("j label_8");

        // End of the loop
        addMipsInstruction("label_9:");
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
