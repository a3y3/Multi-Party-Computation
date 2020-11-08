import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * In typical Multi Party Computation, there are several clients that store parts of a
 * secret. This class represents one such client.
 * Ideally, this will be run in a docker container.
 */
public class Client {
    public static void main(String[] args) {
        Client client = new Client();
        client.enterInfiniteWait();
    }

    private void enterInfiniteWait() {
        System.out.println("Entering 5s wait...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Finished!");
    }
}
