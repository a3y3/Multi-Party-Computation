import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class Runner {
    private static final int NUM_PEERS = 5;
    BigInteger secret;

    public static void main(String[] args) throws IOException {
        Runner runner = new Runner();
        runner.demonstrateTOverNSecretSharing();
    }

    /**
     * Creates a polynomial based on a secret and uses it to distribute shares of the
     * secret to peers.
     *
     * @throws IOException if something goes wrong during networking/IO communication.
     */
    private void demonstrateTOverNSecretSharing() throws IOException {
        secret = getSecret();
        Polynomial polynomial = new Polynomial(secret);
        HashMap<Integer, Integer> idToXMap = getIDToXMapping();
        BigInteger[] f = getF(polynomial, idToXMap);
        System.out.println("Secret is: " + secret + " and polynomial is: " + polynomial);
        System.out.println("Distributing shares to peers. Peer i gets share = f(x).");
        distributeShares(f, idToXMap, NUM_PEERS, Utils.SERVICE_NAME, Peer.PORT);
    }

    /**
     * For the secret sharing protocol, the Runner class has the secret. Ideally, this
     * secret should be taken from the user, but currently is generated randomly.
     *
     * @return the secret.
     */
    public BigInteger getSecret() {
        Random r = new Random();
        int numBits = 80;
        return new BigInteger(numBits, r);
    }

    /**
     * Calcluates the value of f(i) for i in range 0 to n, where n is the number of peers.
     *
     * @param polynomial The current Polynomial object.
     * @param idToXMap   A mapping from i->x. This adds more security to the protocol.
     * @return an array such that f[i] = f(x), and f is the current polynomial, and x
     * to i mapping is stored in idToXMap.
     */
    public BigInteger[] getF(Polynomial polynomial, HashMap<Integer, Integer> idToXMap) {
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
    private HashMap<Integer, Integer> getIDToXMapping() {
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
    public void distributeShares(BigInteger[] f, HashMap<Integer, Integer> idToXMap,
                                 int numPeers, String serviceName,
                                 int port) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        DatagramPacket packet;
        //peer i gets share f[x]
        for (int i = 1; i <= numPeers; i++) {
            String peerName = serviceName + "_" + i;
            int x = idToXMap.get(i);
            Utils.ShareWrapper shareWrapper = new Utils.ShareWrapper(f[i], i, x);
            byte[] buffer = shareWrapper.toString().getBytes();
            packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(peerName), port);
            socket.send(packet);
        }
    }
}
