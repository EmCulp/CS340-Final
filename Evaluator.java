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


    /**********************************************************
     * CONSTRUCTOR: Evaluator(SymbolTable symbolTable)        *
     * DESCRIPTION: Initializes the Evaluator with a symbol   *
     *              table to retrieve variable values.        *
     * PARAMETERS: SymbolTable symbolTable - the symbol table *
     *              for accessing variables.                  *
     **********************************************************/
    public Evaluator(SymbolTable symbolTable, LiteralTable literalTable, MIPSGenerator mipsGenerator) {
        this.symbolTable = symbolTable;
        this.literalTable = literalTable;
        this.mipsGenerator = mipsGenerator;
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

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    private Object applyOperation(char op, Object b, Object a) {
        double x;
        double y;

        // Convert operand 'a' to double if it's Integer or Double
        if (a instanceof Integer) {
            x = ((Integer) a).doubleValue();
        } else if (a instanceof Double) {
            x = (Double) a;
        } else {
            throw new IllegalArgumentException("Unsupported data type for operand a. Only Integer and Double are supported.");
        }

        // Convert operand 'b' to double if it's Integer or Double
        if (b instanceof Integer) {
            y = ((Integer) b).doubleValue();
        } else if (b instanceof Double) {
            y = (Double) b;
        } else {
            throw new IllegalArgumentException("Unsupported data type for operand b. Only Integer and Double are supported.");
        }

        // Perform the operation
        double result;
        switch (op) {
            case '+':
                result = x + y;
                break;
            case '-':
                result = x - y;
                break;
            case '*':
                result = x * y;
                break;
            case '/':
                if (y == 0) throw new ArithmeticException("Cannot divide by zero.");
                result = x / y;
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + op);
        }

        mipsGenerator.generateArithmeticOperation(String.valueOf(op), x, y, result);

        // Return result as Integer if both operands were Integer
        if (a instanceof Integer && b instanceof Integer) {
            return (int) result;
        }

        // Otherwise, return as Double
        return result;
    }

    private int precedence(char op) {
        return OPERATOR_PRECEDENCE.getOrDefault(op, 0);
    }

    public boolean isInteger(String token) {
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isDouble(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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

    public boolean evaluateCondition(String[] conditionTokens) throws Exception {
        if (conditionTokens.length < 3) {
            throw new Exception("Invalid condition. Condition requires a left operand, operator, and right operand.");
        }

        // Extract the left operand, operator, and right operand
        String leftOperand = conditionTokens[0].trim();
        String operator = conditionTokens[1].trim();
        String rightOperand = conditionTokens[2].trim();

        System.out.println("Evaluating condition: " + leftOperand + " " + operator + " " + rightOperand);

        // Get the values of the operands from the SymbolTable or as literals
        Object leftValue = getValueFromOperand(leftOperand);
        Object rightValue = getValueFromOperand(rightOperand);

        // Handle null values in operands
        if (leftValue == null || rightValue == null) {
            throw new Exception("One or both operands are null: " + leftOperand + ", " + rightOperand);
        }

        System.out.println("Left Operand Value: " + leftValue + " (type: " + leftValue.getClass().getSimpleName() + ")");
        System.out.println("Right Operand Value: " + rightValue + " (type: " + rightValue.getClass().getSimpleName() + ")");

        // Check for valid operand types
        if (!(leftValue instanceof Integer || leftValue instanceof Double || leftValue instanceof Boolean) ||
                !(rightValue instanceof Integer || rightValue instanceof Double || rightValue instanceof Boolean)) {
            throw new IllegalArgumentException("Invalid operand types for conditional comparison");
        }

        // Pass operands to MIPSGenerator for MIPS code generation
        if (mipsGenerator != null) {
            // Pass the actual values, not the strings
            String conditionMIPS = mipsGenerator.generateConditional(operator, leftValue, rightValue);
            System.out.println("Generated MIPS Code: " + conditionMIPS);
        } else {
            System.out.println("MIPSGenerator is not initialized.");
        }

        // Perform the comparison based on operand types
        if (leftValue instanceof Integer && rightValue instanceof Integer) {
            int left = (Integer) leftValue;
            int right = (Integer) rightValue;
            return evaluateNumericCondition(left, right, operator);
        } else {
            double left = convertToDouble(leftValue);
            double right = convertToDouble(rightValue);
            return evaluateNumericCondition(left, right, operator);
        }
    }

    private boolean evaluateNumericCondition(double left, double right, String operator) throws Exception {
        switch (operator) {
            case ">=":
                System.out.println("Evaluating >=: " + (left >= right));
                return left >= right;
            case "<=":
                System.out.println("Evaluating <=: " + (left <= right));
                return left <= right;
            case ">":
                System.out.println("Evaluating >: " + (left > right));
                return left > right;
            case "<":
                System.out.println("Evaluating <: " + (left < right));
                return left < right;
            case "==":
                System.out.println("Evaluating ==: " + (left == right));
                return left == right;
            case "!=":
                System.out.println("Evaluating !=: " + (left != right));
                return left != right;
            default:
                throw new Exception("Unsupported operator: " + operator);
        }
    }

    private double convertToDouble(Object value) throws Exception {
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Double) {
            return (Double) value;
        } else {
            throw new Exception("Cannot convert value to Double: " + value + " (type: " + value.getClass().getSimpleName() + ")");
        }
    }


    public Object getValueFromOperand(String operand) throws Exception {
        // Check if the operand is a variable in the SymbolTable
        if (symbolTable.containsVariable(operand)) {
            Object value = symbolTable.get(operand);
            if (value != null) {
                System.out.println("Found variable: " + operand + " with value: " + value + " (type: " + value.getClass().getSimpleName() + ")");
                return value;
            } else {
                throw new Exception("Variable " + operand + " exists in SymbolTable but has no assigned value.");
            }
        }

        // Check if the operand is a literal in the LiteralTable
        Object literalValue = literalTable.getLiteralValue(operand);
        if (literalValue != null) {
            System.out.println("Found literal: " + operand + " with value: " + literalValue + " (type: " + literalValue.getClass().getSimpleName() + ")");
            return literalValue;
        }

        // Attempt to parse the operand directly as an integer or double
        if (isInteger(operand)) {
            int intValue = Integer.parseInt(operand);
            System.out.println("Parsed operand as Integer: " + intValue);
            return intValue;
        } else if (isDouble(operand)) {
            double doubleValue = Double.parseDouble(operand);
            System.out.println("Parsed operand as Double: " + doubleValue);
            return doubleValue;
        }

        // If operand is not a variable, literal, or valid numeric type, throw an exception
        throw new Exception("Operand " + operand + " not found in SymbolTable or LiteralTable, and is not a valid numeric value.");
    }

    public void evaluateIncrementOrDecrement(String operation, String variableName) throws Exception {
        System.out.println("Evaluating operation: " + operation + " on variable: " + variableName);  // Debug print
        Integer varId = symbolTable.getIdByName(variableName);

        if (varId == null) {
            throw new IllegalArgumentException("Variable '" + variableName + "' is not declared in the symbol table.");
        }

        int currentValue = (int) symbolTable.getValueById(varId);

        // Perform the increment or decrement
        if ("++".equals(operation)) {
            currentValue++;
        } else if ("--".equals(operation)) {
            currentValue--;
        } else {
            throw new IllegalArgumentException("Invalid operation: " + operation);
        }

        // Update the symbol table
        symbolTable.updateValue(variableName, currentValue);
    }

}
