package ch.r7studio.studiomanager.pojo;
// Generated 28.04.2013 03:04:57 by Hibernate Tools 3.2.1.GA



/**
 * Modtronix generated by hbm2java
 */
public class Modtronix  implements java.io.Serializable {


     private String ip;
     private String description;
     private String location;
     private String type;
     private String firmware;

    public Modtronix() {
    }

	
    public Modtronix(String ip, String type) {
        this.ip = ip;
        this.type = type;
    }
    public Modtronix(String ip, String description, String location, String type, String firmware) {
       this.ip = ip;
       this.description = description;
       this.location = location;
       this.type = type;
       this.firmware = firmware;
    }
   
    public String getIp() {
        return this.ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    public String getLocation() {
        return this.location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    public String getType() {
        return this.type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    public String getFirmware() {
        return this.firmware;
    }
    
    public void setFirmware(String firmware) {
        this.firmware = firmware;
    }




}


