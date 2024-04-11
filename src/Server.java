import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    public static Map<String, Double> fromToRate = new HashMap<>(); // {"CAD-USD": 0.7}

    public static void main(String[] args) {
        loadConversions();
        runServer();
    }

    private static void loadConversions() {
        String rateFile = "exchange-rates.rtf";

        try (Scanner scanner = new Scanner(new File(rateFile))) {
            while (scanner.hasNextLine()) {
                processExchangeRateLine(fromToRate, scanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private static void processExchangeRateLine(Map<String, Double> resultContainer, String line) {
        // line: "From Currency: INR, To Currency: CAD, Exchange rate: 0.016"
        String[] parts = line.split(",");

        String fr = null;
        String to = null;
        Double rate = null;

        for (var keyValue : parts) {
            String[] keyValueParts = keyValue.split(":");
            String key = keyValueParts[0].trim();
            String value = keyValueParts[1].trim();

            if (key.equals("From Currency")) {
                fr = value.trim();
            } else if (key.equals("To Currency")) {
                to = value.trim();
            } else if (key.equals("Exchange rate")) {
                rate = Double.parseDouble(value);
            } else {
                throw new RuntimeException("Unknown text: " + keyValue);
            }
        }

        if (fr == null || to == null || rate == null) {
            System.out.println("Cannot process a line: " + line);
        } else {
            String frTo = String.format("%s-%s", fr, to);
            resultContainer.put(
                    frTo, rate
            );

            String frToReversed = String.format("%s-%s", to, fr);
            resultContainer.put(
                    frToReversed, 1/rate
            );
        }
    }

    private static void runServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(9999);
            System.out.println("Server waiting for clients on port 9999");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread handlerThread = new Thread(clientHandler);
                handlerThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

                String messageFromClient;
                while ((messageFromClient = reader.readLine()) != null) {
                    System.out.println("Received from client " + clientSocket.getInetAddress().getHostAddress() + ": " + messageFromClient);
                    writer.println(convertLine(messageFromClient));
                }

                reader.close();
                writer.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static double convertLine(String line) {
            // "CONVERT[100][CAD][USD]"
            try {
                if (line == null || line.isBlank()) {
                    return 0.0;
                }
                String[] parts = line.substring(line.indexOf("[") + 1, line.lastIndexOf("]")).split("\\]\\[");
                System.out.println(parts);
                //parts = ["100", "CAD", "USD"]
                // "CAD-USD", 0.7
                double amount = Double.parseDouble(parts[0]);
                String fr = parts[1];
                String to = parts[2];

                return covertCurrency(amount, fr, to);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }

        private static double covertCurrency(double amount, String fr, String to) {
            String frTo = String.format("%s-%s", fr.toUpperCase(), to.toUpperCase());
            double rate = fromToRate.getOrDefault(frTo, 0.0);
            return amount * rate;
        }
    }
}
//java -cp /Users/zhifenli/Documents/codes/TCP-IPClient-Server-Currency-Converter/exchange-rates.rtf: Server