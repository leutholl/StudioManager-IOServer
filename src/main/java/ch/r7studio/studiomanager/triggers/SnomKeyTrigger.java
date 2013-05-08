/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.r7studio.studiomanager.triggers;


public class SnomKeyTrigger extends Trigger {
    
    private int               address;
    private String            from_ip;
    
    public SnomKeyTrigger(int address, String from_ip) {
        super(E_TriggerType.SnomKEY);
        this.address = address;
        this.from_ip = from_ip;
    }

    @Override
    public String getTriggerString() {
        //dbformat is SnomKEY(address)[ip]
        return E_TriggerType.SnomKEY.name()+"("+address+")"; //["+from_ip+"]";
    }

    /**
     * @return the address
     */
    public int getAddress() {
        return address;
    }

    /**
     * @return the from_ip
     */
    public String getFrom_ip() {
        return from_ip;
    }

    @Override
    public Integer getRundeKlammer() {
        return address;
    }

    @Override
    public Object getEckigeKlammer() {
        return from_ip;
    }

    @Override
    public Object getRightFromEquals() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
