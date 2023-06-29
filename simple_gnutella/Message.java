import java.io.PrintWriter;
import java.net.ConnectException;
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
        } catch(ConnectException c) {
            // ovaj exception se javlja samo kad jedan peer prestane raditi. 
            // ta greška se može progutati jer će nakon 5 pingova susjedi mrtvog peera odustati od veze
        } catch(Exception e) {
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