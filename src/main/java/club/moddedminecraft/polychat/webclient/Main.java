package club.moddedminecraft.polychat.webclient;

import club.moddedminecraft.polychat.networking.io.*;
import club.moddedminecraft.polychat.webclient.threads.InputThread;
import club.moddedminecraft.polychat.webclient.threads.ReattachThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class Main {

    //Used to determine whether the server cleanly shutdown or crashed
    public static boolean shutdownClean = false;

    public static Properties properties;
    public static MessageBus messageBus;
    public static ReattachThread reattachThread;
    public static InputThread inputThread;
    public static Webserver webserver;
    public static ArrayList<String> messages;

    public static void main(String[] args) {
        Main.handleConfiguration();
        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdownHook));

        reattachThread = new ReattachThread(5000);
        inputThread = new InputThread(10);
        webserver = new Webserver();
        messages = new ArrayList<>();

        reattachThread.start();
        reattachThread.sendServerOnline();
        inputThread.start();
    }

    public static void shutdownHook() {
        reattachThread.interrupt();
        short exitVal;
        //Sends either crashed or offline depending on if shutdown happened cleanly
        if (shutdownClean) {
            exitVal = 2;
        } else {
            exitVal = 3;
        }
        ClientStatusMessage statusMessage = new ClientStatusMessage(properties.getProperty("web_id", "DEFAULT_ID"), exitVal);
        Main.sendMessage(statusMessage);
        try {
            //Makes sure message has time to send
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        messageBus.stop();
    }

    public static void sendMessage(AbstractMessage message) {
        try {
            messageBus.sendMessage(message);
        } catch (NullPointerException ignored) {
        }
    }

    public static void sendChat(String message) {
        if (message.equals("stop")) {
            Main.shutdownClean = true;
            System.exit(0);
        }
        String username = Main.properties.getProperty("web_name", "DEFAULT_NAME") + ":";
        if (Main.reattachThread.isConnected()) {
            ChatMessage chatMessage = new ChatMessage(username, message, "web");
            Main.sendMessage(chatMessage);
        }
        messages.add(username + " " + message);
    }

    public static void handleConfiguration() {
        properties = new Properties();
        File config = new File("polychat-web.properties");

        //Loads config if it exists or creates a default one if not
        if (config.exists() && config.isFile()) {
            try (FileInputStream istream = new FileInputStream(config)) {
                properties.load(istream);
            } catch (IOException e) {
                System.err.println("Error loading configuration file!");
                e.printStackTrace();
            }
        } else {
            properties.setProperty("address", "127.0.0.1");
            properties.setProperty("port", "25566");
            properties.setProperty("web_id", "empty");
            properties.setProperty("web_name", "empty");
            properties.setProperty("id_color", "15"); //Default to white color
            try (FileOutputStream ostream = new FileOutputStream(config)) {
                properties.store(ostream, null);
            } catch (IOException e) {
                System.err.println("Error saving new configuration file!");
                e.printStackTrace();
            }
        }
    }

    public static void handleMessage(AbstractMessage message) {
        //Determines the content of the text component
        StringBuilder string = new StringBuilder();
        if (message instanceof BroadcastMessage) {
            string.append(((BroadcastMessage) message).getPrefix()).append(" ");
            string.append(((BroadcastMessage) message).getMessage()).append(" ");
            System.out.println(string.toString());
        } else if (message instanceof ChatMessage) {
            String component = ((ChatMessage) message).getComponentJson();
            if (component.equals("discord")) {
                string.append("[Discord] ");
            }
            string.append(((ChatMessage) message).getUsername()).append(" ");
            string.append(((ChatMessage) message).getMessage()).append(" ");
        } else if (message instanceof ServerStatusMessage) {
            ServerStatusMessage serverStatus = ((ServerStatusMessage) message);
            switch (serverStatus.getState()) {
                case 1:
                    string.append(" Server Online");
                    break;
                case 2:
                    string.append(" Server Offline");
                    break;
                case 3:
                    string.append(" Server Crashed");
                    break;
                default:
                    System.err.println("Unrecognized server state " + serverStatus.getState() + " received from " + serverStatus.getServerID());
            }
        } else if (message instanceof PlayerStatusMessage) {
            PlayerStatusMessage playerStatus = ((PlayerStatusMessage) message);
            if (!(playerStatus.getSilent())) {
                if (playerStatus.getJoined()) {
                    string.append(" ").append(playerStatus.getUserName()).append(" has joined the game");
                } else {
                    string.append(" ").append(playerStatus.getUserName()).append(" has left the game");
                }
            }
        }
        String output = string.toString();
        System.out.println(output);
        messages.add(output);
    }

}
