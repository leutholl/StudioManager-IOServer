/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.triggers;

/**
 *
 * @author leutholl
 */
public class ModLcdKeyTrigger extends Trigger {
    
    private char              key;
    private String            i2c_address;

    public ModLcdKeyTrigger(char key, String i2c_address) {
        super(E_TriggerType.ModLCDKEY);
        this.key = key;
        this.i2c_address = i2c_address;
    }
    
    @Override
    public String getTriggerString() {
        //dbformat is ModLCDKEY(key)[i2c_address]
        return E_TriggerType.ModLCDKEY.name()+"("+key+")["+i2c_address+"]";
    }

    /**
     * @return the key
     */
    public char getKey() {
        return key;
    }

    /**
     * @return the i2c_address
     */
    public String getI2c_address() {
        return i2c_address;
    }

    public Character getRundeKlammer() {
        return key;
    }

    @Override
    public String getEckigeKlammer() {
        return i2c_address;
    }

    @Override
    public String getRightFromEquals() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
