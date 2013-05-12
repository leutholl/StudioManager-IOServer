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
public enum E_HD44780CommandType {

    TEXT_MSG("t"), BACKLIGHT_CMD("b");

    private final String abbreviation;
    // Reverse-lookup map for getting a day from an abbreviation
    private static final Map<String, E_HD44780CommandType> lookup = new HashMap<String, E_HD44780CommandType>();

    static {
        for (E_HD44780CommandType d : E_HD44780CommandType.values()) {
            lookup.put(d.getAbbreviation(), d);
        }
    }

    private E_HD44780CommandType(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public static E_HD44780CommandType get(String abbreviation) {
        E_HD44780CommandType ct = lookup.get(abbreviation);
        if (ct==null) {
            ct = TEXT_MSG;
        }
        return ct;
    }
}
