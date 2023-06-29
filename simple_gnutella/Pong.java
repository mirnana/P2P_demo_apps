public class Pong extends Message{
    public Pong(Peer sender, Peer receiver) {
        super(sender, receiver);
    }

    public void send() {
        String payload = "pong;" + sender.port.toString() + ";" + sender.IP + ";" + sender.getDataCount();
        super.send(payload);
    }

    public String toString() {
        return "Ping{ " + super.toString() + " }";
    }
}
