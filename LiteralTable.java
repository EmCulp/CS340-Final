/*******************************************************************
 * LiteralTable Class                                               *
 *                                                                 *
 * PROGRAMMER: Emily Culp                                           *
 * COURSE: CS340 - Programming Language Design                      *
 * DATE: 11/12/2024                                                 *
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
    private final Map<Integer, Object> literalTable = new HashMap<>();

    private int nextLiteralID = 900;

    public int addLiteral(Object value){
        if(!literalTable.containsValue(value)){
            int literalID = nextLiteralID++;
            literalTable.put(literalID, value);
            return literalID;
        }else{
            for(Map.Entry<Integer, Object> entry : literalTable.entrySet()){
                if(entry.getValue().equals(value)){
                    return entry.getKey();
                }
            }
        }
        return -1;
    }

    public int getLiteralID(Object value){
        for(Map.Entry<Integer, Object> entry : literalTable.entrySet()){
            if(entry.getValue().equals(value)){
                return entry.getKey();
            }
        }

        return -1;
    }

    public boolean containsValue(Object value){
        return literalTable.containsValue(value);
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
        for (Map.Entry<Integer, Object> entry : literalTable.entrySet()) {
            System.out.println("ID: " + entry.getKey() + ", Value: " + entry.getValue());
        }
    }
}