package ch.r7studio.studiomanager.pojo;
// Generated 28.04.2013 03:04:57 by Hibernate Tools 3.2.1.GA



/**
 * Daenetip generated by hbm2java
 */
public class Daenetip  implements java.io.Serializable {


     private String ip;
     private String description;
     private String location;
     private Integer temp;

    public Daenetip() {
    }

	
    public Daenetip(String ip, String description) {
        this.ip = ip;
        this.description = description;
    }
    public Daenetip(String ip, String description, String location, Integer temp) {
       this.ip = ip;
       this.description = description;
       this.location = location;
       this.temp = temp;
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
    public Integer getTemp() {
        return this.temp;
    }
    
    public void setTemp(Integer temp) {
        this.temp = temp;
    }




}


