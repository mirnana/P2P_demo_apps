public class Ping extends Message {
    
    public Ping(Peer sender, Peer receiver) {
        super(sender, receiver);
    }

    public void send() {
        String payload = "ping;" + sender.port.toString() + ";" + sender.IP;
        super.send(payload);
    }

    public String toString() {
        return "Ping{ " + super.toString() + " }";
    }
}
