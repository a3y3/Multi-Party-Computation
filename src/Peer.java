import java.io.IOException;
import java.math.BigInteger;
import java.net.*;

/**
 * In typical Multi Party Computation, there are several clients that store parts of a
 * secret. This class represents one such client.
 * Ideally, this will be run in a docker container.
 */
public class Peer {
    public static final int PORT = 5760;
    public static final String DELIMITER = ";";

    private static class ShareIDWrapper {
        BigInteger share;
        int id;

        public ShareIDWrapper(String message) {
            this.share = new BigInteger(message.split(DELIMITER)[0]);
            this.id = Integer.parseInt(message.split(DELIMITER)[1]);
        }
    }

    public static void main(String[] args) throws IOException {
        Peer peer = new Peer();
        peer.demonstrateTOverNSecretSharing();
    }

    private void demonstrateTOverNSecretSharing() throws IOException {
        String message = acceptMessage(PORT);
        ShareIDWrapper shareIDWrapper = new ShareIDWrapper(message);
        int id = shareIDWrapper.id;
        System.out.println("I received secretShare " + shareIDWrapper.share + ", and my ID " +
                "is: " + id);
        if (id == 3) {
            System.out.println("I will demonstrate reconstruction by asking peers 2, 4 " +
                            "and 5 to send me their shares.");
            ShareIDWrapper[] shareIDWrappers = acceptSharesFromThreePeers();
            BigInteger[] y = new BigInteger[shareIDWrappers.length];
            int[] x = new int[shareIDWrappers.length];
            for (int i = 0; i < shareIDWrappers.length; i++){
                x[i] = shareIDWrappers[i].id;
                y[i] = shareIDWrappers[i].share;
            }
            BigInteger reconstructedSecret = Polynomial.calculateSecret(x, y, 2);
            System.out.println("Found the secret! Value: " + reconstructedSecret);
        } else if (id == 2 || id == 4 || id == 5) {
            sendShareToPeer(shareIDWrapper.share, id, 3);
        }
    }

    private ShareIDWrapper[] acceptSharesFromThreePeers() throws IOException {
        ShareIDWrapper[] shareIDWrappers = new ShareIDWrapper[3];
        for (int i = 0; i < 3; i++) {
            String message = acceptMessage(PORT);
            shareIDWrappers[i] = new ShareIDWrapper(message);
        }
        return shareIDWrappers;
    }

    @SuppressWarnings("SameParameterValue")
    private void sendShareToPeer(BigInteger secretShare, int fromPeerID, int toPeerID) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        String peerName = Runner.SERVICE_NAME + "_" + toPeerID;
        String share = String.valueOf(secretShare);
        share += Peer.DELIMITER + fromPeerID;
        byte[] buffer = share.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                InetAddress.getByName(peerName), PORT);
        socket.send(packet);
    }

    @SuppressWarnings("SameParameterValue")
    private String acceptMessage(int port) throws IOException {
        DatagramSocket datagramSocket = new DatagramSocket(port);
        byte[] buff = new byte[256];
        DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);
        datagramSocket.receive(datagramPacket);
        datagramSocket.close();
        return new String(datagramPacket.getData(), 0, datagramPacket.getLength());
    }
}
