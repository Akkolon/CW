package com.bazylev.server;

import com.bazylev.server.config.AppConfig;
import com.bazylev.server.bootstrap.AdminBootstrap;
import com.bazylev.server.db.ConnectionPool;
import com.bazylev.server.network.Server;

public class Main {

    public static void main(String[] args) {
        AppConfig config = AppConfig.getInstance();
        int port = config.getInt("server.port");
        int threadPoolSize = config.getInt("server.thread.pool.size");

        System.out.println("=== Сервер школы иностранных языков ===");
        System.out.println("Порт: " + port);
        System.out.println("Размер пула потоков: " + threadPoolSize);

        ConnectionPool.getInstance();
        AdminBootstrap.ensureDefaultAdmin();

        Server server = new Server(port, threadPoolSize);
        server.start();
    }
}
