import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Server {
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private final int port = 5000;
    private final Map<SocketChannel, Set<String>> subscriptions = new HashMap<>();
    private final Set<String> validTopics = new HashSet<>();
    private final Map<String, List<String>> topicNews = new HashMap<>();

    public Server() throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void run() throws IOException {
        System.out.println("Server is running on port " + port);

        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();

                if (key.isAcceptable()) {
                    acceptClient();
                } else if (key.isReadable()) {
                    readData(key);
                }
                iterator.remove();
            }
        }
    }

    private void acceptClient() throws IOException {
        SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        subscriptions.put(client, new HashSet<>());
        System.out.println("Accepted new connection from client: " + client);
    }

    private void readData(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(256);
        int bytesRead = client.read(buffer);

        if (bytesRead == -1) {
            client.close();
            subscriptions.remove(client);
            System.out.println("Closed connection from client: " + client);
        } else {
            buffer.flip();
            String message = new String(buffer.array(), buffer.position(), buffer.limit()).trim();
            System.out.println("Message from client: " + message);
            if (message.startsWith("admin")) {
                handleAdminCommand(message, client);
            } else {
                handleCommand(message, client);
            }
        }
    }

    private void handleCommand(String message, SocketChannel client) throws IOException {
        String[] command = message.split(" ", 2);
        String response = "Invalid command";
        if (command.length < 2) {
            if (command[0].equalsIgnoreCase("check")) {
                response = "Subscriptions: " + subscriptions.get(client);
            }
        } else {
            switch (command[0].toLowerCase()) {
                case "subscribe":
                    if (validTopics.contains(command[1])) {
                        subscriptions.get(client).add(command[1]);
                        response = "Subscribed to " + command[1];
                    } else {
                        response = "Invalid topic";
                    }
                    break;
                case "unsubscribe":
                    if (subscriptions.get(client).remove(command[1])) {
                        response = "Unsubscribed from " + command[1];
                    } else {
                        response = "Not subscribed to " + command[1];
                    }
                    break;
                case "news":
                    response = getNews(command[1]);
                    break;

                default:
                    response = "Unknown command";
            }
        }

        sendResponse(client, response);
    }
    private String getNews(String topic) {
        List<String> news = topicNews.getOrDefault(topic, new ArrayList<>());
        if (news.isEmpty()) {
            return "No news for topic: " + topic;
        }
        return "News for " + topic + ": " + String.join("\n ", news);
    }

    private void handleAdminCommand(String message, SocketChannel client) throws IOException {
        String[] parts = message.split(" ", 4); // Split into parts for admin commands
        String response = "Invalid admin command";
        if (parts.length < 3) return;

        switch (parts[1].toLowerCase()) {
            case "addtopic":
                addTopic(parts[2]);
                response = "Topic added: " + parts[2];
                break;
            case "removetopic":
                removeTopic(parts[2]);
                response = "Topic removed: " + parts[2];
                break;
            case "send":
                List<String> news = topicNews.getOrDefault(parts[2], new ArrayList<>());
                news.add(parts[3]);
                topicNews.put(parts[2], news);
                response = "Message sent to topic " + parts[2];
                break;
        }

        sendResponse(client, response);
    }

    private void addTopic(String topic) {
        validTopics.add(topic);
    }

    private void removeTopic(String topic) {
        validTopics.remove(topic);
        subscriptions.values().forEach(set -> set.remove(topic)); // Remove topic from all subscriptions
    }



    private void sendResponse(SocketChannel client, String response) throws IOException {
        ByteBuffer responseBuffer = ByteBuffer.allocate(256);
        responseBuffer.put(response.getBytes());
        responseBuffer.flip();
        while (responseBuffer.hasRemaining()) {
            client.write(responseBuffer);
        }
    }

    public static void main(String[] args) {
        try {
            new Server().run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
