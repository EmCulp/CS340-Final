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
    private final Map<Integer, Entry> table; // Map to store variable names and their details
    private int nextId; // To keep track of the next available ID

    /**********************************************************
     * CONSTRUCTOR: SymbolTable()                             *
     * DESCRIPTION:                                            *
     * Initializes the symbol table with an empty map and sets *
     * the next ID to 600.                                     *
     **********************************************************/

    private static class Entry{
        String name;
        String type;
        Object value;
        String scope;

        public Entry(String name, String type, Object value, String scope){
            this.name = name;
            this.type = type;
            this.value = value;
            this.scope = scope;
        }

        public String getName(){
            return name;
        }

        public String getType(){
            return type;
        }

        public void setType(String value){
            this.type = value;
        }

        public Object getValue(){
            return value;
        }

        public void setValue(Object value){
            this.value = value;
        }

        public String getScope(){
            return scope;
        }

        @Override
        public String toString(){
            return String.format("Name: %s, Type: %s, Value: %s, Scope: %s", name, type, value, scope);
        }
    }

    public SymbolTable() {
        this.table = new HashMap<>();
        this.nextId = 600; // Start IDs from 600
    }

    public Entry get(String name){
        return table.get(name);
    }

    public void addEntry(String name, String type, Object value, String scope){
        table.put(nextId, new Entry(name, type, value, scope));
        nextId++;
    }

    public void addBoolean(String variableName, boolean value){
        int id = nextId++;
        Entry entry = new Entry(variableName, "boolean", value, "global");
        table.put(id, entry);
        System.out.println("Added " +variableName+ " to Symbol Table with ID " +id);
    }

    public void addOrUpdateBoolean(String variableName, boolean value){
        if(containsVariable(variableName)){
            updateValue(variableName, value);
            System.out.println("Updated " +variableName+ " with new value: " +value);
        }else{
            addBoolean(variableName, value);
        }
    }

    public void addOrUpdateDouble(String variableName, double value){
        int id = nextId++;
        table.put(id, new Entry(variableName, "double", value, "global"));
        System.out.println("Added double variable: " +variableName+ " with value: " +value);
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
    public Object getValueById(int id){
        Entry entry = table.get(id);
        return (entry != null) ? entry.value : null;
    }

    public Integer getIdByName(String name){
        for(Map.Entry<Integer, Entry> entry : table.entrySet()){
            if(entry.getValue().getName().equals(name)){
                return entry.getKey();
            }
        }
        return null;    //not found
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
    public void updateValue(String name, Object newValue) {
        for(Map.Entry<Integer, Entry> entry : table.entrySet()){
            if(entry.getValue().name.equals(name)){
                entry.getValue().value = newValue;
                return;
            }
        }
        throw new IllegalArgumentException("Variable '" + name + "' not found.");
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
        System.out.println("Checking for variable in symbol table: " +name);
        return table.containsKey(name);
    }

    /**********************************************************
     * METHOD: display()                                    *
     * DESCRIPTION:                                            *
     * Prints the contents of the symbol table to the console, *
     * displaying each variable's name, value, and ID.         *
     **********************************************************/

    // Method to display all entries in the symbol table
    public void display() {
        System.out.println("Symbol Table:");
        System.out.println("ID    | Name       | Type       | Value      | Scope");
        System.out.println("---------------------------------------------------");
        for (Map.Entry<Integer, Entry> entry : table.entrySet()) {
            System.out.printf("%-6d | %-10s | %-10s | %-10s | %-6s\n",
                    entry.getKey(), entry.getValue().name, entry.getValue().type, entry.getValue().value, entry.getValue().scope);
        }
    }
}
