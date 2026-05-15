package com.bazylev.server.db;

import com.bazylev.server.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class ConnectionPool {

    private static final ConnectionPool INSTANCE = new ConnectionPool();

    private final LinkedBlockingQueue<Connection> pool;
    private final String url;
    private final String username;
    private final String password;
    private final int poolSize;

    private ConnectionPool() {
        AppConfig config = AppConfig.getInstance();
        this.url = config.get("db.url");
        this.username = config.get("db.username");
        this.password = config.get("db.password");
        this.poolSize = config.getInt("db.pool.size");
        this.pool = new LinkedBlockingQueue<>(poolSize);
        initPool();
    }

    public static ConnectionPool getInstance() {
        return INSTANCE;
    }

    private void initPool() {
        for (int i = 0; i < poolSize; i++) {
            pool.offer(createConnection());
        }
        System.out.println("Пул соединений инициализирован: " + poolSize + " соединений");
    }

    private Connection createConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось создать соединение с БД", e);
        }
    }

    public Connection getConnection() {
        try {
            Connection connection = pool.poll(5, TimeUnit.SECONDS);
            if (connection == null) {
                throw new RuntimeException("Таймаут ожидания соединения из пула");
            }
            if (!connection.isValid(2)) {
                connection = createConnection();
            }
            return connection;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Поток прерван при ожидании соединения", e);
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки соединения", e);
        }
    }

    public void releaseConnection(Connection connection) {
        if (connection != null) {
            pool.offer(connection);
        }
    }

    public void closeAll() {
        for (Connection connection : pool) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
            }
        }
        pool.clear();
    }
}
