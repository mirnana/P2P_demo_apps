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
    public final static int DEFAULT_TTL     = 30 * 1000;    // u milisekundama

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
    
    Map<String, Integer> peers = new HashMap<String, Integer>();        // (peer.getAddress, numPings-numPongs)
    Map<String, Query> pendingQueries = new HashMap<String, Query>();   // (searched term, query{sender=od koga je stigao})

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
        if(peers.size() < MAX_CONNECTIONS)
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
                System.out.println("Unesi ime rijeke");
                String name = sc.nextLine();
                Integer length = thisPeer.getData(name);
                if(length == null) {
                    for(String peer : peers.keySet()) {
                        Query q = new Query(thisPeer, new Peer(peer), name, System.currentTimeMillis(), DEFAULT_TTL);
                        q.send();
                    }
                    Query q = new Query(thisPeer, null, name, System.currentTimeMillis(), DEFAULT_TTL);
                    pendingQueries.put(name.toLowerCase(), q);
                }
                else {
                    System.out.println("Duljina rijeke " + name + " je " + length);
                }
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
                        peers.replace(peer, peers.get(peer) + 1);
                    }
                }
                // brišem susjede koji se dugo nisu javili
                for(String peer : removable) {
                    peers.remove(peer);
                }

                cleanPendingQueries();

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
                            if(!peers.containsKey(peer) && peers.size() < MAX_CONNECTIONS) {
                                peers.put(peer, 0);
                            }
                            Pong p = new Pong(thisPeer, new Peer(peer));
                            p.send();
                        } else if(split[0].equals("pong")) {
                            peers.replace(peer, peers.get(peer) - 1);
                        } else if(split[0].equals("query")) {
                            String name = split[3].toLowerCase();
                            Long timestamp = Long.parseLong(split[4]);
                            Long timeToLive = Long.parseLong(split[5]);
                            if(System.currentTimeMillis() - timestamp >= timeToLive 
                                || pendingQueries.containsKey(name))
                                continue;
                            Integer length = thisPeer.getData(name);
                            if(length == null) {
                                Query q = new Query(new Peer(peer), null, name, timestamp, timeToLive);
                                pendingQueries.put(name, q);

                                for(var p : peers.keySet()) {
                                    q = new Query(thisPeer, new Peer(p), name, timestamp, timeToLive);
                                    q.send();
                                }
                            }
                            else {
                                QueryHit qh = new QueryHit(thisPeer, new Peer(peer), name, length);
                                qh.send();
                            }
                        } else if(split[0].equals("qhit")) {
                            String name = split[3].toLowerCase();
                            Integer length = Integer.parseInt(split[4]);
                            Query q = pendingQueries.get(name);
                            if(q == null)
                                continue;
                            // osiguravam replikciju podataka -- sve podatke koje prosljeđujem, usput sačuvam i sebi
                            thisPeer.insertData(name, length);
                             // ako nisam ja originalno postavila ovaj query, onda odgovor prosljeđujem peeru koji je meni poslao taj query
                            if(!q.sender.equals(thisPeer)) {   
                                QueryHit qh = new QueryHit(thisPeer, new Peer(peer), name, length);
                                qh.send();
                            }
                        }
                    }
                }
            } catch(Exception e) {
                System.err.println("MessageReceiver error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void cleanPendingQueries() {
        ArrayList<String> removable = new ArrayList<>();
        for(var q : pendingQueries.entrySet()) {
            var key = q.getKey();
            var val = q.getValue();
            if(System.currentTimeMillis() - val.timestamp >= val.timeToLive)
                removable.add(key);
            if(thisPeer.getData(key) != null) {
                removable.add(key);
                // ako saznam odgovor na neki query, proslijedim ga
                if(!val.sender.equals(thisPeer)) {
                    QueryHit qh = new QueryHit(thisPeer, val.sender, key, thisPeer.getData(key));
                    qh.send();
                }
            }
        }
        for(String query : removable) {
            pendingQueries.remove(query);
        }
    }
}
