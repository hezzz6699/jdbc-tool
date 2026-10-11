package com.example.jdbc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 数据库连接工具类：负责读取 {@code db.properties}、注册驱动、获取连接，
 * 并提供一键初始化数据库（执行 {@code schema.sql}）的能力。
 *
 * <p>默认使用 H2 嵌入式数据库（MySQL 兼容模式），无需安装数据库服务即可运行；
 * 如需切换为 MySQL，只需修改 {@code db.properties} 中对应的配置项。
 */
public final class DBUtil {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("类路径下找不到 db.properties");
            }
            PROPS.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            String driver = PROPS.getProperty("db.driver", "").trim();
            if (!driver.isEmpty()) {
                Class.forName(driver);
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBUtil() {
    }

    /**
     * 获取数据库连接。
     *
     * @return 数据库连接对象
     */
    public static Connection getConnection() throws SQLException {
        String url = PROPS.getProperty("db.url");
        String username = PROPS.getProperty("db.username", "");
        String password = PROPS.getProperty("db.password", "");
        return DriverManager.getConnection(url, username, password);
    }

    /** 驱动类名（用于在演示中打印） */
    public static String getDriver() {
        return PROPS.getProperty("db.driver", "");
    }

    /** 连接地址（用于在演示中打印） */
    public static String getUrl() {
        return PROPS.getProperty("db.url", "");
    }

    /** 安全关闭连接 */
    public static void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 初始化数据库：执行类路径下的 {@code schema.sql} 脚本（建表语句使用
     * {@code CREATE TABLE IF NOT EXISTS}，可重复执行）。
     */
    public static void initDatabase() {
        String script = readResource("schema.sql");
        List<String> statements = splitSql(script);
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("初始化数据库失败：" + e.getMessage(), e);
        }
    }

    /** 读取类路径下的文本资源 */
    private static String readResource(String name) {
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("类路径下找不到资源：" + name);
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException("读取资源失败：" + name, e);
        }
    }

    /** 把 SQL 脚本按分号拆分为单条语句，忽略注释行 */
    private static List<String> splitSql(String script) {
        List<String> result = new ArrayList<>();
        for (String raw : script.split(";")) {
            StringBuilder sb = new StringBuilder();
            for (String line : raw.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                sb.append(line).append('\n');
            }
            String sql = sb.toString().trim();
            if (!sql.isEmpty()) {
                result.add(sql);
            }
        }
        return result;
    }
}
