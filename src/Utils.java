import java.math.BigInteger;

public class Utils {
    public static final String DELIMITER = ";";
    public static final String SERVICE_NAME = "multi-party-computation_peer";

    public static class ShareWrapper {
        BigInteger share;
        int id;
        int x;

        public ShareWrapper(BigInteger share, int id, int x) {
            this.share = share;
            this.id = id;
            this.x = x;
        }

        public ShareWrapper(String message) {
            String[] splitMessage = message.split(DELIMITER);
            this.share = new BigInteger(splitMessage[0]);
            this.id = Integer.parseInt(splitMessage[1]);
            this.x = Integer.parseInt(splitMessage[2]);
        }

        @Override
        public String toString() {
            return share + DELIMITER + id + DELIMITER + x;
        }
    }
}
