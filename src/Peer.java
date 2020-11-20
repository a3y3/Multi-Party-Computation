import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * In typical Multi Party Computation, there are several clients that store parts of a
 * secret. This class represents one such client.
 * Ideally, this will be run in a docker container.
 */
public class Peer {
    public static final int PORT = 5760;
    public static final int TCP_PORT = 4874;
    int id;
    DatagramSocket datagramSocket;

    public Peer() throws SocketException {
        datagramSocket = new DatagramSocket(PORT);
    }

    /**
     * Calls 4 functions:
     * - {@code reconstructSecret()}, to demonstrate reconstruction of a secret given n
     * equations and n variables.
     * - {@code demonstrateSecretShareSummation()}, to demonstrate addition of several
     * secrets given only the shares.
     * - {@code demonstrateBeaverTriplesNaive()}, a naive way to multiply secrets given
     * only the shares.
     * - {@code demonstrateBeaverTriplesFast()}, a slightly faster way to multiply
     * secrets.
     * @param args STDIN, ignored.
     * @throws IOException for socket.send().
     */
    public static void main(String[] args) throws IOException {
        System.out.println("********* Demonstration 1: Secret reconstruction *********");
        Peer peer = new Peer();
        Utils.ShareWrapper shareWrapper = peer.init();
        BigInteger reconstructedSecret = peer.reconstructSecret(shareWrapper);
        System.out.println("Found the secret! Value: " + reconstructedSecret);
        peer.timeout();
        System.out.println("********* Demonstration 2: Secret summation *********");
        int privateValue = switch (peer.id) {
            case 1 -> 11;
            case 2 -> 15;
            case 3 -> 28;
            case 4 -> 31;
            default -> 21;
        };
        BigInteger summation = peer.demonstrateSecretShareSummation(privateValue);
        System.out.println("Found the summation! Value: " + summation);

        peer.timeout();
        System.out.println("********* Demonstration 3: Secret multiplication " +
                "(Naive version) *********");
        long start = System.nanoTime();
        peer.demonstrateBeaverTriplesNaive(privateValue);
        long end = System.nanoTime();
        long elapsedTime = end - start;
        System.out.println("Sequential execution took: " +
                TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "s.");

        peer.timeout();
        System.out.println("********* Demonstration 4: Secret multiplication (Fast " +
                "version) *********");
        start = System.nanoTime();
        peer.demonstrateBeaverTriplesFast(privateValue);
        end = System.nanoTime();
        elapsedTime = end - start;
        System.out.println("Parallel execution took: " +
                TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "s.");
    }

    /**
     * This function accepts a million triples from a trusted dealer (Runner.java) and
     * picks 2 triples out of them to do the multiplication.
     * We need to do the multiplication of secrets of containers 1-5. The approach is:
     *      Containers 1 and 2 create shares for their private values and distribute
     *      them. All the containers use beaver triple multiplication to multiply them
     *      and store the answer.
     *      Containers 3 and 4 repeat the process, and multiply their result with the
     *      result acquired in the step above.
     *      Container 5 then multiplies the result acquired above with its own private
     *      value, thus getting the multiplication of all the private shares.
     * @param privateValue the private value for this class.
     * @throws IOException on socket.send()
     */
    private void demonstrateBeaverTriplesNaive(int privateValue) throws IOException {
        BigInteger finalResult = new BigInteger("1");
        Utils.OneMillionBeaverTriples millionTriples = null;
        for (int i = 1; i <= Utils.NUM_PEERS - 1; i += 2) {
            if (id == i || id == i + 1) {
                Polynomial polynomial =
                        new Polynomial(new BigInteger(String.valueOf(privateValue)));
                HashMap<Integer, Integer> idToXMap = Utils.getIDToXWithoutRandomization();
                BigInteger[] f = Utils.getF(polynomial, idToXMap);
                try {
                    Thread.sleep(id * 100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Utils.distributeShares(f, idToXMap, Utils.NUM_PEERS, Utils.SERVICE_NAME_PEER, PORT);
            }
            Utils.ShareWrapper[] shareWrappers = acceptSharesFromNPeers(2);

            BigInteger x_i = shareWrappers[0].share;
            BigInteger y_i = shareWrappers[1].share;

            if (i == 1) {
                sendContinueToRunner();
                ServerSocket serverSocket = new ServerSocket(TCP_PORT);
                millionTriples = acceptMillionTriples(serverSocket);
                serverSocket.close();
            }

            BigInteger a = millionTriples.a[i];
            BigInteger b = millionTriples.b[i];
            BigInteger c = millionTriples.c[i];

            shareWrappers = new Utils.ShareWrapper[3];
            shareWrappers[0] = new Utils.ShareWrapper(a, id, id);
            shareWrappers[1] = new Utils.ShareWrapper(b, id, id);
            shareWrappers[2] = new Utils.ShareWrapper(c, id, id);

            BigInteger a_i = shareWrappers[0].share;
            BigInteger b_i = shareWrappers[1].share;
            BigInteger c_i = shareWrappers[2].share;

            BigInteger differenceXA = x_i.subtract(a_i);
            BigInteger differenceYB = y_i.subtract(b_i);

            timeout();
            BigInteger xPrime = reconstructSecret(new Utils.ShareWrapper(differenceXA,
                    id, id));
            System.out.println("xPrime: " + xPrime);

            timeout();
            BigInteger yPrime = reconstructSecret(new Utils.ShareWrapper(differenceYB,
                    id, id));
            System.out.println("yPrime: " + yPrime);

            BigInteger xPrimeBi = xPrime.multiply(b_i);
            BigInteger yPrimeAi = yPrime.multiply(a_i);
            BigInteger xPrimeYPrime = xPrime.multiply(yPrime);
            BigInteger z_i = c_i.add(xPrimeBi).add(yPrimeAi).add(xPrimeYPrime);

            timeout();
            BigInteger result = reconstructSecret(new Utils.ShareWrapper(z_i, id, id));
            System.out.println("Sub multiplication: " + result);

            finalResult = finalResult.multiply(result);
            System.out.println("Multiplication so far: " + finalResult);
        }
        if (id == 5) {
            finalResult =
                    finalResult.multiply(new BigInteger(String.valueOf(privateValue)));
            System.out.println("Final result (including peer 5's secret): " + finalResult);
        }
    }

    /**
     * Slightly improves upon {@code demonstrateBeaverTriplesNaive()} by only getting
     * triples from the dealer as necessary (who uses PRNG to randomize the triples
     * every time). Also, this function gets the shares of the private values of all
     * the containers at once, and then multiplies them together, resulting in a faster
     * overall execution.
     * @param privateValue the private value for this class.
     * @throws IOException on socket.send()
     */
    private void demonstrateBeaverTriplesFast(int privateValue) throws IOException {
        if (id != 5) {
            Polynomial polynomial =
                    new Polynomial(new BigInteger(String.valueOf(privateValue)));
            HashMap<Integer, Integer> idToXMap = Utils.getIDToXWithoutRandomization();
            BigInteger[] f = Utils.getF(polynomial, idToXMap);
            try {
                Thread.sleep(id * 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Utils.distributeShares(f, idToXMap, Utils.NUM_PEERS, Utils.SERVICE_NAME_PEER, PORT);
        }
        Utils.ShareWrapper[] shareWrappers = acceptSharesFromNPeers(4);

        BigInteger x_1_i = shareWrappers[0].share;
        BigInteger y_1_i = shareWrappers[1].share;
        BigInteger x_2_i = shareWrappers[2].share;
        BigInteger y_2_i = shareWrappers[3].share;

        sendContinueToRunner();

        //There aren't three peers, this is the runner sending 3 separate values.
        shareWrappers = acceptSharesFromNPeers(3);
        BigInteger a_1_i = shareWrappers[0].share;
        BigInteger b_1_i = shareWrappers[1].share;
        BigInteger c_1_i = shareWrappers[2].share;

        sendContinueToRunner();

        shareWrappers = acceptSharesFromNPeers(3);
        BigInteger a_2_i = shareWrappers[0].share;
        BigInteger b_2_i = shareWrappers[1].share;
        BigInteger c_2_i = shareWrappers[2].share;

        BigInteger differenceXA_1 = x_1_i.subtract(a_1_i);
        BigInteger differenceYB_1 = y_1_i.subtract(b_1_i);
        BigInteger differenceXA_2 = x_2_i.subtract(a_2_i);
        BigInteger differenceYB_2 = y_2_i.subtract(b_2_i);

        timeout();
        BigInteger xPrime1 = reconstructSecret(new Utils.ShareWrapper(differenceXA_1,
                id, id));
        System.out.println("xPrime1: " + xPrime1);

        timeout();
        BigInteger yPrime1 = reconstructSecret(new Utils.ShareWrapper(differenceYB_1,
                id, id));
        System.out.println("yPrime1: " + yPrime1);

        timeout();
        BigInteger xPrime2 = reconstructSecret(new Utils.ShareWrapper(differenceXA_2,
                id, id));
        System.out.println("xPrime2: " + xPrime2);

        timeout();
        BigInteger yPrime2 = reconstructSecret(new Utils.ShareWrapper(differenceYB_2,
                id, id));
        System.out.println("yPrime2: " + yPrime2);


        BigInteger xPrimeBi1 = xPrime1.multiply(b_1_i);
        BigInteger yPrimeAi1 = yPrime1.multiply(a_1_i);
        BigInteger xPrimeYPrime1 = xPrime1.multiply(yPrime1);
        BigInteger z_i_1 = c_1_i.add(xPrimeBi1).add(yPrimeAi1).add(xPrimeYPrime1);

        BigInteger xPrimeBi2 = xPrime2.multiply(b_2_i);
        BigInteger yPrimeAi2 = yPrime2.multiply(a_2_i);
        BigInteger xPrimeYPrime2 = xPrime2.multiply(yPrime2);
        BigInteger z_i_2 = c_2_i.add(xPrimeBi2).add(yPrimeAi2).add(xPrimeYPrime2);

        timeout();
        BigInteger result1 = reconstructSecret(new Utils.ShareWrapper(z_i_1, id, id));
        System.out.println("Sub multiplication1: " + result1);

        timeout();
        BigInteger result2 = reconstructSecret(new Utils.ShareWrapper(z_i_2, id, id));
        System.out.println("Sub multiplication2: " + result2);


        if (id == 5) {
            BigInteger finalResult = result1.multiply(result2);
            finalResult = finalResult.multiply(new BigInteger(String.valueOf(privateValue)));
            System.out.println("Final result (including peer 5's secret): " + finalResult);
        }
    }

    /**
     * Accepts shares of private values from all containers and adds them together. The
     * function then calls {@code reconstructSecret()} to calculate the secret for the
     * new polynomial, which results in adding up all the shares.
     * @param privateValue the private value for this class.
     * @return the summation of all the private values.
     * @throws IOException on socket.send().
     */
    private BigInteger demonstrateSecretShareSummation(int privateValue) throws IOException {
        Polynomial polynomial =
                new Polynomial(new BigInteger(String.valueOf(privateValue)));
        System.out.println(polynomial);
        HashMap<Integer, Integer> idToXMap = Utils.getIDToXWithoutRandomization();
        BigInteger[] f = Utils.getF(polynomial, idToXMap);
        Utils.distributeShares(f, idToXMap, Utils.NUM_PEERS, Utils.SERVICE_NAME_PEER, PORT);
        Utils.ShareWrapper[] shareWrappers = acceptSharesFromNPeers(5);
        Utils.ShareWrapper sharesSummation = addReceivedShares(shareWrappers);
        timeout();
        return reconstructSecret(sharesSummation);
    }

    /**
     * Utility method for {@code demonstrateSecretShareSummation()}. This function adds
     * all the shares and returns the result.
     * @param shareWrappers a list of shares whose value is to be added.
     * @return a {@code ShareWrapper} object whose share is the summation of all the
     * shares inside {@code shareWrappers}.
     */
    private Utils.ShareWrapper addReceivedShares(Utils.ShareWrapper[] shareWrappers) {
        BigInteger result = new BigInteger("0");
        for (Utils.ShareWrapper shareWrapper : shareWrappers) {
            result = result.add(shareWrapper.share);
        }
        return new Utils.ShareWrapper(result, id, id);
    }

    /**
     * Serves to accept the first message from Runner. Note that this function is
     * important because it sets the id of the Peer. The id is used later in many
     * functions, so this function is critical and must not be omitted.
     * @return A {@code ShareWrapper} object that contains the id of the peer and the
     * share for testing reconstruction of the secret.
     * @throws IOException see {@code acceptMessage()}.
     */
    private Utils.ShareWrapper init() throws IOException {
        String message = acceptMessage();
        Utils.ShareWrapper shareWrapper = new Utils.ShareWrapper(message);
        id = shareWrapper.id;
        System.out.println("I received secretShare " + shareWrapper.share + ", my id is" +
                " " + shareWrapper.id + " and my x is " + shareWrapper.x);
        return shareWrapper;
    }

    /**
     * Given n equations and n variables, we can find any co-efficients of a polynomial
     * f. Since the secret is encoded at f(0), we are only interested in that.
     * This function gathers shares from everyone else (and also broadcasts it's own
     * shares to everyone else) and reconstructs the secret by calling {@code
     * Polynomial.calculateSecret()}.
     * @param shareWrapper The share of this peer. Mainly used for broadcasting it to
     *                     everyone else.
     * @return the calculated secret, ie. f(0).
     * @throws IOException on socket.send().
     */
    private BigInteger reconstructSecret(Utils.ShareWrapper shareWrapper) throws IOException {
        broadcastValue(shareWrapper);
        Utils.ShareWrapper[] shareWrappers = acceptSharesFromNPeers(Utils.NUM_PEERS);
        BigInteger[] y = new BigInteger[shareWrappers.length];
        int[] x = new int[shareWrappers.length];
        for (int i = 0; i < shareWrappers.length; i++) {
            x[i] = shareWrappers[i].x;
            y[i] = shareWrappers[i].share;
        }
        return Polynomial.calculateSecret(x, y, 2);
    }

    /**
     * Accepts one million triples from the Runner.
     * @param serverSocket a TCP socket used to accept an incoming connection from the
     *                     Runner. It's the responsibility of the caller of this
     *                     function to close this socket after use.
     * @return A new object of {@code OneMillionBeaverTriples}.
     * @throws IOException on serverSocket.accept().
     */
    private Utils.OneMillionBeaverTriples acceptMillionTriples(ServerSocket serverSocket) throws IOException {
        Socket clientSocket = serverSocket.accept();
        BufferedReader in =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String triples = in.readLine();
        return new Utils.OneMillionBeaverTriples(triples);
    }

    /**
     * A for loop in this function accepts shares from {@code n} peers and returns an
     * array of the received shares.
     * @param n the number of peers to accept shares from.
     * @return an array of the received shares.
     * @throws IOException see {@code acceptMessage()}.
     */
    private Utils.ShareWrapper[] acceptSharesFromNPeers(int n) throws IOException {
        Utils.ShareWrapper[] shareWrappers = new Utils.ShareWrapper[n];
        for (int i = 0; i < n; i++) {
            String message = acceptMessage();
            shareWrappers[i] = new Utils.ShareWrapper(message);
        }
        return shareWrappers;
    }

    /**
     * Given a share, sends it to {@code peerName}.
     * @param shareWrapper the share to be sent.
     * @param peerName the name of the peer to send it to.
     * @param socket used for sending the share.
     * @param port the port on which the share is to be sent to.
     * @throws IOException on socket.send().
     */
    @SuppressWarnings("SameParameterValue")
    private void sendShareToPeer(Utils.ShareWrapper shareWrapper, String peerName,
                                 DatagramSocket socket, int port) throws IOException {
        String message = shareWrapper.toString();
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                InetAddress.getByName(peerName), port);
        socket.send(packet);
    }

    /**
     * Uses a for loop to send a share to everyone else in the group.
     * @param shareWrapper the share to be broadcasted.
     * @throws IOException see {@code sendShareToPeer()}
     */
    private void broadcastValue(Utils.ShareWrapper shareWrapper) throws IOException {
        DatagramSocket datagramSocket = new DatagramSocket();
        for (int i = 1; i <= Utils.NUM_PEERS; i++) {
            String peerName = Utils.SERVICE_NAME_PEER + "_" + i;
            sendShareToPeer(shareWrapper, peerName, datagramSocket, PORT);
        }
        datagramSocket.close();
    }

    /**
     * Accepts a single UDP message.
     * @return the received message.
     * @throws IOException on {@code datagramSocket.receive()}
     */
    private String acceptMessage() throws IOException {
        byte[] buff = new byte[256];
        DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);
        datagramSocket.receive(datagramPacket);
        return new String(datagramPacket.getData(), 0, datagramPacket.getLength());
    }

    /**
     * The runner might need to wait for peers to do some processing. This function
     * sends a continue message to runner to signal it to resume operations.
     * @throws IOException on {@code datagramSocket.send()}
     */
    private void sendContinueToRunner() throws IOException {
        DatagramSocket datagramSocket = new DatagramSocket();
        String message = "continue";
        DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(),
                message.length(), InetAddress.getByName(Utils.SERVICE_NAME_RUNNER),
                Runner.PORT);
        datagramSocket.send(datagramPacket);
    }

    /**
     * Used to establish a "happens before" relation between multi node peers. For
     * example, peer1 creates-a-socket-before peer2 sends a messages, or peer3
     * sends-a-message-before peer4 sends it.
     */
    private void timeout() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
