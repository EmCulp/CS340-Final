/*******************************************************************
 * MIPS Code Generator for CS340 *
 * *
 * PROGRAMMER: Emily Culp *
 * COURSE: CS340 *
 * DATE: 12/10/2024 *
 * REQUIREMENT: Final - Compiler *
 * *
 * DESCRIPTION: *
 * The MIPSGenerator class is responsible for generating MIPS assembly code. *
 * It handles register allocation, the creation of MIPS instructions, *
 * and the management of a data section for variables and their initialization. *
 * The class includes methods for allocating temporary and saved registers, *
 * freeing registers, and adding variables to the data section with proper initialization. *
 * It also maintains a symbol table for use in MIPS code generation. *
 * *
 * COPYRIGHT: This code is copyright (C) 2024 Emily Culp*
 * and Dean Zeller. *
 * *
 * CREDITS: This code was written with the help of ChatGPT. *
 * *
 *******************************************************************/


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

    /**********************************************************
     * METHOD: addToDataSection(String variableName, String initValue, String dataType) *
     * DESCRIPTION: Adds a variable to the data section with its initialization value. *
     * If the variable is already present, it will not be added again. The method handles *
     * various data types (int, string, boolean, double) and provides default values for *
     * uninitialized variables. *
     * PARAMETERS: *
     * String variableName - the name of the variable to add to the data section. *
     * String initValue - the initialization value for the variable. *
     * String dataType - the data type of the variable (int, string, boolean, double). *
     * RETURN VALUE: void *
     **********************************************************/
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

    /**********************************************************
     * METHOD: isVariableInDataSection(String variableName) *
     * DESCRIPTION: Checks if a variable is already present in the data section. *
     * PARAMETERS: *
     * String variableName - the name of the variable to check. *
     * RETURN VALUE: boolean - true if the variable is in the data section, false otherwise. *
     **********************************************************/
    public boolean isVariableInDataSection(String variableName) {
        return dataSection.containsKey(variableName);
    }

    /**********************************************************
     * METHOD: allocateTempRegister() *
     * DESCRIPTION: Allocates a temporary register from the pool. If no registers are available, *
     * it resets the register pool and attempts to allocate a register again. *
     * PARAMETERS: none *
     * RETURN VALUE: String - the name of the allocated temporary register. *
     **********************************************************/
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

    /**********************************************************
     * METHOD: allocateSavedRegister() *
     * DESCRIPTION: Allocates a saved register from the pool. If no registers are available, *
     * it resets the register pool and attempts to allocate a register again. *
     * PARAMETERS: none *
     * RETURN VALUE: String - the name of the allocated saved register. *
     **********************************************************/
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

    /**********************************************************
     * METHOD: freeRegister() *
     * DESCRIPTION: Frees a register that was previously allocated. If the register is in use, *
     * it is removed from the used registers set and added back to the free registers pool. *
     * PARAMETERS: *
     * String reg - the name of the register to free. *
     * RETURN VALUE: void *
     **********************************************************/
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

    /**********************************************************
     * METHOD: resetRegisterPools() *
     * DESCRIPTION: This method clears the usedRegisters and usedSavedRegisters
     *      sets and reinitializes  the freeRegisters deque with the
     *      original temporary registers. It effectively resets the register
     *      allocation pools to their initial state*
     * PARAMETERS: None*
     * RETURN VALUE: void *
     **********************************************************/
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

    /**********************************************************
     * METHOD: loadRegister(String register, Object operand) *
     * DESCRIPTION: This method checks the type of the operand
     *      and generates the corresponding MIPS instruction to
     *      load the operand into the specified register. It the
     *      operand is a double, the method loads it from the data
     *      section, assuming it has already been stored. For strings,
     *      the address of the string is loaded, and then the
     *      string's value is fetched*
     * PARAMETERS:
     *      String register - the target register to load the operand
     *      into (e.g., $t0, $f2)
     *      Object operand - the value to load into the register. This
     *      can be an integer, double, boolean, or string*
     * EXCEPTION:
     *      Throws IllegalArgumentException if the operand is of an
     *       unsupported type
     * RETURN VALUE: void *
     **********************************************************/
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

    /**********************************************************
     * METHOD: generateUniqueLabelForDouble(double value) *
     * DESCRIPTION: This method generates a unique label by appending
     *      the double value to the prefix double_, which is useful
     *      for referencing double values in the data section of the
     *      generated MIPS code*
     * PARAMETERS: double value - the double value for which to generate
     *      the label*
     * RETURN VALUE: String - A unique label string for the given double
     *      value, in the format double_<value>*
     **********************************************************/
    private String generateUniqueLabelForDouble(double value){
        return "double_" + value;
    }

    /**********************************************************
     * METHOD: loadImmediate(String reg, int value) *
     * DESCRIPTION: This method generates a MIPS li (load immediate)
     *      instruction to load the specified integer value into the
     *      given register*
     * PARAMETERS: String reg : The target register to load the value into (e.g., $t0).
     *      int value : The immediate value to load into the register
     * RETURN VALUE: void*
     **********************************************************/
    public static void loadImmediate(String reg, int value){
        addMipsInstruction("li " +reg+ ", " +value);
    }

    /**********************************************************
     * METHOD: evaluateExpression(String expression, String endLabel) *
     * DESCRIPTION: This method splits the input expression into operands
     *      and an operator, resolves the operands to registers, and generates MIPS
     *      code to perform the operation based on the operator. It supports basic
     *      arithmetic operations (+, -, *, /) and comparison operations
     *      (<, >, ==, !=, <=, >=). It then adds the result to the MIPS code,
     *      followed by a jump to the specified endLabel*
     * PARAMETERS: String expression - the arithmetic expression to evaluate,
     *      formatted as "operand1 operator operand2" (e.g., "x+y")
     *      String endLabel - the label to jump to after evaluating the expression
     *         (used for conditional branches)
     * RETURN VALUE: String - A string containing the MIPS code for evaluating the
     *      expression*
     **********************************************************/
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
                addMipsInstruction("bne " + regResult + ", $zero, " + endLabel); // Branch if equal
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

    /**********************************************************
     * METHOD: mipsAdd(String reg1, String reg2, String regResult) *
     * DESCRIPTION: Generates MIPS assembly code to perform addition between two operands (either registers or an immediate value). *
     * PARAMETERS: *
     *     String reg1 - The first operand (either a register or an integer literal). *
     *     String reg2 - The second operand (either a register or an integer literal). *
     *     String regResult - The register where the result of the addition will be stored. *
     * RETURN VALUE: none *
     **********************************************************/
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

    /**********************************************************
     * METHOD: mipsSub(String reg1, String reg2, String regResult) *
     * DESCRIPTION: Generates MIPS assembly code to perform subtraction between two operands (either registers or an immediate value). *
     * PARAMETERS: *
     *     String reg1 - The first operand (either a register or an integer literal). *
     *     String reg2 - The second operand (either a register or an integer literal). *
     *     String regResult - The register where the result of the subtraction will be stored. *
     * RETURN VALUE: none *
     **********************************************************/

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

    /**********************************************************
     * METHOD: mipsMul(String reg1, String reg2, String regResult) *
     * DESCRIPTION: Generates MIPS assembly code to perform multiplication between two operands (either registers or an immediate value). *
     * PARAMETERS: *
     *     String reg1 - The first operand (either a register or an integer literal). *
     *     String reg2 - The second operand (either a register or an integer literal). *
     *     String regResult - The register where the result of the multiplication will be stored. *
     * RETURN VALUE: none *
     **********************************************************/
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

    /**********************************************************
     * METHOD: mipsDiv(String reg1, String reg2, String regResult) *
     * DESCRIPTION: Generates MIPS assembly code to perform division between two operands (either registers or an immediate value). Throws an ArithmeticException if attempting to divide by zero. *
     * PARAMETERS: *
     *     String reg1 - The first operand (either a register or an integer literal). *
     *     String reg2 - The second operand (either a register or an integer literal). *
     *     String regResult - The register where the result of the division will be stored. *
     * RETURN VALUE: none *
     **********************************************************/
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

    /**********************************************************
     * METHOD: isRegister(String operand) *
     * DESCRIPTION: Checks if a given operand is a register (starts with '$'). *
     * PARAMETERS: *
     *     String operand - The operand to check. *
     * RETURN VALUE: *
     *     boolean - true if the operand is a register, otherwise false. *
     **********************************************************/
    private boolean isRegister(String operand) {
        return operand.startsWith("$"); // Check if the operand is a register (starts with '$')
    }

    /**********************************************************
     * METHOD: resolveToRegister(String operand) *
     * DESCRIPTION: Resolves an operand to a register. If the operand is an integer literal, it returns the literal. If the operand is a variable, it retrieves the corresponding register from the SymbolTable. *
     * PARAMETERS: *
     *    String  operand - The operand to resolve (either a register or a variable). *
     * RETURN VALUE: *
     *    String - The resolved register or immediate value. *
     * EXCEPTION:
     *     Throws IllegalArgumentException if the variable is not found in the SymbolTable. *
     **********************************************************/
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

    /**********************************************************
     * METHOD: isInteger(String token) *
     * DESCRIPTION: Checks if a given string represents an integer. *
     * PARAMETERS: *
     *    String token - The string to check. *
     * RETURN VALUE: *
     *    boolean - true if the string can be parsed as an integer, otherwise false. *
     **********************************************************/
    public boolean isInteger(String token) {
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**********************************************************
     * METHOD: convertConditionToMips(String condition, String label, SymbolTable symbolTable) *
     * DESCRIPTION: Converts a condition (e.g., "x < 5") into MIPS assembly code for comparison and branching. *
     * PARAMETERS: *
     *     String condition - The condition to convert (e.g., "x < 5"). *
     *     String label - The label to jump to if the condition is true. *
     *     SymbolTable symbolTable - The SymbolTable used to resolve variable registers. *
     * RETURN VALUE: *
     *    String - The generated MIPS assembly code as a string. *
     **********************************************************/
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

    /**********************************************************
     * METHOD: generateIfElse(String condition, List<String> ifBodyTokens, List<String> elseBodyTokens) *
     * DESCRIPTION: Generates MIPS assembly code for an if-else structure. The method evaluates a condition and generates MIPS for both the "if" and "else" blocks. *
     * PARAMETERS: *
     * - String condition: A string representing the condition to evaluate. *
     * - List<String> ifBodyTokens: A list of strings representing the tokens for the "if" block. *
     * - List<String> elseBodyTokens: A list of strings representing the tokens for the "else" block. *
     * RETURN VALUE: None *
     **********************************************************/
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

    /**********************************************************
     * METHOD: processBodyTokens(List<String> bodyTokens) *
     * DESCRIPTION: Processes a list of body tokens and generates MIPS code for each token. It handles print statements, assignments, and nested if-else structures. *
     * PARAMETERS: *
     * - List<String> bodyTokens: A list of strings representing the tokens in the body of a control structure. *
     * RETURN VALUE: None *
     **********************************************************/
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

    /**********************************************************
     * METHOD: extractPrintMessage(String token) *
     * DESCRIPTION: Extracts the message to be printed from a print statement token. *
     * PARAMETERS: *
     * - String token: A string representing the print statement, e.g., print("message"). *
     * RETURN VALUE: String - A string containing the message to be printed. *
     **********************************************************/
    private String extractPrintMessage(String token) {
        // Assuming format: print("message")
        int startIndex = token.indexOf("\"") + 1;
        int endIndex = token.lastIndexOf("\"");
        return token.substring(startIndex, endIndex);
    }

    /**********************************************************
     * METHOD: generateAssignmentInstruction(String variable, String value) *
     * DESCRIPTION: Generates MIPS assembly code to assign a value to a variable. The method handles integer assignment. *
     * PARAMETERS: *
     * - String variable: A string representing the variable name to which the value will be assigned. *
     * - String value: A string representing the value to assign to the variable. *
     * RETURN VALUE: None *
     **********************************************************/
    private void generateAssignmentInstruction(String variable, String value) {
        // Assuming integer assignment
        addMipsInstruction("li $t0, " + value); // Load immediate value into a temporary register
        addMipsInstruction("sw $t0, " + variable); // Store the value into the variable's memory address
    }

    /**********************************************************
     * METHOD: extractIfBodyTokens(String token) *
     * DESCRIPTION: Extracts the tokens for the "if" block from a given if statement string. *
     * PARAMETERS: *
     * - String token: A string representing the if statement containing the body in curly braces. *
     * RETURN VALUE: List<String> - A list of strings representing the tokens in the "if" body. *
     **********************************************************/
    private List<String> extractIfBodyTokens(String token) {
        // Extracts tokens between `{` and `}` of the `if` block
        int start = token.indexOf("{") + 1;
        int end = token.indexOf("}");
        String body = token.substring(start, end).trim();
        return Arrays.asList(body.split(";")); // Assuming semicolon-separated statements
    }


    /**********************************************************
     * METHOD: extractElseBodyTokens(String token) *
     * DESCRIPTION: Extracts the tokens for the "else" block from a given if-else statement string. *
     * PARAMETERS: *
     * - String - token: A string representing the if-else statement containing the body in curly braces. *
     * RETURN VALUE: List<String> - A list of strings representing the tokens in the "else" body. If no "else" block exists, returns an empty list. *
     **********************************************************/
    private List<String> extractElseBodyTokens(String token) {
        // Extracts tokens for the `else` block, if it exists
        int elseIndex = token.indexOf("else");
        if (elseIndex == -1) return new ArrayList<>();
        int start = token.indexOf("{", elseIndex) + 1;
        int end = token.indexOf("}", elseIndex);
        String body = token.substring(start, end).trim();
        return Arrays.asList(body.split(";")); // Assuming semicolon-separated statements
    }

    /**********************************************************
     * METHOD: generateWhileLoop(String condition, List<String> bodyTokens) *
     * DESCRIPTION: Generates MIPS assembly code for a while loop. The method processes the loop condition and body tokens, generating the appropriate assembly instructions. *
     * PARAMETERS: *
     * - String condition: A string representing the loop condition. *
     * - List<String> - bodyTokens: A list of strings representing the tokens in the body of the while loop. *
     * RETURN VALUE: None *
     **********************************************************/
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

        // Start of the loop
        String startLabel = "label_6";
        String endLabel = "label_7";
        addMipsInstruction(startLabel + ":");

        // Condition check using the evaluateExpression method
        addComment("Check condition for " + loopVar + " " + operator + " " + constant);
        evaluateExpression(condition, endLabel);

        // Loop body
        // Inside the loop body processing
        for (String bodyToken : bodyTokens) {
            bodyToken = bodyToken.trim();

            if (bodyToken.contains("=")) {
                // Assignment statement with an arithmetic expression (e.g., y = y + 1)
                String[] statementParts = bodyToken.split("=");
                String leftSide = statementParts[0].trim();
                String rightSide = statementParts[1].trim().replace(";", ""); // Remove semicolon

                // Resolve the register for the left-hand side variable
                String leftRegister = symbolTable.getRegisterForVariable(leftSide);
                if (leftRegister == null) {
                    throw new IllegalStateException("Variable '" + leftSide + "' not found in symbol table");
                }

                // Handle cases like y = y + 1
                if (rightSide.contains("+") || rightSide.contains("-")) {
                    // For expressions like "y + 1" or "y - 1"
                    String[] operands = rightSide.split("\\+|\\-");
                    String operand1 = operands[0].trim();
                    String operand2 = operands[1].trim();

                    // Get the registers for operands
                    String operand1Register = symbolTable.getRegisterForVariable(operand1);
                    String operand2Register = assignRegister(operand2); // Register for the constant value, e.g., 1

                    if (operand1Register == null) {
                        throw new IllegalStateException("Operand variable '" + operand1 + "' not found in symbol table");
                    }

                    String resultRegister = evaluateExpression(rightSide, endLabel);

                    // Store the result back into the left-hand side variable register
                    addMipsInstruction("move " + leftRegister + ", " + resultRegister);
                } else {
                    // Handle simple assignments without arithmetic expressions
                    String resultRegister = evaluateExpression(rightSide, endLabel); // Evaluate the arithmetic expression
                    addMipsInstruction("move " + leftRegister + ", " + resultRegister);
                }
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

    /**********************************************************
     * METHOD: handlePrintStatement(String statement) *
     * DESCRIPTION: Handles the print statement by extracting the variable name and generating MIPS code to print its value. *
     * PARAMETERS: String statement - The print statement to handle, in the form of "print(variable)". The variable should be extracted and printed using its corresponding register. *
     * RETURN VALUE: None *
     **********************************************************/
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

    /**********************************************************
     * METHOD: assignRegister(String variable) *
     * DESCRIPTION: Assigns a register for a given variable. If the variable is a constant, it is handled differently by loading the immediate value into a register. *
     * PARAMETERS: String variable - The variable for which a register is assigned. *
     * RETURN VALUE: String - The register that holds the value of the variable. *
     **********************************************************/
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

    /**********************************************************
     * METHOD: isConstant(String variable) *
     * DESCRIPTION: Helper method to check if a variable is a constant (numeric value). *
     * PARAMETERS: String variable - The variable to check. *
     * RETURN VALUE: boolean - True if the variable is a constant, false otherwise. *
     **********************************************************/
    // Helper method to check if a variable is a constant (numeric value)
    private boolean isConstant(String variable) {
        try {
            Integer.parseInt(variable);  // Try to parse the variable as an integer
            return true;  // It's a constant if it parses successfully
        } catch (NumberFormatException e) {
            return false;  // Not a constant if it throws an exception
        }
    }

    /**********************************************************
     * METHOD: generatePrint(String register) *
     * DESCRIPTION: Generates MIPS code to print an integer using the value stored in the given register. *
     * PARAMETERS: String register - The register containing the integer value to be printed. *
     * RETURN VALUE: None *
     **********************************************************/
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

    /**********************************************************
     * METHOD: handleIncrementOrDecrement(String expression) *
     * DESCRIPTION: Handles increment or decrement operations (e.g., "i++" or "i--"). This method extracts the variable and calls generateIncrementOrDecrement for processing. *
     * PARAMETERS: String expression - The expression to process, which can be either an increment (++) or decrement (--). *
     * RETURN VALUE: None *
     **********************************************************/
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

    /**********************************************************
     * METHOD: generateIncrementOrDecrement(String variable, boolean isIncrement) *
     * DESCRIPTION: Generates MIPS code to increment or decrement a variable. *
     * PARAMETERS: String variable - The variable to increment or decrement. *
     *             boolean isIncrement - True for increment, false for decrement. *
     * RETURN VALUE: None *
     **********************************************************/
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

    /**********************************************************
     * METHOD: generateForLoop(String initialization, String condition, String increment, List<String> bodyTokens) *
     * DESCRIPTION: Generates MIPS code for a for loop, including initialization, condition check, body execution, and increment/decrement operation. *
     * PARAMETERS: String initialization - The initialization statement for the loop (e.g., "i = 0"). *
     *            String condition - The loop condition to check (e.g., "i < 10"). *
     *            String increment - The increment or decrement operation (e.g., "i++"). *
     *            List<String>  bodyTokens - The list of tokens representing the body of the loop. *
     * RETURN VALUE: None *
     **********************************************************/
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

    /**********************************************************
     * METHOD: processBodyToken(String bodyToken) *
     * DESCRIPTION: Processes each token in the body of the loop, handling different types of statements such as variable declarations, assignments, and control structures. *
     * PARAMETERS: String bodyToken - The token representing a statement in the loop body. *
     * RETURN VALUE: None *
     **********************************************************/
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

    /**********************************************************
     * METHOD: generateMove(String destinationRegister, String sourceRegister) *
     * DESCRIPTION: Generates a MIPS move instruction, which either loads an immediate value or moves a value from one register to another. *
     * PARAMETERS: String destinationRegister - The register where the value will be moved. *
     *             String sourceRegister - The register or literal value to be moved. *
     * RETURN VALUE: None *
     **********************************************************/
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

    /**********************************************************
     * METHOD: extractCondition(String token) *
     * DESCRIPTION: Extracts the condition from an 'if' or 'while' statement, enclosed within parentheses. *
     * PARAMETERS: String token - The token representing the 'if' or 'while' statement, containing the condition. *
     * RETURN VALUE: String - The condition as a string, extracted from the parentheses. *
     **********************************************************/
    // Extract condition from an 'if' or 'while' statement
    private String extractCondition(String token) {
        int start = token.indexOf("(") + 1;
        int end = token.indexOf(")");
        if (start == -1 || end == -1) {
            throw new IllegalArgumentException("Condition not found in token: " + token);
        }
        return token.substring(start, end).trim();
    }

    /**********************************************************
     * METHOD: extractIfTokens(String bodyToken) *
     * DESCRIPTION: Extracts the individual statements from the body of an 'if' block. Assumes the block follows the format "if(condition) { ... } else { ... }". *
     * PARAMETERS: String bodyToken - The token representing the body of the 'if' block, including the 'if' and 'else' sections. *
     * RETURN VALUE: List<String> - A list of strings, each representing an individual statement in the 'if' block. *
     **********************************************************/
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

    /**********************************************************
     * METHOD: extractElseTokens(String bodyToken) *
     * DESCRIPTION: Extracts the individual statements from the body of the 'else' block, if it exists. *
     * PARAMETERS: String bodyToken - The token representing the body of the 'else' block. *
     * RETURN VALUE: List<String> - A list of strings, each representing an individual statement in the 'else' block, or an empty list if no 'else' block exists. *
     **********************************************************/
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

    /**********************************************************
     * METHOD: extractBodyTokens(String token) *
     * DESCRIPTION: Extracts the body (statements inside curly braces) from an 'if' or 'while' statement. *
     * PARAMETERS: String token - The token representing the 'if' or 'while' statement, containing the body inside curly braces. *
     * RETURN VALUE: List<String> - A list of strings, each representing an individual statement inside the curly braces. *
     **********************************************************/
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

    /**********************************************************
     * METHOD: declareVariable(String variableName, String value, boolean inMainMethod) *
     * DESCRIPTION: Declares a variable and assigns it a value. If the variable is in the main method, the value is loaded into a register. *
     * PARAMETERS: String variableName - The name of the variable being declared. *
     *             String value - The value to assign to the variable. *
     *             boolean inMainMethod - A flag indicating whether the variable is in the main method. *
     * RETURN VALUE: None *
     **********************************************************/
    public void declareVariable(String variableName, String value, boolean inMainMethod) {
        String reg = symbolTable.getRegister(variableName);

        if (inMainMethod) {
            // If the variable should be in the main method, use the li instruction
            addMipsInstruction("li " + reg + ", " + value);  // Load immediate value to the register
        }
    }

    /**********************************************************
     * METHOD: generateLabel() *
     * DESCRIPTION: Generates a unique label for use in the MIPS code. The label is prefixed with "label_" followed by an incrementing counter. *
     * PARAMETERS: None *
     * RETURN VALUE: String - A unique label as a string. *
     **********************************************************/
    private String generateLabel() {
        return "label_" + labelCounter++;  // Increment and return a unique label
    }

    /**********************************************************
     * METHOD: addMipsInstruction(String instruction) *
     * DESCRIPTION: Adds a MIPS instruction to the list of generated MIPS code. *
     * PARAMETERS: String instruction - The MIPS instruction to add to the code. *
     * RETURN VALUE: None *
     **********************************************************/
    // Add an instruction to the list of generated MIPS code
    public static void addMipsInstruction(String instruction) {
        mipsCode.add(instruction);
    }

    /**********************************************************
     * METHOD: addComment(String comment) *
     * DESCRIPTION: Adds a comment to the list of generated MIPS code. *
     * PARAMETERS: String comment - The comment to add. *
     * RETURN VALUE: None *
     **********************************************************/
    public void addComment(String comment){
        mipsCode.add("# " +comment);
    }

    /**********************************************************
     * METHOD: generateDataSection() *
     * DESCRIPTION: Prints the data section of the MIPS code, including all entries from the data section map. *
     * PARAMETERS: None *
     * RETURN VALUE: None *
     **********************************************************/
    public void generateDataSection(){
        System.out.println();
        System.out.println(".data");
        for(String entry : dataSection.values()){
            System.out.println(entry);
        }
    }

    /**********************************************************
     * METHOD: printMipsCode() *
     * DESCRIPTION: Prints the generated MIPS code from the list of instructions. It includes the main section of the code. *
     * PARAMETERS: None *
     * RETURN VALUE: None *
     **********************************************************/
    // Print the generated MIPS code
    public void printMipsCode() {
        System.out.println();
        System.out.println(".main");
        for (String instruction : mipsCode) {
            System.out.println(instruction);
        }
    }
}
