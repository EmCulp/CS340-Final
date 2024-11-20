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
        private final int id;
        String name;
        String type;
        Object value;
        String scope;

        public Entry(int id, String name, String type, Object value, String scope){
            this.id = id;
            this.name = name;
            this.type = type;
            this.value = value;
            this.scope = scope;
        }

        public int getId(){
            return id;
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
        table.put(nextId, new Entry(nextId, name, type, value, scope));
        nextId++;
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
                return entry.getValue().getId();
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
        for (Map.Entry<Integer, Entry> entry : table.entrySet()) {
            System.out.println("Variable: " + entry.getValue().name + ", Value: " + entry.getValue().value);
            if (entry.getValue().name.equals(name)) {
                System.out.println("Updating " + name + " to new value: " + newValue);  // Debug print
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
        for(Entry entry : table.values()){
            if(entry.getName().equals(name)){
                return true;
            }
        }
        return false;
    }

    public String getTypeByName(String variableName){
        for(Map.Entry<Integer, Entry> entry : table.entrySet()){
            if(entry.getValue().getName().equals(variableName)){
                return entry.getValue().getType();
            }
        }
        return null;
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
