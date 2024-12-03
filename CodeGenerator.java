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
     * START_PAREN: Represents an opening parenthesis used to *
     *              group operations or expressions.          *
     **********************************************************/
    START_PAREN,

    /**********************************************************
     * END_PAREN: Represents a closing parenthesis used to    *
     *            end a grouped operation or expression.      *
     **********************************************************/
    END_PAREN,

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

    ADDI,    // Add Immediate
    SUBI,    // Subtract Immediate
    LW,      // Load Word
    SLT,     // Set Less Than
    BNE,     // Branch if Not Equal
    SGN,     // Set Greater Than
    BLT,     // Branch if Less Than
    BEQ,     // Branch if Equal
    BGT      // Branch if Greater Than
}
