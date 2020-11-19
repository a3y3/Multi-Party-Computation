import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.*;
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
        runner.demonstrateBeaverTriplesNaive();
        runner.demonstrateBeaverTriplesFast();
    }

    private void demonstrateBeaverTriplesNaive() throws IOException {
        waitForContinue();
        Utils.OneMillionBeaverTriples millionBeaverTriples =
                new Utils.OneMillionBeaverTriples();
        System.out.println("Got 1 million triples.");

        int n = millionBeaverTriples.n;
        BigInteger[][] a = new BigInteger[Utils.NUM_PEERS + 1][n];
        BigInteger[][] b = new BigInteger[Utils.NUM_PEERS + 1][n];
        BigInteger[][] c = new BigInteger[Utils.NUM_PEERS + 1][n];

        for (int j = 0; j < n; j++) {
            Polynomial polynomialA = new Polynomial(millionBeaverTriples.a[j]);
            Polynomial polynomialB = new Polynomial(millionBeaverTriples.b[j]);
            Polynomial polynomialC = new Polynomial(millionBeaverTriples.c[j]);
            for (int i = 1; i <= Utils.NUM_PEERS; i++) {
                a[i][j] = polynomialA.f(i);
                b[i][j] = polynomialB.f(i);
                c[i][j] = polynomialC.f(i);
            }
        }
        System.out.println("Split 1 million triples");

        // Send a[i] to peer i.
        for (int i = 1; i <= Utils.NUM_PEERS; i++) {
            Utils.OneMillionBeaverTriples sharesOfTriples =
                    new Utils.OneMillionBeaverTriples(a[i], b[i], c[i]);
            Socket socket =
                    new Socket(InetAddress.getByName(Utils.SERVICE_NAME_PEER + "_" + i),
                            Peer.TCP_PORT);
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
            printWriter.println(sharesOfTriples.toString());
            socket.close();
        }
    }


    private void demonstrateBeaverTriplesFast() throws IOException {
        for (int i = 0; i < 2; i++) {
            waitForContinue();
            int numBits = 20;
            Random r = new Random();
            BigInteger a = new BigInteger(numBits, r);
            BigInteger b = new BigInteger(numBits, r);
            BigInteger c = a.multiply(b);

            Polynomial polynomialA = new Polynomial(a);
            Polynomial polynomialB = new Polynomial(b);
            Polynomial polynomialC = new Polynomial(c);
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
        int numBits = 80;
        Random r = new Random();
        return new BigInteger(numBits, r);
    }

    private void waitForContinue() throws IOException {
        for (int i = 0; i < Utils.NUM_PEERS; i++) {
            byte[] buffer = new byte[256];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
            datagramSocket.receive(datagramPacket);
        }
    }
}
