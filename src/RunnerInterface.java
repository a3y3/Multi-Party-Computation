public interface RunnerInterface {
    /**
     * For the secret sharing protocol, the Runner class has the secret. This secret is
     * taken from the user if specified, or defaulted to 50.
     *
     * @return the secret.
     */
    int getSecretFromUser();

    /**
     * Calcluates the value of f(i) for i in range 0 to n, where n is the number of peers.
     *
     * @param polynomial the current Polynomial object.
     * @return an array such that f[i] = f(i), and f is the current polynomial.
     */
    int[] getF(Polynomial polynomial);

    /**
     * Sends the shares to the corresponding peer. share i is sent to peer i, along
     * with i (so that the peer is aware of its position in the group)
     * @param f an array such that f[i] = f(i). See {@code getSecretFromUser()}
     * @param serviceName the host name of the service. Used for InetAddress.getByName().
     */
    void distributeShares(int[] f, String serviceName);
}
