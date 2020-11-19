import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class Utils {
    public static final int NUM_PEERS = 5;
    public static final String DELIMITER = ";";
    public static final String SERVICE_NAME = "multi-party-computation";
    public static final String SERVICE_NAME_RUNNER = SERVICE_NAME + "_runner_1";
    public static final String SERVICE_NAME_PEER = SERVICE_NAME + "_peer";

    public static class ShareWrapper {
        BigInteger share;
        int id;
        int x;

        public ShareWrapper(BigInteger share, int id, int x) {
            this.share = share;
            this.id = id;
            this.x = x;
        }

        public ShareWrapper(String message) {
            String[] splitMessage = message.split(DELIMITER);
            this.share = new BigInteger(splitMessage[0]);
            this.id = Integer.parseInt(splitMessage[1]);
            this.x = Integer.parseInt(splitMessage[2]);
        }

        @Override
        public String toString() {
            return share + DELIMITER + id + DELIMITER + x;
        }
    }

    public static class OneMillionBeaverTriples {
        int nBits = 20;
        Random r = new Random();
        int n = (int) Math.pow(2, 17);
        private static final String DELIMITER = ";"; //Separates a,b,c.
        private static final String SEPARATOR = "/"; //Separates 2 triples (a;b;c/a;b;c/)

        BigInteger[] a = new BigInteger[n];
        BigInteger[] b = new BigInteger[n];
        BigInteger[] c = new BigInteger[n];

        OneMillionBeaverTriples() {
            for (int i = 0; i < n; i++) {
                a[i] = new BigInteger(nBits, r);
                b[i] = new BigInteger(nBits, r);
                c[i] = a[i].multiply(b[i]);
            }
        }

        public OneMillionBeaverTriples(BigInteger[] a, BigInteger[] b, BigInteger[] c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        OneMillionBeaverTriples(String oneMillionTriples) {
            String[] triples = oneMillionTriples.split(SEPARATOR);
            for (int i = 0; i < triples.length; i++) {
                String triple = triples[i];
                String[] splitTriples = triple.split(DELIMITER);
                a[i] = new BigInteger(splitTriples[0]);
                b[i] = new BigInteger(splitTriples[1]);
                c[i] = new BigInteger(splitTriples[2]);
            }
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < n; i++) {
                String triple = a[i] + DELIMITER + b[i] + DELIMITER + c[i];
                if (i != n - 1) {
                    triple += SEPARATOR;
                }
                result.append(triple);
            }
            return result.toString();
        }
    }

    /**
     * Calculates the value of f(i) for i in range 0 to n, where n is the number of peers.
     *
     * @param polynomial The current Polynomial object.
     * @param idToXMap   A mapping from i->x. This adds more security to the protocol.
     * @return an array such that f[i] = f(x), and f is the current polynomial, and x
     * to i mapping is stored in idToXMap.
     */
    public static BigInteger[] getF(Polynomial polynomial,
                                    HashMap<Integer, Integer> idToXMap) {
        BigInteger[] result = new BigInteger[NUM_PEERS + 1];
        for (int i = 1; i <= NUM_PEERS; i++) {
            int x = idToXMap.get(i);
            result[i] = polynomial.f(x);
        }
        return result;
    }

    /**
     * Generates N unique random numbers, and creates a hashmap of the mapping of i to
     * the random number.
     *
     * @return a hashmap which contains mapping of i->x.
     */
    public static HashMap<Integer, Integer> getIDToXMapping() {
        HashMap<Integer, Integer> idToX = new HashMap<>();
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = 1; i <= NUM_PEERS * 2; i++) {
            list.add(i);
        }
        Collections.shuffle(list);
        for (int i = 1; i <= NUM_PEERS; i++) {
            idToX.put(i, list.get(i));
        }
        return idToX;
    }

    /**
     * Similar to {@code getIDToXMapping()}, except that i is mapped to i instead of x.
     * This function is important because to add up secret shares, we can't randomize x
     * for each i. i has to be mapped with i, because given m polynomials p1,p2...pm,
     * at each party, it must do p1(i)+p2(i)...+pm(i), and not p1(x1)+p2(x2)...pm(xm),
     * in order to line up and add secrets at p1(0) to pm(0).
     *
     * @return a hashmap which contains mapping of i->i. There isn't exactly a point to
     * this mapping, except to maintain code compatibility and flow/structure.
     */
    public static HashMap<Integer, Integer> getIDToXWithoutRandomization() {
        HashMap<Integer, Integer> idToX = new HashMap<>();
        for (int i = 1; i <= NUM_PEERS; i++) {
            idToX.put(i, i);
        }
        return idToX;
    }

    /**
     * Sends the shares to the corresponding peer. A share is sent to peer i, along
     * with i (so that the peer is aware of its position in the group) and x (so that
     * reconstruction of the secret is possible).
     *
     * @param f           An array such that f[i] = f(x). See {@code getSecretFromUser()}
     * @param idToXMap    A mapping of i->x
     * @param numPeers    The number of parties in the protocol.
     * @param serviceName The host name of the service. Used for InetAddress.getByName().
     * @param port        The receiving party listens for comms on this port.
     */
    public static void distributeShares(BigInteger[] f, HashMap<Integer, Integer> idToXMap,
                                        int numPeers, String serviceName,
                                        int port) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        DatagramPacket packet;
        //peer i gets share f[x]
        for (int i = 1; i <= numPeers; i++) {
            String peerName = serviceName + "_" + i;// multi-party-computation_peer_1
            int x = idToXMap.get(i);
            Utils.ShareWrapper shareWrapper = new Utils.ShareWrapper(f[i], i, x);
            byte[] buffer = shareWrapper.toString().getBytes();
            packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(peerName), port);
            socket.send(packet);
        }
    }
}
