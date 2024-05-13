import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class AdminGUI {
    private JFrame frame;
    private JTextArea textArea;
    private JTextField textField;
    private JButton sendButton;
    private SocketChannel channel;
    private ByteBuffer buffer = ByteBuffer.allocate(256);

    public AdminGUI(String address, int port) throws IOException {
        channel = SocketChannel.open(new InetSocketAddress(address, port));
        initializeUI();
    }

    private void initializeUI() {
        frame = new JFrame("Admin");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 300);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        textField = new JTextField();
        sendButton = new JButton("Send");

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(textField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sendCommand("admin " + textField.getText());
                    textField.setText("");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        frame.setVisible(true);
    }

    private void sendCommand(String command) throws IOException {
        buffer.clear();
        buffer.put(command.getBytes());
        buffer.flip();
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
        readResponse();
    }

    private void readResponse() throws IOException {
        buffer.clear();
        channel.read(buffer);
        buffer.flip(); // Flip the buffer before reading the data
        String response = new String(buffer.array(), 0, buffer.limit()).trim();
        SwingUtilities.invokeLater(() -> textArea.append("Server: " + response + "\n"));
        buffer.clear(); // Clear the buffer after use
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new AdminGUI("localhost", 5000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
