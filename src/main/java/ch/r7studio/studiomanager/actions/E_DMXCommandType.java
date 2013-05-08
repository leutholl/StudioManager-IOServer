/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.actions;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author leutholl
 */
public enum E_DMXCommandType {

    ABSOLUTE_VALUE("a"), RELATIVE_VALUE("r"), INTELLIGENT("i"), SYSTEM("s");

    private final String abbreviation;
    // Reverse-lookup map for getting a day from an abbreviation
    private static final Map<String, E_DMXCommandType> lookup = new HashMap<String, E_DMXCommandType>();

    static {
        for (E_DMXCommandType d : E_DMXCommandType.values()) {
            lookup.put(d.getAbbreviation(), d);
        }
    }

    private E_DMXCommandType(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public static E_DMXCommandType get(String abbreviation) {
        return lookup.get(abbreviation);
    }
}
