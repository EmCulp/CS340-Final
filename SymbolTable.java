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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    final Map<Integer, Entry> table; // Map to store variable names and their details
    private int nextId; // To keep track of the next available ID
    private Map<String, String> conditionRegisters;
    private Map<String, Token> tokens;

    /**********************************************************
     * CONSTRUCTOR: SymbolTable()                             *
     * DESCRIPTION:                                            *
     * Initializes the symbol table with an empty map and sets *
     * the next ID to 600.                                     *
     **********************************************************/

    public static class Entry{
        private final int id;
        String name;
        String type;
        Object value;
        String scope;
        String register;

        public Entry(int id, String name, String type, Object value, String scope, String register){
            this.id = id;
            this.name = name;
            this.type = type;
            this.value = value;
            this.scope = scope;
            this.register = register;
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

        public String getRegister(){
            return register;
        }

        public void setRegister(String reg){
            this.register = reg;
        }

        @Override
        public String toString(){
            return String.format("Name: %s, Type: %s, Value: %s, Scope: %s", name, type, value, scope);
        }
    }

    public SymbolTable() {
        this.table = new HashMap<>();
        this.nextId = 600; // Start IDs from 600
        conditionRegisters = new HashMap<>();
        tokens = new HashMap<>();
    }

    public void addEntry(String name, String type, Object value, String scope, String register){
        table.put(nextId, new Entry(nextId, name, type, value, scope, register));
        nextId++;
    }

    public Entry getEntry(String variableName){
        if(!table.containsKey(variableName)){
            throw new IllegalArgumentException("Variable " +variableName+ " not found in the symbol table.");
        }
        return table.get(variableName);
    }

    public String getRegister(String variableName){
        // Iterate through the map and find the entry with the matching variable name
        for(Map.Entry<Integer, Entry> entry : table.entrySet()){
            if(entry.getValue().getName().equals(variableName)){
                return entry.getValue().getRegister();  // Return the register of the matching variable
            }
        }
        return null; // Return null if the variable is not found
    }

    public void addRegisterToVariable(String variableName, String register) {
        // Iterate through the map to find the entry with the matching variable name
        for (Map.Entry<Integer, Entry> entry : table.entrySet()) {
            if (entry.getValue().getName().equals(variableName)) {
                // Update the register of the variable
                entry.getValue().setRegister(register);
                System.out.println("Register " + register + " has been assigned to variable " + variableName);
                return;
            }
        }

        // If the variable is not found, throw an exception
        throw new IllegalArgumentException("Variable '" + variableName + "' not found in the Symbol Table.");
    }

    public String getRegisterForVariable(String variableName) {
        System.out.println("Looking up register for variable name: " + variableName);  // Debug print

        for (Map.Entry<Integer, Entry> entry : table.entrySet()) {
            System.out.println("Checking variable: " + entry.getValue().getName());  // Debug print

            if (entry.getValue().getName().equals(variableName)) {
                return entry.getValue().getRegister();
            }
        }

        System.out.println("No register found for variable: " + variableName);  // Debug print
        return null; // Return null if the variable is not found
    }

    public void putConditionRegister(String register, String description) {
        conditionRegisters.put(register, description);  // Store the register with a description
    }

    // Method to retrieve a register description
    public String getConditionRegister(String register) {
        return conditionRegisters.get(register);
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
        boolean variableUpdated = false;

        for(Map.Entry<Integer, Entry> entry : table.entrySet()){
            System.out.println("Checking Variable: " +entry.getValue().name + ", Current Value: " +entry.getValue().value);

            if(entry.getValue().name.trim().equalsIgnoreCase(name.trim())){
                System.out.println("Updating variable '" +name+ "' to new value: " +newValue);
                entry.getValue().value = newValue;
                variableUpdated = true;
                break;
            }
        }

        if(!variableUpdated){
            throw new IllegalArgumentException("Variable '" +name+ "' not found in the SymbolTable. Ensure it's declared");
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

    public Entry getValue(String variableName) throws Exception {
        if (table.containsKey(variableName)) {
            return table.get(variableName); // Return the value of the variable
        } else {
            throw new Exception("Variable not found: " + variableName);
        }
    }


    public Object get(String name) {
        for (Map.Entry<Integer, Entry> entry : table.entrySet()) {
            if (entry.getValue().name.equals(name)) {
                return entry.getValue().value;
            }
        }
        return null; // Return null if the variable name is not found
    }

    public void assignRegisterToVariable(String variableName, String register){
        for(Map.Entry<Integer, Entry> entry : table.entrySet()){
            if(entry.getValue().getName().equals(variableName)){
                entry.getValue().scope = register;
                System.out.println("Assigned register " +register+ " to variable " +variableName);
                return;
            }
        }

        System.out.println("Error: Variable " +variableName+ " not found in the Symbol Table.");
    }

    public String getOffsetByName(String name){
        Entry entry = table.get(name);
        return entry != null ? entry.getRegister() : null;
    }

    public void updateRegister(String variableName, String register) {
        // Check if the variable exists in the table
        Entry entry = getEntry(variableName);  // This will throw an exception if the variable is not found
        if (entry != null) {
            entry.setRegister(register);  // Assuming setRegister is properly defined in Entry class
            System.out.println("Register " + register + " has been assigned to variable " + variableName);
        } else {
            throw new IllegalArgumentException("Variable " + variableName + " not found in symbol table.");
        }
    }


    public Map<Integer, Entry> getAllEntries(){
        return table;
    }

    public void addToken(String key, Token token){
        tokens.put(key, token);
    }

    public Collection<Token> getTokens(){
        return tokens.values();
    }

    public boolean isVariableInDataSection(String variableName) {
        for (Entry entry : table.values()) {
            if (entry.getName().equals(variableName) && "data".equalsIgnoreCase(entry.getScope())) {
                return true;
            }
        }
        return false;
    }


    /**********************************************************
     * METHOD: display()                                    *
     * DESCRIPTION:                                            *
     * Prints the contents of the symbol table to the console, *
     * displaying each variable's name, value, and ID.         *
     **********************************************************/

    // Method to display all entries in the symbol table
    public void display() {
        System.out.println();
        System.out.println("Symbol Table:");
        System.out.println("ID     | Name       | Type       | Value      | Scope  | Register");
        System.out.println("---------------------------------------------------------------------");
        for (Map.Entry<Integer, Entry> entry : table.entrySet()) {
            System.out.printf("%-6d | %-10s | %-10s | %-10s | %-6s | %-10s\n",
                    entry.getKey(), entry.getValue().name, entry.getValue().type, entry.getValue().value, entry.getValue().scope, entry.getValue().getRegister());
        }
    }
}
