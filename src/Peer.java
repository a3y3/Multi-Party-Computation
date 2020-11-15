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
    int id;

    public static void main(String[] args) throws IOException {
        Peer peer = new Peer();
        peer.demonstrateTOverNSecretSharing();
    }

    private void demonstrateTOverNSecretSharing() throws IOException {
        DatagramSocket datagramSocket = new DatagramSocket(PORT);
        String message = acceptMessage(datagramSocket);
        datagramSocket.close();

        Utils.ShareWrapper shareWrapper = new Utils.ShareWrapper(message);
        id = shareWrapper.id;
        System.out.println("I received secretShare " + shareWrapper.share + ", my id is" +
                " " + shareWrapper.id + " and my x is " + shareWrapper.x);
        if (id == 3) {
            System.out.println("I will demonstrate reconstruction by asking peers 2, 4 " +
                    "and 5 to send me their shares.");
            Utils.ShareWrapper[] shareWrappers = acceptSharesFromThreePeers();
            BigInteger[] y = new BigInteger[shareWrappers.length];
            int[] x = new int[shareWrappers.length];
            for (int i = 0; i < shareWrappers.length; i++) {
                x[i] = shareWrappers[i].x;
                y[i] = shareWrappers[i].share;
            }
            BigInteger reconstructedSecret = Polynomial.calculateSecret(x, y, 2);
            System.out.println("Found the secret! Value: " + reconstructedSecret);
        } else if (id == 2 || id == 4 || id == 5) {
            // Ensure that id==3 has a socket up and running before sending a share.
            try {
                Thread.sleep(1000);
                String peerName = Utils.SERVICE_NAME + "_" + 3;
                sendShareToPeer(shareWrapper, peerName, PORT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Utils.ShareWrapper[] acceptSharesFromThreePeers() throws IOException {
        Utils.ShareWrapper[] shareWrappers = new Utils.ShareWrapper[3];
        DatagramSocket datagramSocket = new DatagramSocket(PORT);
        for (int i = 0; i < 3; i++) {
            String message = acceptMessage(datagramSocket);
            shareWrappers[i] = new Utils.ShareWrapper(message);
        }
        datagramSocket.close();
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

    @SuppressWarnings("SameParameterValue")
    private String acceptMessage(DatagramSocket datagramSocket) throws IOException {
        byte[] buff = new byte[256];
        DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);
        datagramSocket.receive(datagramPacket);
        return new String(datagramPacket.getData(), 0, datagramPacket.getLength());
    }
}
