package club.moddedminecraft.polychat.webclient.threads;


import club.moddedminecraft.polychat.networking.io.ChatMessage;
import club.moddedminecraft.polychat.webclient.Main;

import java.util.Scanner;

public class InputThread extends HeartbeatThread {

    Scanner scanner;

    public InputThread(int interval) {
        super(interval);
        scanner = new Scanner(System.in);
    }

    @Override
    protected void run() {
        String message = scanner.nextLine();
        if (message.equals("stop")) {
            Main.shutdownClean = true;
            System.exit(0);
        }
        if (Main.reattachThread.isConnected()) {
            ChatMessage chatMessage = new ChatMessage(Main.properties.getProperty("web_name", "DEFAULT_NAME") + ":", message, "web");
            Main.sendMessage(chatMessage);
        }
    }
}
