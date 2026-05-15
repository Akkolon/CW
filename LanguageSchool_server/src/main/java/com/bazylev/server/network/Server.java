package com.bazylev.server.network;

import com.bazylev.server.db.ConnectionPool;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {

    private final int port;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public Server(int port, int threadPoolSize) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            registerShutdownHook();
            System.out.println("Сервер запущен на порту " + port);
            printPoolStats();

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientAddress = clientSocket.getInetAddress().getHostAddress()
                            + ":" + clientSocket.getPort();
                    System.out.println("[+] Подключился клиент: " + clientAddress);
                    threadPool.execute(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Ошибка принятия соединения: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось запустить сервер на порту " + port, e);
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии серверного сокета: " + e.getMessage());
        }

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        ConnectionPool.getInstance().closeAll();
        System.out.println("Сервер остановлен.");
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nПолучен сигнал завершения. Останавливаем сервер...");
            stop();
        }));
    }

    private void printPoolStats() {
        if (threadPool instanceof ThreadPoolExecutor tpe) {
            System.out.println("Пул потоков: " + tpe.getCorePoolSize() + " потоков");
        }
    }
}
