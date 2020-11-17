import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Random;

public class Runner {
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
        HashMap<Integer, Integer> idToXMap = Utils.getIDToXMapping();
        BigInteger[] f = Utils.getF(polynomial, idToXMap);
        System.out.println("Secret is: " + secret + " and polynomial is: " + polynomial);
        System.out.println("Distributing shares to peers. Peer i gets share = f(x).");
        Utils.distributeShares(f, idToXMap, Utils.NUM_PEERS, Utils.SERVICE_NAME,
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
        return new BigInteger(numBits, r);
    }
}
