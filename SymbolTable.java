/*******************************************************************
 * SymbolTable Class                                                *
 *                                                                 *
 * PROGRAMMER: Emily Culp                                           *
 * COURSE: CS340 - Programming Language Design                      *
 * DATE: 11/12/2024                                                 *
 * REQUIREMENT: Manage variables for the interpreter                *
 *                                                                 *
 * DESCRIPTION:                                                     *
 * The SymbolTable class manages variables in the interpreter,      *
 * including their names, values, and unique IDs. It allows adding, *
 * retrieving, updating, and checking for variables. Each variable  *
 * receives a unique ID starting from 600. The class also provides  *
 * a method to print the current state of the table for debugging.  *
 *                                                                 *
 * COPYRIGHT: This code is copyright (C) 2024 Emily Culp and Dean   *
 * Zeller.                                                         *
 *                                                                 *
 * CREDITS: This code was written with the help of ChatGPT.         *
 *******************************************************************/

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private final Map<String, Variable> table; // Map to store variable names and their details
    private int nextId; // To keep track of the next available ID

    /**********************************************************
     * CONSTRUCTOR: SymbolTable()                             *
     * DESCRIPTION:                                            *
     * Initializes the symbol table with an empty map and sets *
     * the next ID to 600.                                     *
     **********************************************************/

    public SymbolTable() {
        this.table = new HashMap<>();
        this.nextId = 600; // Start IDs from 600
    }

    /**********************************************************
     * METHOD: getId(String variableName)                     *
     * DESCRIPTION:                                            *
     * Retrieves the ID of a variable by its name. If the      *
     * variable doesn't exist, returns null.                   *
     * PARAMETERS:                                             *
     *  String variableName - the name of the variable         *
     * RETURN VALUE:                                           *
     *  Integer - the ID of the variable, or null if not found *
     **********************************************************/

    // Retrieves the ID of a variable by its name
    public Integer getId(String variableName) {
        Variable var = table.get(variableName); // Retrieve variable
        return (var != null) ? var.getId() : null; // Return ID or null if not found
    }

    /**********************************************************
     * METHOD: getNextId()                                     *
     * DESCRIPTION:                                            *
     * Retrieves the next available ID for a new variable and  *
     * increments the ID counter.                              *
     * RETURN VALUE:                                           *
     *  int - the next available ID                            *
     **********************************************************/

    // Retrieves the next available ID
    public int getNextId() {
        return nextId++;
    }

    /**********************************************************
     * METHOD: addVariable(String name, int value)             *
     * DESCRIPTION:                                            *
     * Adds a new variable to the symbol table. A unique ID is *
     * assigned to the variable, and it is stored in the table.*
     * PARAMETERS:                                             *
     *  String name - the variable's name                      *
     *  int value - the initial value of the variable          *
     **********************************************************/

    // Adds a variable to the symbol table
    public void addVariable(String name, int value) {
        if(!table.containsKey(name)){
//            table.put(name, new Variable(name, nextId++));
            put(name, value);
        }
    }

    /**********************************************************
     * METHOD: getValue(String name)                           *
     * DESCRIPTION:                                            *
     * Retrieves the value associated with a variable name. If *
     * the variable is not found, an error message is printed, *
     * and null is returned.                                   *
     * PARAMETERS:                                             *
     *  String name - the name of the variable                 *
     * RETURN VALUE:                                           *
     *  Integer - the value of the variable, or null if not    *
     *  found                                                  *
     **********************************************************/

    // Retrieves the value associated with a variable name
    public Integer getValue(String name) {
        Variable value = table.get(name);
        if(value == null){
            System.err.println("Variable not found " + name);
            return null;
        }
        return value.getValue();
    }

    /**********************************************************
     * METHOD: updateValue(String name, int value)             *
     * DESCRIPTION:                                            *
     * Updates the value of a variable. If the variable is not *
     * found, an exception is thrown to indicate that it has   *
     * not been declared.                                      *
     * PARAMETERS:                                             *
     *  String name - the name of the variable                 *
     *  int value - the new value to assign                    *
     **********************************************************/

    // Updates the value of a variable
    public void updateValue(String name, int value) {
        Variable var = table.get(name);
        if (var != null) {
            var.setValue(value);
        } else {
            throw new IllegalArgumentException("Variable '" + name + "' is undeclared.");
        }
    }

    /**********************************************************
     * METHOD: containsVariable(String name)                  *
     * DESCRIPTION:                                            *
     * Checks if a variable exists in the symbol table.        *
     * PARAMETERS:                                             *
     *  String name - the name of the variable to check        *
     * RETURN VALUE:                                           *
     *  boolean - true if the variable exists, false otherwise *
     **********************************************************/

    // Checks if a variable exists in the symbol table
    public boolean containsVariable(String name) {
        return table.containsKey(name);
    }

    public void setVariableValue(String variableName, Integer value){
        Variable var = table.get(variableName);
        if(var != null){
            var.setValue(value);
        }else{
            System.out.println("Error: Variable not found");
        }
    }

    public void put(String variableName, int value){
        Variable variable = new Variable(variableName, value, nextId++);
        table.put(variableName, variable);
    }

    /**********************************************************
     * METHOD: printTable()                                    *
     * DESCRIPTION:                                            *
     * Prints the contents of the symbol table to the console, *
     * displaying each variable's name, value, and ID.         *
     **********************************************************/

    // Prints the symbol table (for debugging)
    public void printTable() {
        System.out.println("Symbol Table:");
        for (Map.Entry<String, Variable> entry : table.entrySet()) {
            System.out.println("Variable: " + entry.getKey() + ", Value: " + entry.getValue().getValue() + ", ID: " + entry.getValue().getId());
        }
    }
}
