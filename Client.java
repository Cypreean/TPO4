import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client {
    private SocketChannel channel;
    private ByteBuffer buffer = ByteBuffer.allocate(256);

    public Client(String address, int port) throws IOException {
        channel = SocketChannel.open(new InetSocketAddress(address, port));
    }

    public void sendMessage(String msg) throws IOException {
        buffer.clear();
        buffer.put(msg.getBytes());
        buffer.flip();
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    public void readMessage() throws IOException {
        buffer.clear();
        channel.read(buffer);
        buffer.flip(); // Flip the buffer before reading the data
        int limit = buffer.limit();
        byte[] bytes = new byte[limit];
        buffer.get(bytes);
        System.out.println("Server: " + new String(bytes).trim());
        buffer.clear(); // Clear the buffer after use
    }


    public static void main(String[] args) {
        try {
            Client client = new Client("localhost", 5000);
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Enter command (subscribe <topic>, unsubscribe <topic>, check):");
                String message = scanner.nextLine();
                client.sendMessage(message);
                client.readMessage();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
