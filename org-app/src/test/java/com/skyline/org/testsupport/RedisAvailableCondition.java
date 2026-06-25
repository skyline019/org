package com.skyline.org.testsupport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class RedisAvailableCondition {

    private RedisAvailableCondition() {
    }

    public static boolean isAvailable() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1500);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
