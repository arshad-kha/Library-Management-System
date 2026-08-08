package com.technglobal.library.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Centralized JDBC connection factory.
 *
 * Reads credentials from db.properties (src/main/resources) instead of
 * hardcoding them in Java source — standard practice so the same code
 * works across dev/test/prod just by swapping the properties file.
 *
 * Every DAO calls DBConnection.getConnection() and opens it inside a
 * try-with-resources block, so connections are never leaked.
 */
public class DBConnection {

    private static final String PROPERTIES_FILE = "/db.properties";
    private static String url;
    private static String user;
    private static String password;

    static {
        loadProperties();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found on classpath.", e);
        }
    }

    private DBConnection() {
        // utility class — no instances
    }

    private static void loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new RuntimeException(
                    "db.properties not found on classpath. Expected at src/main/resources/db.properties");
            }
            props.load(in);
            url = props.getProperty("db.url");
            user = props.getProperty("db.user");
            password = props.getProperty("db.password");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
