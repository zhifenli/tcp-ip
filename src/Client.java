import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 9999);
            System.out.println("Connected to server");

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            String inputLine;

            while ((inputLine = userInput.readLine()) != null) {
                if (inputLine.equalsIgnoreCase("QUIT")) {
                    break;
                }

                writer.println(inputLine);

                String messageFromServer = reader.readLine();
                System.out.println("Server response: " + messageFromServer);
            }

            reader.close();
            writer.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
