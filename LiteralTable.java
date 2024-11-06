/*******************************************************************
 * LiteralTable Class                                               *
 *                                                                 *
 * PROGRAMMER: Emily Culp                                           *
 * COURSE: CS340 - Programming Language Design                      *
 * DATE: 10/17/2024                                                 *
 * REQUIREMENT: Manage literals for the interpreter                 *
 *                                                                 *
 * DESCRIPTION:                                                     *
 * This class manages a table of literals, allowing for the         *
 * addition, retrieval, and management of integer literals used     *
 * in the interpreter. Each literal is assigned a unique ID, and    *
 * the table provides functionality to access literal values        *
 * based on their IDs. If the literal already exists, the same ID   *
 * will be returned to ensure consistency. This helps maintain      *
 * efficient tracking of literals during program execution.         *
 *                                                                 *
 * COPYRIGHT: This code is copyright (C) 2024 Emily Culp and Dean   *
 * Zeller.                                                         *
 *                                                                 *
 * CREDITS: This code was written with the help of ChatGPT.         *
 *******************************************************************/

import java.util.HashMap;
import java.util.Map;

public class LiteralTable{
    private final Map<Integer, Integer> literalMap = new HashMap<>();

    private int nextLiteralID = 900;

    /**********************************************************
     * METHOD: addLiteral(int value)                          *
     * DESCRIPTION:                                            *
     * Adds a new literal to the table if it doesn't already   *
     * exist, assigns a unique ID, and returns the ID. If the  *
     * literal already exists, it returns the existing ID.     *
     * PARAMETERS:                                             *
     *  int value - the integer literal to add                 *
     * RETURN VALUE:                                           *
     *  int - the ID assigned to the literal                   *
     **********************************************************/

    public int addLiteral(int value) {
        if(!literalMap.containsValue(value)){
            literalMap.put(nextLiteralID, value);
            return nextLiteralID++;
        }
        return getLiteralID(value);
    }


    /**********************************************************
     * METHOD: getLiteralID(int value)                        *
     * DESCRIPTION:                                            *
     * Retrieves the ID of a given literal from the table. If  *
     * the literal does not exist, it returns -1.              *
     * PARAMETERS:                                             *
     *  int value - the literal value to search for            *
     * RETURN VALUE:                                           *
     *  Integer - the ID of the literal or -1 if not found     *
     **********************************************************/
    public int getLiteralID(int value){
        return getLiteralID(String.valueOf(value));
    }

    /**********************************************************
     * METHOD: getLiteralID(String literal)                    *
     * DESCRIPTION:                                            *
     * Retrieves the ID of a given literal (as a String) from  *
     * the table. If the literal does not exist, it returns -1. *
     * PARAMETERS:                                             *
     *  String literal - the literal value to search for       *
     * RETURN VALUE:                                           *
     *  Integer - the ID of the literal or -1 if not found     *
     **********************************************************/
    public int getLiteralID(String literal) {
        for (Map.Entry<Integer, Integer> entry : literalMap.entrySet()) {
            if (entry.getValue().toString().equals(literal)) {
                return entry.getKey();  // Return the ID of the literal
            }
        }
        return -1; // Return -1 if the literal is not found
    }

    public boolean containsLiteral(String literal){
        return getLiteralID(literal) != -1;
    }

    /**********************************************************
     * METHOD: printTable()                                   *
     * DESCRIPTION:                                            *
     * Prints the contents of the literal table to the console.*
     * This includes each literal ID and its corresponding     *
     * value, displaying the current state of the table.       *
     **********************************************************/

    public void printTable() {
        System.out.println("Literal Table:");
        for(Map.Entry<Integer, Integer> entry : literalMap.entrySet()){
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}