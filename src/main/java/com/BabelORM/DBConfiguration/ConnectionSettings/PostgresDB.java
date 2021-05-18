package com.BabelORM.DBConfiguration.ConnectionSettings;

import com.BabelORM.DBConfiguration.DBConfig.BabelConSettings;
import com.BabelORM.Settings.BabelSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;

public class PostgresDB extends BabelJDBC{

    private static final PostgresDB POSTGRES_DB_INST = new PostgresDB();

    private PostgresDB() {

    }

    public static PostgresDB getInstance() {
        return POSTGRES_DB_INST;
    }

    @Override
    public Connection getBabelConnectionPool() throws SQLException {
        return super.getBabelConnectionPool();
    }

    @Override
    public Connection getBabelRootConnectionPool() throws SQLException {
        return super.getBabelRootConnectionPool();
    }

    @Override
    public void endConnectionPoolSession(Connection con) throws SQLException {
        super.endConnectionPoolSession(con);
    }

    @Override
    protected Connection getStandardHikari() throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Babel was unable to find the DB driver -- ensure the driver is installed and running");
            System.out.println(e.getStackTrace());
        }
        return DriverManager.getConnection(BabelConSettings.returnRootDBCon(),
                BabelSettings.rootUSER.toLowerCase(), BabelSettings.rootPASSWORD);
    }

    @Override
    public void closeDB() {
        super.closeDB();
    }

    @Override
    protected void runConnectionPool() {
        HikariConfig config = new HikariConfig();

        config.setDataSourceClassName(BabelSettings.HIKARI_CLASSNAME_DS);
        config.setAutoCommit(BabelSettings.HIKARI_AUTO);
        config.setConnectionTimeout(BabelSettings.TIMEOUT_CONNECTION);
        config.setIdleTimeout(BabelSettings.IDLE_TIMEOUT);
        config.setMaximumPoolSize(BabelSettings.POOL_MAX_SIZE);
        config.setMaxLifetime(BabelSettings.POOL_MAX_LIFESPAN);
        config.setMinimumIdle(BabelSettings.IDLE_MIN);
        config.setThreadFactory(BabelSettings.CONNECTION_POOL_THREADER);
        config.setConnectionInitSql(BabelSettings.SQL_DRIVER);

        config.setJdbcUrl(BabelConSettings.returnDBCon());
        config.setUsername(BabelSettings.USERNAME);
        config.setPassword(BabelSettings.PASSWORD);

        HDS = new HikariDataSource();
    }
}
