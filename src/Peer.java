import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

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
            ServerSocket serverSocket = new ServerSocket(6666);
            Socket clientSocket = serverSocket.accept();
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = in.readLine();
            System.out.println("Peer received: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
