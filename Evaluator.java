/*******************************************************************
 * Evaluator Class                                                  *
 *                                                                  *
 * PROGRAMMER: Emily Culp                                           *
 * COURSE: CS340 - Programming Language Design                      *
 * DATE: 11/12/2024                                                 *
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
    private MIPSGenerator mipsGenerator;

    public Evaluator(MIPSGenerator mipsGenerator){
        this.mipsGenerator = mipsGenerator;
    }

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
    public Object evaluate(String expression) {
        Stack<Object> values = new Stack<>();
        Stack<Character> ops = new Stack<>();

        String[] tokens = expression.split("\\s+");

        for (String token : tokens) {
            if (isInteger(token)) {
                int value = Integer.parseInt(token);
                values.push(value);
                literalTable.addLiteral(value); // Assuming you have an addLiteral() method
            } else if (symbolTable.containsVariable(token)) {
                Integer id = symbolTable.getIdByName(token);
                if (id != null) {
                    Object value = symbolTable.getValueById(id);
                    values.push(value);
                } else {
                    throw new IllegalArgumentException("Variable '" + token + "' not found.");
                }
            } else if (isOperator(token.charAt(0))) {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(token.charAt(0))) {
                    values.push(applyOperation(ops.pop(), values.pop(), values.pop()));
                }
                ops.push(token.charAt(0));
            }
        }

        while (!ops.isEmpty()) {
            values.push(applyOperation(ops.pop(), values.pop(), values.pop()));
        }

        return values.pop();
    }

    private boolean isOperator(char c){
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    private Object applyOperation(char op, Object b, Object a) {
        if (!(a instanceof Integer) || !(b instanceof Integer)) {
            throw new IllegalArgumentException("Only integer operations are supported.");
        }

        int x = (int) a;
        int y = (int) b;

        switch (op) {
            case '+':
                return x + y;
            case '-':
                return x - y;
            case '*':
                return x * y;
            case '/':
                if (y == 0) throw new ArithmeticException("Cannot divide by zero.");
                return x / y;
        }
        return 0;
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

    private int precedence(char op) {
        return OPERATOR_PRECEDENCE.getOrDefault(op, 0);
    }

    /**********************************************************
     * METHOD: isInteger(String token)                        *
     * DESCRIPTION: Checks if the given token is an integer.  *
     * PARAMETERS: String token - the token to check.         *
     * RETURN VALUE: boolean - true if the token is an integer*
     *              otherwise false.                          *
     **********************************************************/

    public boolean isInteger(String token) {
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public Object evaluateExpression(String expression) {
        Stack<Object> values = new Stack<>();
        Stack<Character> ops = new Stack<>();

        String[] tokens = expression.split("\\s+");

        for (String token : tokens) {
            if (isInteger(token)) {
                values.push(Integer.parseInt(token));
            } else if (symbolTable.containsVariable(token)) {
                Integer id = symbolTable.getIdByName(token);
                if (id != null) {
                    Object value = symbolTable.getValueById(id);
                    values.push(value);
                }
            } else if (isOperator(token.charAt(0))) {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(token.charAt(0))) {
                    values.push(applyOperation(ops.pop(), values.pop(), values.pop()));
                }
                ops.push(token.charAt(0));
            }
        }

        while (!ops.isEmpty()) {
            values.push(applyOperation(ops.pop(), values.pop(), values.pop()));
        }

        return values.pop();
    }


    /**********************************************************
     * METHOD: evaluateCondition(List<String> tokens, List<Integer> tokenIDs)*
     * DESCRIPTION: Evaluates a condition (e.g., equality, inequality, comparison)*
     *              based on the provided tokens. The condition can check for equality, *
     *              inequality, and relational operators such as <, >, <=, >=.          *
     * PARAMETERS: List<String> tokens - A list of tokens representing the condition,   *
     *             where the first token is the left operand, the second token is the   *
     *             operator, and the third token is the right operand.                 *
     *             List<Integer> tokenIDs - A list of token IDs (not used in this method).*
     * RETURN VALUE: boolean - Returns true if the condition is met, otherwise false.   *
     * EXCEPTIONS: None                                                      *
     **********************************************************/

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

    private int getValue(String operand) {
        // If the operand is a variable, get its value from the symbol table
        if (symbolTable.containsVariable(operand)) {
            Integer id = symbolTable.getIdByName(operand);
            return (int) symbolTable.getValueById(id); // Assuming getValueById returns an int
        }
        // If it's a literal (e.g., a number), return it directly
        else {
            return Integer.parseInt(operand); // Convert string literals to integers
        }
    }


    /**********************************************************
     * METHOD: isVariable(String token)                          *
     * DESCRIPTION: Checks if the token is a variable by searching*
     *              the symbol table for its existence.          *
     * PARAMETERS: String token - The token to check for variable status. *
     * RETURN VALUE: boolean - Returns true if the token is a variable, false otherwise.  *
     * EXCEPTIONS: None                                           *
     **********************************************************/

    // Method to check if the token is a variable (you can modify this to check based on your symbol table)
    static boolean isVariable(String token) {
        boolean exists = symbolTable.containsVariable(token);
        System.out.println("Checking if variable exists: " +token+ " => " +exists);
        return exists;  // Assuming symbolTable is a map of variable names to values
    }

}
