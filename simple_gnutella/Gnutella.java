import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Gnutella {
    
    public final static int DEFAULT_PORT    = 8080;
    public final static int MAX_CONNECTIONS = 5;
    public final static int SLEEP_SECONDS   = 10;

    public static void main(String[] args) {
        Integer port = DEFAULT_PORT;
        String connect = "";
        for (int i = 0; i < args.length; ++i) {
            try {
                switch (args[i++]) {
                    case "--port":
                        port = Integer.parseInt(args[i]);
                        break;
                    case "--connect":
                        connect = args[i];
                        break;
                    default:
                        System.err.println("Invalid argument: " + args[i - 1]);
                        return;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.println("Invalid arguments");
                }
            }
            
        Gnutella g = new Gnutella(port);
        if(connect != null && !connect.isEmpty())
            g.connect(connect);
        g.start();
    }
    
    Map<String, Integer> peers = new HashMap<String, Integer>();    // (peer.getAddress, numPings-numPongs)
    Map<String, Peer> pendingQueries = new HashMap<String, Peer>(); // (searched term, peer from whom query was forwarded)

    Peer thisPeer;

    Gnutella(Integer port) {
        thisPeer = new Peer(port, getMyIP());
    }

    public void connect(String address) {
        String[] split = address.split(":", 2);
        String ip = split[0];
        if (ip.equalsIgnoreCase("localhost") || ip.equalsIgnoreCase("127.0.0.1"))
        {
            ip = getMyIP();
        }
        Integer connectPort = DEFAULT_PORT;
        if (split.length > 1)
            connectPort = Integer.parseInt(split[1]);

        Peer p = new Peer(connectPort, ip);
        peers.put(p.getAddress(), 0);
    }

    public String getMyIP() {
        try (final DatagramSocket datagramSocket = new DatagramSocket()){
            datagramSocket.connect(InetAddress.getByName("8.8.8.8"), 12345);
            return datagramSocket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            System.err.println("Error getting local IP: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void start() {

        PingSender pingSender = new PingSender();
        MessageReceiver msgReceiver = new MessageReceiver();

        msgReceiver.setDaemon(true);
        msgReceiver.start();
        pingSender.setDaemon(true);
        pingSender.start();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("Unesi: " +  System.lineSeparator()
                                + "q za query;" +  System.lineSeparator()
                                + "i za unošenje novog podatka;" +  System.lineSeparator() 
                                + "p za ispis vlastitih podataka;" +  System.lineSeparator()
                                + "c za novu konekciju;" +  System.lineSeparator()
                                + "t za ispis susjeda u mreži;" +  System.lineSeparator()
                                + "x za izlaz iz aplikacije.");
            String str = sc.nextLine();

            if(str.equals("q")) {

            } else if(str.equals("i")) {
                System.out.println("Unesi ime rijeke");
                String name = sc.nextLine();
                System.out.println("Unesi duljinu rijeke");
                int length = sc.nextInt();
                thisPeer.insertData(name, length);
            } else if (str.equals("p")) {
                thisPeer.printData();
            } else if(str.equals("c")) {
                System.out.println("Unesi <adresa>:<port>");
                String address = sc.nextLine();
                this.connect(address);
            } else if(str.equals("t")) {
                for(String peer : peers.keySet())
                    System.out.println(peer);
            } else if(str.equals("x")) {
                msgReceiver.running = false;
                break;
            } else {
                System.out.println("Neispravan unos");
            }
        }
        sc.close();
    }

    private class PingSender extends Thread {

        public PingSender() {}

        public void run() {
            
            while(true) {
                ArrayList<String> removable = new ArrayList<>();
                for(String peer : peers.keySet()) {
                    if(peers.get(peer) > 5) 
                        removable.add(peer);
                    else {
                        Ping p = new Ping(thisPeer, new Peer(peer));
                        p.send();
                        peers.put(peer, peers.get(peer) + 1);
                    }
                }

                // brišem susjede koji se dugo nisu javili
                for(String peer : removable) {
                    peers.remove(peer);
                }

                try {
                    TimeUnit.SECONDS.sleep(SLEEP_SECONDS);
                } catch(Exception e) {
                    System.err.println("PingSender error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private class MessageReceiver extends Thread {

        public boolean running = true;

        public MessageReceiver() {}

        public void run() {

            try (ServerSocket server = new ServerSocket(thisPeer.port, 0, InetAddress.getLocalHost())) { //zasad samo localhost!!!!!!!!!
                while(running) {
                    Socket socket = server.accept();
                    var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String inputLine;
                    while((inputLine = in.readLine()) != null) {
                        String[] split = inputLine.split(";");
                        
                        String peer = split[2] + ":" + split[1];
                        if(split[0].equals("ping")) {
                            if(!peers.containsKey(peer))
                                peers.put(peer, 0);
                            Pong p = new Pong(thisPeer, new Peer(peer));
                            p.send();
                        } else if(split[0].equals("pong")) {
                            if(peers.containsKey(peer)) {
                                peers.replace(peer, peers.get(peer) - 1);
                            }
                        } else if(split[0].equals("query")) {

                        } else if(split[0].equals("qhit")) {

                        }
                    }
                }
            } catch(Exception e) {
                System.err.println("MessageReceiver error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
