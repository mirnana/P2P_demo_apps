public class Query extends Message{

    public String searchTerm;
    public Long timestamp;
    public Long timeToLive;
    
    public Query(Peer sender, Peer receiver, String searchTerm, long timestamp, long timeToLive) {
        super(sender, receiver);
        this.searchTerm = searchTerm;
        this.timestamp  = timestamp;
        this.timeToLive = timeToLive;
    }

    public void send() {
        String payload = "query;" + sender.port.toString() 
                            + ";" + sender.IP
                            + ";" + searchTerm
                            + ";" + timestamp.toString()
                            + ";" + timeToLive.toString();
        super.send(payload);
    }

    public String toString() {
        return "Query{ " + super.toString() + " }";
    }
}
