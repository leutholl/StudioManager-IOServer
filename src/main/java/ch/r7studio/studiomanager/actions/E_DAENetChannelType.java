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
public enum E_DAENetChannelType {

    RELAYS_PORT("relais"), DIGITAL_PORT("digital");

    private final String abbreviation;
    // Reverse-lookup map for getting a day from an abbreviation
    private static final Map<String, E_DAENetChannelType> lookup = new HashMap<String, E_DAENetChannelType>();

    static {
        for (E_DAENetChannelType d : E_DAENetChannelType.values()) {
            lookup.put(d.getAbbreviation(), d);
        }
    }

    private E_DAENetChannelType(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public static E_DAENetChannelType get(String abbreviation) {
        E_DAENetChannelType ct = lookup.get(abbreviation);
        return ct;
    }
}
