/*******************************************************************
 * Evaluator Class                                                  *
 *                                                                  *
 * PROGRAMMER: Emily Culp                                           *
 * COURSE: CS340 - Programming Language Design                      *
 * DATE: 10/29/2024                                                 *
 * REQUIREMENT: Expression Evaluation for Interpreter               *
 *                                                                  *
 * DESCRIPTION:                                                     *
 * The Evaluator class provides methods for evaluating mathematical *
 * expressions, supporting variables and operators following the    *
 * PEMDAS rule. It integrates with the SymbolTable class to resolve *
 * variable values. This class ensures proper handling of           *
 * parentheses and operator precedence using stacks.                *
 *                                                                  *
 * COPYRIGHT: This code is copyright (C) 2024 Emily Culp and Dean   *
 * Zeller.                                                          *
 *                                                                  *
 * CREDITS: This code was written with the help of ChatGPT.         *
 *******************************************************************/

import java.util.*;

public class Evaluator {
    private static final Map<String, Integer> OPERATOR_PRECEDENCE = Map.of(
            "+", 1, "-", 1, "*", 2, "/", 2, "^", 3
    );
    private static SymbolTable symbolTable;
    private static LiteralTable literalTable;

    /**********************************************************
     * CONSTRUCTOR: Evaluator(SymbolTable symbolTable)        *
     * DESCRIPTION: Initializes the Evaluator with a symbol   *
     *              table to retrieve variable values.        *
     * PARAMETERS: SymbolTable symbolTable - the symbol table *
     *              for accessing variables.                  *
     **********************************************************/
    public Evaluator(SymbolTable symbolTable, LiteralTable literalTable) {
        this.symbolTable = symbolTable;
        this.literalTable = literalTable;
    }

    /**********************************************************
     * METHOD: evaluate(String expression)                    *
     * DESCRIPTION: Evaluates a mathematical expression,      *
     *              resolving variables and applying PEMDAS.  *
     * PARAMETERS: String expression - the expression to      *
     *             evaluate, with tokens separated by spaces. *
     * RETURN VALUE: int - the result of the evaluated        *
     *              expression.                               *
     * EXCEPTIONS: Throws an Exception for invalid operations *
     *             or undefined variables.                    *
     **********************************************************/

    // Evaluate a mathematical expression with support for variables and PEMDAS
    public int evaluate(String expression) throws Exception {
        String[] tokens = expression.split(" ");
        Stack<Integer> values = new Stack<>();
        Stack<String> ops = new Stack<>();

        for (String token : tokens) {
            if (isInteger(token)) {
                values.push(Integer.parseInt(token));
            } else if (isVariable(token)) {
                Integer value = symbolTable.getValue(token);
                if (value == null) {
                    throw new RuntimeException("Variable not found: " + token);
                }
                values.push(value);
            } else if (token.equals("(")) {
                ops.push(token);
            } else if (token.equals(")")) {
                while (!ops.isEmpty() && !ops.peek().equals("(")) {
                    values.push(applyOperation(ops.pop(), values.pop(), values.pop()));
                }
                ops.pop(); // pop '('
            } else {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(token)) {
                    values.push(applyOperation(ops.pop(), values.pop(), values.pop()));
                }
                ops.push(token);
            }
        }

        while (!ops.isEmpty()) {
            values.push(applyOperation(ops.pop(), values.pop(), values.pop()));
        }

        int result = values.pop();
        literalTable.addLiteral(result);

        return result;
    }

    /**********************************************************
     * METHOD: applyOperation(String operator, int right,     *
     *                        int left)                       *
     * DESCRIPTION: Applies the specified operator to two     *
     *              operands and returns the result.          *
     * PARAMETERS: String operator - the operator to apply.   *
     *             int right - the right operand.             *
     *             int left - the left operand.               *
     * RETURN VALUE: int - the result of the operation.       *
     * EXCEPTIONS: Throws an Exception for unknown operators. *
     **********************************************************/

    private int applyOperation(String operator, int right, int left) throws Exception {
        if(operator.equals("^")){
            System.out.print(CodeGenerator.START_DEFINE + " ");
            for(int i = 1; i < right; i++){
                System.out.print(CodeGenerator.MULT + " ");
            }
            System.out.println(CodeGenerator.END_DEFINE + " ");
            return (int) Math.pow(left, right);
        }

        Stack<Integer> operands = new Stack<>();
        operands.push(left);
        operands.push(right);
        processOperation(operands, operator);
        return operands.pop(); // Return the result of the operation
    }

    /**********************************************************
     * METHOD: processOperation(Stack<Integer> operands,      *
     *                          String operator)              *
     * DESCRIPTION: Processes the operation by applying the   *
     *              operator to the operands and pushing the  *
     *              result back to the stack.                 *
     * PARAMETERS: Stack<Integer> operands - stack of operands*
     *             String operator - the operator to apply.   *
     * EXCEPTIONS: Throws an Exception for division by zero or*
     *             insufficient operands.                     *
     **********************************************************/

    private void processOperation(Stack<Integer> operands, String operator) throws Exception {
        if (operands.size() < 2) {
            throw new Exception("Insufficient operands for operator: " + operator);
        }

        int right = operands.pop();
        int left = operands.pop();

        switch (operator) {
            case "+" -> operands.push(left + right);
            case "-" -> operands.push(left - right);
            case "*" -> operands.push(left * right);
            case "/" -> {
                if (right == 0) throw new ArithmeticException("Division by zero");
                operands.push(left / right);
            }
            case "^" -> operands.push((int) Math.pow(left, right));
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    /**********************************************************
     * METHOD: precedence(String operator)                    *
     * DESCRIPTION: Returns the precedence of the given       *
     *              operator.                                 *
     * PARAMETERS: String operator - the operator whose       *
     *             precedence is required.                    *
     * RETURN VALUE: int - the precedence value of the        *
     *              operator.                                 *
     **********************************************************/

    private int precedence(String operator) {
        return OPERATOR_PRECEDENCE.getOrDefault(operator, 0);
    }

    /**********************************************************
     * METHOD: isInteger(String token)                        *
     * DESCRIPTION: Checks if the given token is an integer.  *
     * PARAMETERS: String token - the token to check.         *
     * RETURN VALUE: boolean - true if the token is an integer*
     *              otherwise false.                          *
     **********************************************************/

    private boolean isInteger(String token) {
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean evaluateCondition(List<String> tokens, List<Integer> tokenIDs) {
        // Assume the tokens are in the form [a, ==, 0] or [a, !=, b]
        String leftOperand = tokens.get(0);
        String operator = tokens.get(1);
        String rightOperand = tokens.get(2);

        int leftValue = getValue(leftOperand);   // Get value from symbol table or literal
        int rightValue = getValue(rightOperand);

        System.out.println("Left Value: " + leftValue + ", Right Value: " + rightValue);

        switch (operator) {
            case "==":
                return leftValue == rightValue;
            case "!=":
                return leftValue != rightValue;
            case "<":
                return leftValue < rightValue;
            case ">":
                return leftValue > rightValue;
            case "<=":
                return leftValue <= rightValue;
            case ">=":
                return leftValue >= rightValue;
            default:
                return false;  // Invalid operator
        }
    }

    // Helper method to get the value of a token (either from the symbol table or directly as a literal value)
    private static int getValue(String token) {
        // Check if the token is a variable (i.e., it exists in the symbol table)
        if (isVariable(token)) {
            // Fetch the variable's value from the symbol table
            return symbolTable.getValue(token);  // Assuming symbolTable stores variables and their values
        } else {
            // Check if the token is a literal integer
            try {
                return Integer.parseInt(token);  // Parse the token as an integer if it's a literal value
            } catch (NumberFormatException e) {
                System.out.println("Invalid token for getValue: " + token);
                return 0;  // Return a default value or handle the error appropriately
            }
        }
    }

    // Method to check if the token is a variable (you can modify this to check based on your symbol table)
    private static boolean isVariable(String token) {
        return symbolTable.containsVariable(token);  // Assuming symbolTable is a map of variable names to values
    }

}
