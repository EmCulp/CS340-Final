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
    private int stackPointer = 0;
    private Map<String, Integer> stackMap;
    private int stackOffset = -4;
    private Map<String, String> dataSection = new HashMap<>();

    public MIPSGenerator() {
        // Initialize register pools
        tempRegisters = new ArrayDeque<>(List.of("$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9"));
        savedRegisters = new ArrayDeque<>(List.of("$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7"));
        tempFloatRegisters = new ArrayDeque<>(List.of("$f0", "$f2", "$f4", "$f6", "$f8", "$f10", "$f12", "$f14", "$f16", "$f18"));
        savedFloatRegisters = new ArrayDeque<>(List.of("$f20", "$f22", "$f24", "$f26", "$f28", "$f30"));
        usedRegisters = new HashSet<>();
        mipsCode = new ArrayList<>();  // Initialize the list to store MIPS code
        stackMap = new HashMap<>();
        stackPointer = 0;
    }

    public void RegisterManager(){
        for(String reg : tempRegisters){
            freeRegisters.offer(reg);
        }
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

    public void pushToStack(String register){
        stackOffset -= 4;
        addMipsInstruction("addi $sp, $sp, -" +stackOffset);
        addMipsInstruction("sw " +register+ ", -" +stackOffset+"($sp)");
    }

    public void popFromStack(String register){
        addMipsInstruction("lw " +register+ ", " +stackOffset+ "($sp)");
        addMipsInstruction("addi $sp, $sp, " +stackOffset);
        stackPointer += 4;
    }

    public void allocateVariable(String variableName){
        stackPointer -= 4;
        stackMap.put(variableName, stackPointer);
        addMipsInstruction("# Allocating variable " +variableName+ " at offset " +stackPointer);
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

    // Method to allocate a temporary register
    public String allocateTempRegister() {
        for (String reg : tempRegisters) {
            if (!usedRegisters.contains(reg)) {
                usedRegisters.add(reg);
                addMipsInstruction("# Allocating temporary register: " + reg);
                return reg;
            }
        }
        throw new RuntimeException("No available temporary registers");
    }

    // Method to allocate a saved register
    public String allocateSavedRegister() {
        for (String reg : savedRegisters) {
            if (!usedRegisters.contains(reg)) {
                usedRegisters.add(reg);
                addMipsInstruction("# Allocating saved register: " + reg);
                return reg;
            }
        }
        throw new RuntimeException("No available saved registers");
    }

    // Method to allocate a temporary floating-point register
    public String allocateTempFloatRegister() {
        for (String reg : tempFloatRegisters) {
            if (!usedRegisters.contains(reg)) {
                usedRegisters.add(reg);
                addMipsInstruction("# Allocating temporary floating-point register: " + reg);
                return reg;
            }
        }
        throw new RuntimeException("No available temporary floating-point registers");
    }

    // Method to allocate a saved floating-point register
    public String allocateSavedFloatRegister() {
        for (String reg : savedFloatRegisters) {
            if (!usedRegisters.contains(reg)) {
                usedRegisters.add(reg);
                addMipsInstruction("# Allocating saved floating-point register: " + reg);
                return reg;
            }
        }
        throw new RuntimeException("No available saved floating-point registers");
    }

    // Method to free a register
    public void freeRegister(String reg) {
        if (usedRegisters.contains(reg)) {
            usedRegisters.remove(reg);
            addMipsInstruction("# Register freed: " + reg);
        } else {
            addMipsInstruction("# Warning: Attempted to free an unallocated register: " + reg);
        }
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

    // Helper method to check if operand is an integer
    public boolean isInteger(String operand) {
        return operand != null && operand.matches("-?\\d+");
    }

    // Helper method to check if operand is a double (or float in MIPS terms)
    public boolean isDouble(String operand) {
        return operand != null && operand.matches("-?\\d*\\.\\d+");
    }

    // Helper method to check if operand is a boolean
    public boolean isBoolean(String operand) {
        return operand != null && (operand.equals("true") || operand.equals("false"));
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


    // Example usage in assignment generation
    public void generateAssignment(String variable, String expression) {
        String reg = allocateSavedRegister(); // Allocate a saved register for the variable

        // Assuming `evaluateExpression` generates MIPS code for the expression
        String resultRegister = evaluateExpression(expression);

        // Store the result in the allocated register
        addMipsInstruction("# Assigning result of expression to " + variable);
        addMipsInstruction("move " + reg + ", " + resultRegister);

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

    // Generate an arithmetic operation for doubles or integers
    public String generateArithmeticOperation(String operator, Object operand1, Object operand2, Object result) {
        String reg1 = allocateTempRegister();
        String reg2 = allocateTempRegister();
        String regResult = allocateTempRegister();

        // Load the operands into registers
        loadRegister(reg1, operand1);  // Load operand1 into reg1
        loadRegister(reg2, operand2);  // Load operand2 into reg2

        // Integer operations (add, sub, mul, div, and immediate versions)
        if (operand1 instanceof Integer && operand2 instanceof Integer) {
            switch (operator) {
                case "+":
                    if (isImmediate(operand2)) {
                        addMipsInstruction("addi " + regResult + ", " + reg1 + ", " + operand2);
                    } else {
                        addMipsInstruction("add " + regResult + ", " + reg1 + ", " + reg2);
                    }
                    break;
                case "-":
                    if (isImmediate(operand2)) {
                        addMipsInstruction("subi " + regResult + ", " + reg1 + ", " + operand2);
                    } else {
                        addMipsInstruction("sub " + regResult + ", " + reg1 + ", " + reg2);
                    }
                    break;
                case "*":
                    addMipsInstruction("mul " + regResult + ", " + reg1 + ", " + reg2);
                    break;
                case "/":
                    addMipsInstruction("div " + reg1 + ", " + reg2);  // Integer division
                    addMipsInstruction("mflo " + regResult);          // Move the result to the destination register
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported operator for integers: " + operator);
            }
        } else {
            throw new IllegalArgumentException("Unsupported operand types: " + operand1.getClass() + ", " + operand2.getClass());
        }

        // Store result into the destination object (assuming it's an object that can hold the result)
        addMipsInstruction("# Store result of operation into " + result);

        // Free the registers after use
        freeRegister(reg1);
        freeRegister(reg2);
        freeRegister(regResult);

        // Return the register where the result is stored
        return regResult;
    }

    // Helper method to check if the operand is an immediate value
    private boolean isImmediate(Object operand) {
        return operand instanceof Integer;
    }


    public String generateConditional(String operator, Object operand1, Object operand2) {
        String reg1 = allocateTempRegister();  // Load operand1
        String reg2 = allocateTempRegister();  // Load operand2

        loadRegister(reg1, operand1);  // Load operand1 value
        loadRegister(reg2, operand2);  // Load operand2 value

        // Conditional checks for boolean or numeric operands
        if (operand1 instanceof Boolean || operand2 instanceof Boolean) {
            // Handle boolean conditionals (true/false)
            switch (operator) {
                case "==":
                    addMipsInstruction("beq " + reg1 + ", " + reg2 + ", label_if_true");
                    break;
                case "!=":
                    addMipsInstruction("bne " + reg1 + ", " + reg2 + ", label_if_true");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported boolean operator: " + operator);
            }
        } else if (operand1 instanceof Integer || operand1 instanceof Double) {
            // Handle integer or floating-point conditionals
            switch (operator) {
                case "==":
                    addMipsInstruction("beq " + reg1 + ", " + reg2 + ", label_if_true");
                    break;
                case "!=":
                    addMipsInstruction("bne " + reg1 + ", " + reg2 + ", label_if_true");
                    break;
                case "<":
                    addMipsInstruction("blt " + reg1 + ", " + reg2 + ", label_if_true");
                    break;
                case ">":
                    addMipsInstruction("bgt " + reg1 + ", " + reg2 + ", label_if_true");
                    break;
                case "<=":
                    addMipsInstruction("ble " + reg1 + ", " + reg2 + ", label_if_true");
                    break;
                case ">=":
                    addMipsInstruction("bge " + reg1 + ", " + reg2 + ", label_if_true");
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

    // Add an instruction to the list of generated MIPS code
    public static void addMipsInstruction(String instruction) {
        mipsCode.add(instruction);
    }

    public String appendToMIPSOutput(String mipsCode){
        StringBuilder mipsOutput = new StringBuilder();
        mipsOutput.append(mipsCode).append("\n");
        return mipsOutput.toString();
    }

    public static String generateMIPSForStatement(String[] statementTokens) throws Exception{
        return "# MIPS code for: " +String.join(" ", statementTokens);
    }

    public static String generateUniqueLabel(String prefix){
        labelCounter++;
        return prefix+"_"+labelCounter;
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
