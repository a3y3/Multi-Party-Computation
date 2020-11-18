import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;

/**
 * In typical Multi Party Computation, there are several clients that store parts of a
 * secret. This class represents one such client.
 * Ideally, this will be run in a docker container.
 */
public class Peer {
    public static final int PORT = 5760;
    int id;
    DatagramSocket datagramSocket;

    public Peer() throws SocketException {
        datagramSocket = new DatagramSocket(PORT);
    }

    public static void main(String[] args) throws IOException {
        Peer peer = new Peer();
        Utils.ShareWrapper shareWrapper = peer.init();
        BigInteger reconstructedSecret = peer.reconstructSecret(shareWrapper);
        System.out.println("Found the secret! Value: " + reconstructedSecret);
        peer.timeout();
        System.out.println("*****");
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
        System.out.println("*****");
        peer.demonstrateBeaverTriples(privateValue);
    }

    private void demonstrateBeaverTriples(int privateValue) throws IOException {
        BigInteger finalResult = new BigInteger("1");
        for (int i = 1; i <= Utils.NUM_PEERS - 1; i += 2) {
            if (id == i || id == i + 1) {
                Polynomial polynomial =
                        new Polynomial(new BigInteger(String.valueOf(privateValue)));
                HashMap<Integer, Integer> idToXMap = Utils.getIDToXWithoutRandomization();
                BigInteger[] f = Utils.getF(polynomial, idToXMap);
                Utils.distributeShares(f, idToXMap, Utils.NUM_PEERS, Utils.SERVICE_NAME_PEER, PORT);
            }
            Utils.ShareWrapper[] shareWrappers = acceptSharesFromNPeers(2);

            BigInteger x_i = shareWrappers[0].share;
            BigInteger y_i = shareWrappers[1].share;

            sendContinueToRunner();

            //There aren't three peers, this is the runner sending 3 separate values.
            shareWrappers = acceptSharesFromNPeers(3);
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
            System.out.println("Overall multiplication: " + finalResult);
        }
        if (id == 5) {
            finalResult =
                    finalResult.multiply(new BigInteger(String.valueOf(privateValue)));
            System.out.println("Final result (including peer 5's secret: " + finalResult);
        }
    }
    

    private BigInteger demonstrateSecretShareSummation(int privateValue) throws IOException {
        Polynomial polynomial =
                new Polynomial(new BigInteger(String.valueOf(privateValue)));
        System.out.println(polynomial);
        HashMap<Integer, Integer> idToXMap = Utils.getIDToXWithoutRandomization();
        BigInteger[] f = Utils.getF(polynomial, idToXMap);
        Utils.distributeShares(f, idToXMap, Utils.NUM_PEERS, Utils.SERVICE_NAME_PEER, PORT);
        Utils.ShareWrapper[] shareWrappers = acceptSharesFromNPeers(5);
        System.out.println("Received shares " + Arrays.toString(shareWrappers));
        Utils.ShareWrapper sharesSummation = addReceivedShares(shareWrappers);
        timeout();
        return reconstructSecret(sharesSummation);
    }

    private Utils.ShareWrapper addReceivedShares(Utils.ShareWrapper[] shareWrappers) {
        BigInteger result = new BigInteger("0");
        for (Utils.ShareWrapper shareWrapper : shareWrappers) {
            result = result.add(shareWrapper.share);
        }
        return new Utils.ShareWrapper(result, id, id);
    }

    private Utils.ShareWrapper init() throws IOException {
        String message = acceptMessage();
        Utils.ShareWrapper shareWrapper = new Utils.ShareWrapper(message);
        id = shareWrapper.id;
        System.out.println("I received secretShare " + shareWrapper.share + ", my id is" +
                " " + shareWrapper.id + " and my x is " + shareWrapper.x);
        return shareWrapper;
    }

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

    private Utils.ShareWrapper[] acceptSharesFromNPeers(int n) throws IOException {
        Utils.ShareWrapper[] shareWrappers = new Utils.ShareWrapper[n];
        for (int i = 0; i < n; i++) {
            String message = acceptMessage();
            shareWrappers[i] = new Utils.ShareWrapper(message);
        }
        return shareWrappers;
    }

    @SuppressWarnings("SameParameterValue")
    private void sendShareToPeer(Utils.ShareWrapper shareWrapper, String peerName,
                                 DatagramSocket socket, int port) throws IOException {
        String message = shareWrapper.toString();
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                InetAddress.getByName(peerName), port);
        socket.send(packet);
    }

    private void broadcastValue(Utils.ShareWrapper shareWrapper) throws IOException {
        DatagramSocket datagramSocket = new DatagramSocket();
        for (int i = 1; i <= Utils.NUM_PEERS; i++) {
            String peerName = Utils.SERVICE_NAME_PEER + "_" + i;
            sendShareToPeer(shareWrapper, peerName, datagramSocket, PORT);
        }
        datagramSocket.close();
    }

    private String acceptMessage() throws IOException {
        byte[] buff = new byte[256];
        DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);
        datagramSocket.receive(datagramPacket);
        return new String(datagramPacket.getData(), 0, datagramPacket.getLength());
    }

    private void sendContinueToRunner() throws IOException {
        DatagramSocket datagramSocket = new DatagramSocket();
        String message = "continue";
        DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(),
                message.length(), InetAddress.getByName(Utils.SERVICE_NAME_RUNNER),
                Runner.PORT);
        datagramSocket.send(datagramPacket);
    }

    private void timeout() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
