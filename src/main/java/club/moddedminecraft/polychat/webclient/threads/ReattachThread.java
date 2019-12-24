package club.moddedminecraft.polychat.webclient.threads;

import club.moddedminecraft.polychat.networking.io.*;
import club.moddedminecraft.polychat.webclient.Main;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class ReattachThread extends HeartbeatThread {

    private boolean isConnected = true;

    public ReattachThread(int interval) {
        super(interval);
    }

    @Override
    protected void run() throws InterruptedException {
        try {

            if (Main.messageBus == null || (Main.messageBus.isSocketClosed())) {
                //Tells players ingame that the connection failed
                if (isConnected) {
                    isConnected = false;
                    System.out.println("Lost connection to main server, attempting reconnect...");
                }

                //Stops threads if they are still running
                if (Main.messageBus != null) Main.messageBus.stop();

                //Attempts to start the connection
                Main.messageBus = new MessageBus(new Socket(Main.properties.getProperty("address"), Integer.parseInt(Main.properties.getProperty("port"))), Main::handleMessage);
                Main.messageBus.start();

                //If the socket was reopened, wait 3 seconds to make sure sending online message works
                if (!Main.messageBus.isSocketClosed()) {
                    Thread.sleep(2000);
                    System.out.println("Connection re-established!");
                    sendServerOnline();
                    Thread.sleep(1000);
                    isConnected = true;
                }

            }
        } catch (UnknownHostException e) {
            System.out.println("Unknown host exception on reattach");
        } catch (IOException e) {
            System.out.println("IOException on reattach");
        }
    }

    public void sendServerOnline() {
        //Reports the server as starting
        ClientInfoMessage infoMessage = new ClientInfoMessage(Main.properties.getProperty("web_id", "DEFAULT_ID"),
                Main.properties.getProperty("web_name", "DEFAULT_NAME"));
        Main.sendMessage(infoMessage);
        //Reports the server as online and ready to receive players
        ClientStatusMessage statusMessage = new ClientStatusMessage(Main.properties.getProperty("web_id"), (short) 1);
        Main.sendMessage(statusMessage);
    }

    public boolean isConnected() {
        return isConnected;
    }

}
