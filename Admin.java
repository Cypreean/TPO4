import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Admin {
    private SocketChannel channel;
    private ByteBuffer buffer = ByteBuffer.allocate(256);

    public Admin(String address, int port) throws IOException {
        channel = SocketChannel.open(new InetSocketAddress(address, port));
    }

    public void sendCommand(String command) throws IOException {
        buffer.clear();
        buffer.put(command.getBytes());
        buffer.flip();
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    public void readResponse() throws IOException {
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
            Admin admin = new Admin("localhost", 5000);
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter admin commands (addtopic <topic>, removetopic <topic>, send <topic> <message>):");

            while (true) {
                String input = scanner.nextLine();
                if (input.equals("exit")) break;
                admin.sendCommand("admin " + input);  // Prefix all admin commands with 'admin'
                admin.readResponse();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
