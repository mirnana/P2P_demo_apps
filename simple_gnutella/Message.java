import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class Message {

    Peer sender;
    Peer receiver;
    long timeToLive;

    public Message(Peer sender, Peer receiver){
        this.sender = sender;
        this.receiver = receiver;
    }

    public void send(String payload) {
        try (Socket client = new Socket(InetAddress.getLocalHost(), receiver.port)) {

            var out = new PrintWriter(client.getOutputStream(), true);
            out.println(payload);
        } catch(Exception e) {
            // exception ce se dogoditi samo kad je host prestao raditi
            // dići custom exception da javimo Gnutelli da može maknuti receivera iz peers
            System.err.println("Message->send error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String toString() {
        return "Message{ sender = " + sender.toString() 
                + ", receiver = " + receiver.toString() 
                + " }";
    }
}