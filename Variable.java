/*******************************************************************
 * Variable Class                                                  *
 *                                                                 *
 * PROGRAMMER: Emily Culp                                          *
 * COURSE: CS340 - Programming Language Design                     *
 * DATE: 10/29/2024                                                *
 * REQUIREMENT: Store variable details for the interpreter         *
 *                                                                 *
 * DESCRIPTION:                                                    *
 * The Variable class represents a variable with a name, value,    *
 * and unique ID. It provides getter and setter methods to access  *
 * and modify the value of the variable, ensuring the variable's   *
 * name and ID remain immutable.                                   *
 *                                                                 *
 * COPYRIGHT: This code is copyright (C) 2024 Emily Culp and Dean  *
 * Zeller.                                                         *
 *                                                                 *
 * CREDITS: This code was written with the help of ChatGPT.        *
 *******************************************************************/

public class Variable {
    private final String name; // Variable name
    private int value; // Variable value
    private final int id; // Variable ID

    /**********************************************************
     * CONSTRUCTOR: Variable(String name, int value, int id)   *
     * DESCRIPTION:                                            *
     * Initializes a new Variable object with the specified    *
     * name, value, and ID.                                    *
     * PARAMETERS:                                             *
     *  String name - the name of the variable                 *
     *  int value - the initial value of the variable          *
     *  int id - the unique ID assigned to the variable        *
     **********************************************************/

    public Variable(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public Variable(String name, int value, int id) {
        this.name = name;
        this.value = value;
        this.id = id;
    }

    /**********************************************************
     * METHOD: getName()                                       *
     * DESCRIPTION:                                            *
     * Retrieves the name of the variable.                     *
     * RETURN VALUE:                                           *
     *  String - the name of the variable                      *
     **********************************************************/

    public String getName() {
        return name;
    }

    /**********************************************************
     * METHOD: getValue()                                      *
     * DESCRIPTION:                                            *
     * Retrieves the current value of the variable.            *
     * RETURN VALUE:                                           *
     *  int - the current value of the variable                *
     **********************************************************/

    public int getValue() {
        return value;
    }

    /**********************************************************
     * METHOD: setValue(int value)                             *
     * DESCRIPTION:                                            *
     * Updates the value of the variable.                      *
     * PARAMETERS:                                             *
     *  int value - the new value to assign                    *
     **********************************************************/

    public void setValue(int value) {
        this.value = value;
    }

    /**********************************************************
     * METHOD: getId()                                         *
     * DESCRIPTION:                                            *
     * Retrieves the unique ID of the variable.                *
     * RETURN VALUE:                                           *
     *  int - the ID of the variable                           *
     **********************************************************/

    public int getId() {
        return id;
    }
}
