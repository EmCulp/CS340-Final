import java.util.*;

public class MIPSGenerator {
    private Deque<String> tempRegisters;
    private Deque<String> savedRegisters;
    private Deque<String> argRegisters;
    private String[] returnRegisters = {"$v0", "$v1"};
    private Set<String> usedRegisters;

    public MIPSGenerator() {
        // Initialize register pools
        tempRegisters = new ArrayDeque<>(List.of("$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9"));
        savedRegisters = new ArrayDeque<>(List.of("$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7"));
        argRegisters = new ArrayDeque<>(List.of("$a0", "$a1", "$a2", "$a3"));
        usedRegisters = new HashSet<>();
    }

    // Method to allocate a temporary register
    public String allocateTempRegister() {
        for (String reg : tempRegisters) {
            if (!usedRegisters.contains(reg)) {
                usedRegisters.add(reg);
                System.out.println("# Allocating temporary register: " + reg);
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
                System.out.println("# Allocating saved register: " + reg);
                return reg;
            }
        }
        throw new RuntimeException("No available saved registers");
    }

    // Method to allocate an argument register
    public String allocateArgRegister() {
        for (String reg : argRegisters) {
            if (!usedRegisters.contains(reg)) {
                usedRegisters.add(reg);
                System.out.println("# Allocating argument register: " + reg);
                return reg;
            }
        }
        throw new RuntimeException("No available argument registers");
    }

    // Method to free a register
    public void freeRegister(String reg) {
        if (usedRegisters.contains(reg)) {
            usedRegisters.remove(reg);
            System.out.println("# Register freed: " + reg);
        } else {
            System.out.println("# Warning: Attempted to free an unallocated register: " + reg);
        }
    }

    public void loadRegister(String register, String operand) {
        if(operand == null){
            throw new IllegalArgumentException("Operand cannot be null");
        }

        if (isInteger(operand)) {
            // If the operand is an immediate value (integer), load it directly
            System.out.println("li " + register + ", " + operand);  // li = Load immediate
        } else {
            // If the operand is a variable, assume it has a memory address (symbolic handling)
            String addressRegister = allocateTempRegister();  // Temporarily hold the memory address
            System.out.println("la " + addressRegister + ", " + operand);  // Load address of the variable
            System.out.println("lw " + register + ", 0(" + addressRegister + ")");  // Load word into register
            freeRegister(addressRegister);
        }
    }

    public boolean isInteger(String operand){
        //includes optional leading minus sign
        return operand != null && operand.matches("-?\\d+");
    }

    // Example usage in assignment generation
    public void generateAssignment(String variable, String expression) {
        String reg = allocateSavedRegister(); // Allocate a saved register for the variable

        // Assuming `evaluateExpression` generates MIPS code for the expression
        String resultRegister = evaluateExpression(expression);

        // Store the result in the allocated register
        System.out.println("# Assigning result of expression to " + variable);
        System.out.println("move " + reg + ", " + resultRegister);

        // Free the temporary register if no longer needed
        freeRegister(resultRegister);
    }

    // Example evaluateExpression method (simplified)
    private String evaluateExpression(String expression) {
        // In a real implementation, this would generate MIPS code for the expression
        // Here we simulate it with a temporary register allocation
        String tempReg = allocateTempRegister();
        System.out.println("# Evaluating expression: " + expression + " into " + tempReg);
        return tempReg;
    }

    public void generateArithmeticOperation(String operator, String operand1, String opearnd2, String result){
        String reg1 = allocateTempRegister();
        String reg2 = allocateTempRegister();
        String regResult = allocateTempRegister();

        loadRegister(reg1, operand1);
        loadRegister(reg2, opearnd2);

        switch (operator){
            case "+":
                System.out.println("add " +regResult+ ", " +reg1+ ", " +reg2);
                break;
            case "-":
                System.out.println("sub " +regResult+ ", " +reg1+ ", " +reg2);
                break;
            case "*":
                System.out.println("mul " + regResult + ", " + reg1 + ", " + reg2);
                break;
            case "/":
                System.out.println("div " + regResult + ", " + reg1);
                System.out.println("mflo " + regResult);
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
        System.out.println("# Store result of operation into " +result);

        freeRegister(reg1);
        freeRegister(reg2);
        freeRegister(regResult);
    }

    public void generateConditional(String operator, String operand1, String operand2) {
        String reg1 = allocateTempRegister();  // Load operand1
        String reg2 = allocateTempRegister();  // Load operand2

        // Assuming operand1 and operand2 have already been loaded into reg1 and reg2
        loadRegister(reg1, operand1);  // Load operand1 value
        loadRegister(reg2, operand2);  // Load operand2 value

        switch (operator) {
            case "==":
                System.out.println("beq " + reg1 + ", " + reg2 + ", label_if_true");
                break;
            case "!=":
                System.out.println("bne " + reg1 + ", " + reg2 + ", label_if_true");
                break;
            case "<":
                System.out.println("slt $t3, " + reg1 + ", " + reg2);  // Use a temp register to store result
                System.out.println("bne $t3, $zero, label_if_true");
                break;
            case ">":
                System.out.println("slt $t3, " + reg2 + ", " + reg1);  // Swapped operands for greater than
                System.out.println("bne $t3, $zero, label_if_true");
                break;
            case "<=":
                System.out.println("slt $t3, " + reg2 + ", " + reg1);  // Check if reg2 < reg1
                System.out.println("beq $t3, $zero, label_if_true");
                break;
            case ">=":
                System.out.println("slt $t3, " + reg1 + ", " + reg2);  // Check if reg1 < reg2
                System.out.println("beq $t3, $zero, label_if_true");
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }

        freeRegister(reg1);
        freeRegister(reg2);
    }

}
