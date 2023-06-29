import java.util.HashMap;
import java.util.Map;

public class Peer {
    
    Integer port;
    String IP;
    Map<String, Integer> data = new HashMap<String,Integer>();
    public long lastMessage;

    public Peer(Integer port, String IP) {
        this.port = port;
        this.IP = IP;
        this.lastMessage = -1;
    }

    public Peer(String address) {
        String[] split = address.split(":", 2);
        this.port = Integer.parseInt(split[1]);
        this.IP = split[0];
    }

    public String toString() {
        return "Peer{ port = " + this.port.toString()
                + ", IP = " + this.IP 
                + ", dataCount = " + this.getDataCount().toString() 
                + " }";
    }

    public void insertData(String name, Integer length) {
        if(!this.data.containsKey(name))
            this.data.put(name.toLowerCase(), length);
    }

    public Integer getData(String name) {
        return this.data.get(name.toLowerCase());
    }
    
    public Integer getDataCount() {
        return this.data.size();
    }

    public void printData() {
        for(var river : data.entrySet()) {
            System.out.println("Duljina rijeke " + river.getKey() + " je " + river.getValue());
        }
    }

    public String getAddress() {
        return IP + ":" + port.toString();
    }

    public boolean equals(Peer p) {
        return p.IP.equals(this.IP) && p.port == this.port;
    }
}
