import java.io.IOException;
import java.math.BigInteger;
import java.net.*;

public class Runner implements RunnerInterface {

    private static final int NUM_PEERS = 5;
    private static final String SERVICE_NAME = "multi-party-computation_peer";
    BigInteger secret;

    public static void main(String[] args) throws IOException {
        Runner runner = new Runner();
        runner.secret = runner.getSecretFromUser();
        Polynomial polynomial = new Polynomial(runner.secret);
        BigInteger[] f = runner.getF(polynomial);
        runner.distributeShares(f, NUM_PEERS, SERVICE_NAME, Peer.PORT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger getSecretFromUser() {
        return new BigInteger("40");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger[] getF(Polynomial polynomial) {
        BigInteger[] result = new BigInteger[NUM_PEERS + 1];
        for (int i = 0; i <= NUM_PEERS; i++) {
            result[i] = polynomial.f(i);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void distributeShares(BigInteger[] f, int numPeers, String serviceName,
                                 int port) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        DatagramPacket packet;
        //peer i gets share f[i]
        for (int i = 1; i <= NUM_PEERS; i ++){
            String peerName = serviceName + "_" + i;
            String share = String.valueOf(f[i]);
            byte[] buffer = share.getBytes();
            packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(peerName), port);
            socket.send(packet);
        }
    }
}
