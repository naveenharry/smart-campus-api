package com.westminster.smartcampus;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public final class Main {
    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = startServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        System.out.println("Smart Campus API running at " + BASE_URI);
        System.out.println("Press Ctrl+C to stop the server.");
        waitUntilStopped();
    }

    public static HttpServer startServer() {
        ResourceConfig config = ResourceConfig.forApplication(new SmartCampusApplication());
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    private static void waitUntilStopped() {
        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
