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
    DatagramSocket datagramSocket = new DatagramSocket(PORT);

    public Peer() throws SocketException {
    }

    public static void main(String[] args) throws IOException {
        Peer peer = new Peer();
        Utils.ShareWrapper shareWrapper = peer.init();
        peer.demonstrateTOverNSecretSharing(shareWrapper);
        peer.timeout();
        System.out.println("*****");
        int privateValue = switch (peer.id) {
            case 1 -> 11;
            case 2 -> 15;
            case 3 -> 28;
            case 4 -> 31;
            default -> 21;
        };
        peer.demonstrateSecretShareSummation(privateValue);
    }

    private void demonstrateSecretShareSummation(int privateValue) throws IOException {
        Polynomial polynomial =
                new Polynomial(new BigInteger(String.valueOf(privateValue)));
        System.out.println(polynomial);
        HashMap<Integer, Integer> idToXMap = Utils.getIDToXWithoutRandomization();
        BigInteger[] f = Utils.getF(polynomial, idToXMap);
        Utils.distributeShares(f, idToXMap, Utils.NUM_PEERS, Utils.SERVICE_NAME, PORT);
        Utils.ShareWrapper[] shareWrappers = acceptSharesFromNPeers(5);
        System.out.println("Received shares " + Arrays.toString(shareWrappers));
        Utils.ShareWrapper sharesSummation = addReceivedShares(shareWrappers);
        timeout();
        demonstrateTOverNSecretSharing(sharesSummation);
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

    private void demonstrateTOverNSecretSharing(Utils.ShareWrapper shareWrapper) throws IOException {
        if (id == 3) {
            System.out.println("I will demonstrate reconstruction by asking peers 2, 4 " +
                    "and 5 to send me their shares. Note that I need three or more " +
                    "peers to reconstruct the secret, since the reconstruction function" +
                    " has 3 parameters.");
            Utils.ShareWrapper[] shareWrappers = acceptSharesFromNPeers(3);
            System.out.println("Received shares " + Arrays.toString(shareWrappers));
            BigInteger[] y = new BigInteger[shareWrappers.length];
            int[] x = new int[shareWrappers.length];
            for (int i = 0; i < shareWrappers.length; i++) {
                x[i] = shareWrappers[i].x;
                y[i] = shareWrappers[i].share;
            }
            BigInteger reconstructedSecret = Polynomial.calculateSecret(x, y, 2);
            System.out.println("Found the secret! Value: " + reconstructedSecret);
        } else if (id == 2 || id == 4 || id == 5) {
            String peerName = Utils.SERVICE_NAME + "_" + 3;
            sendShareToPeer(shareWrapper, peerName, PORT);
        }
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
                                 int port) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        String message = shareWrapper.toString();
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                InetAddress.getByName(peerName), port);
        socket.send(packet);
    }

    private String acceptMessage() throws IOException {
        byte[] buff = new byte[256];
        DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);
        datagramSocket.receive(datagramPacket);
        return new String(datagramPacket.getData(), 0, datagramPacket.getLength());
    }

    private void timeout() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
