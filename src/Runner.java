import java.math.BigInteger;

public class Runner implements RunnerInterface {
    BigInteger secret;

    public static void main(String[] args) {
        Runner runner = new Runner();
        runner.secret = runner.getSecretFromUser();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger getSecretFromUser() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger[] getF(Polynomial polynomial) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void distributeShares(BigInteger[] f, String serviceName) {

    }
}
