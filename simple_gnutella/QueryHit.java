public class QueryHit extends Message {

    public String searchTerm;
    public Integer result;

    public QueryHit(Peer sender, Peer receiver, String searchTerm, Integer result) {
        super(sender, receiver);
        this.searchTerm = searchTerm;
        this.result = result;
    }

    public void send() {
        String payload = "qhit;" + sender.port.toString() 
                           + ";" + sender.IP
                           + ";" + searchTerm
                           + ";" + result.toString();
        super.send(payload);
    }

    public String toString() {
        return "QueryHit{ " + super.toString() + " }";
    }
}
