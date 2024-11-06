/*******************************************************************
 * CodeGenerator Enum                                              *
 *                                                                 *
 * PROGRAMMER: Emily Culp                                          *
 * COURSE: CS340 - Programming Language Design                     *
 * DATE: 10/29/2024                                                *
 * REQUIREMENT: Code Generation for Interpreter                    *
 *                                                                 *
 * DESCRIPTION:                                                    *
 * The CodeGenerator enum defines a set of symbolic instructions   *
 * used by the interpreter for generating and executing operations.*
 * These symbolic codes correspond to different operations and     *
 * markers used during the interpretation and code generation      *
 * processes. They help in defining, printing, storing, and        *
 * performing arithmetic operations.                               *
 *                                                                 *
 * COPYRIGHT: This code is copyright (C) 2024 Emily Culp and Dean  *
 * Zeller.                                                         *
 *                                                                 *
 * CREDITS: This code was written with the help of ChatGPT.        *
 *******************************************************************/

public enum CodeGenerator {
    /**********************************************************
     * START_DEFINE: Marks the beginning of a variable        *
     *               definition or assignment.                *
     **********************************************************/
    START_DEFINE,
    /**********************************************************
     * END_DEFINE: Marks the end of a variable definition or  *
     *             assignment block.                          *
     **********************************************************/
    END_DEFINE,
    /**********************************************************
     * NO_OP: Represents a no-operation instruction, often    *
     *        used for placeholders or to maintain structure. *
     **********************************************************/
    NO_OP,
    /**********************************************************
     * START_PRINT: Marks the beginning of a print operation, *
     *              signaling the start of printing values.   *
     **********************************************************/
    START_PRINT,

    /**********************************************************
     * END_PRINT: Marks the end of a print operation.         *
     **********************************************************/
    END_PRINT,
    /**********************************************************
     * START_PAREN: Represents an opening parenthesis used to *
     *              group operations or expressions.          *
     **********************************************************/
    END_PAREN,
    /**********************************************************
     * END_PAREN: Represents a closing parenthesis used to    *
     *            end a grouped operation or expression.      *
     **********************************************************/
    START_PAREN,

    /**********************************************************
     * LOAD: Loads a value from a variable or memory location *
     *       into a register for further operations.          *
     **********************************************************/
    LOAD,
    /**********************************************************
     * STORE: Stores a value into a variable or memory        *
     *        location from a register.                       *
     **********************************************************/
    STORE,

    /**********************************************************
     * ADD: Represents an addition operation between two      *
     *      values.                                           *
     **********************************************************/
    ADD,
    /**********************************************************
     * SUB: Represents a subtraction operation between two    *
     *      values.                                           *
     **********************************************************/
    SUB,
    /**********************************************************
     * MULT: Represents a multiplication operation between    *
     *       two values.                                      *
     **********************************************************/
    MULT,
    /**********************************************************
     * DIV: Represents a division operation between two       *
     *      values.                                           *
     **********************************************************/
    DIV,
    BEQ,
    BNE,
    BLT,
    BGT
}
