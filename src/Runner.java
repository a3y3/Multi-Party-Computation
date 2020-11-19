import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class Runner {
    BigInteger secret;
    protected static final int PORT = 7890;
    DatagramSocket datagramSocket = new DatagramSocket(PORT);

    public Runner() throws SocketException {
    }

    public static void main(String[] args) throws IOException {
        Runner runner = new Runner();
        runner.demonstrateTOverNSecretSharing();
        runner.demonstrateBeaverTriples();
    }

    private void demonstrateBeaverTriples() throws IOException {
        for (int i = 0; i < 4; i++) {
            waitForContinue();
            int a = 2;
            int b = 3;
            int c = a * b;
            Polynomial polynomialA = new Polynomial(new BigInteger(String.valueOf(a)));
            Polynomial polynomialB = new Polynomial(new BigInteger(String.valueOf(b)));
            Polynomial polynomialC = new Polynomial(new BigInteger(String.valueOf(c)));
            HashMap<Integer, Integer> idToXMap = Utils.getIDToXWithoutRandomization();
            BigInteger[] fA = Utils.getF(polynomialA, idToXMap);
            BigInteger[] fB = Utils.getF(polynomialB, idToXMap);
            BigInteger[] fC = Utils.getF(polynomialC, idToXMap);

            Utils.distributeShares(fA, idToXMap, Utils.NUM_PEERS, Utils.SERVICE_NAME_PEER,
                    Peer.PORT);
            Utils.distributeShares(fB, idToXMap, Utils.NUM_PEERS, Utils.SERVICE_NAME_PEER,
                    Peer.PORT);
            Utils.distributeShares(fC, idToXMap, Utils.NUM_PEERS, Utils.SERVICE_NAME_PEER,
                    Peer.PORT);
        }
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
        HashMap<Integer, Integer> idToXMap = Utils.getIDToXMapping();
        BigInteger[] f = Utils.getF(polynomial, idToXMap);
        System.out.println("Secret is: " + secret + " and polynomial is: " + polynomial);
        System.out.println("Distributing shares to peers. Peer i gets share = f(x).");
        Utils.distributeShares(f, idToXMap, Utils.NUM_PEERS, Utils.SERVICE_NAME_PEER,
                Peer.PORT);
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
        return new BigInteger("40");
    }

    private void waitForContinue() throws IOException {
        for (int i = 0; i < Utils.NUM_PEERS; i++) {
            byte[] buffer = new byte[256];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
            datagramSocket.receive(datagramPacket);
        }
    }
}
