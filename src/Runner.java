import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class Runner {
    public static void main(String[] args) {
        System.out.println("Welcome");
        InetAddress address = null;
        try {
            Socket clientSocket = new Socket("peer", 6666);
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            String message = "Hello WOrld!";
            out.println(message);
            System.out.println("Sent message successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
