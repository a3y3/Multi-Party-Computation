import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * In typical Multi Party Computation, there are several clients that store parts of a
 * secret. This class represents one such client.
 * Ideally, this will be run in a docker container.
 */
public class Peer {
    public static void main(String[] args) {
        Peer peer = new Peer();
        System.out.println("Hi");
        peer.acceptMessage();
    }

    private void acceptMessage() {
        try {
            DatagramSocket datagramSocket  = new DatagramSocket(6666);
            byte[] buff = new byte[256];
            DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);
            datagramSocket.receive(datagramPacket);
            String message = new String(datagramPacket.getData(), 0,
                    datagramPacket.getLength());
            System.out.println("Peer received: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
