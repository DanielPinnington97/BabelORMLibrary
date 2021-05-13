package com.BabelORM.DBConfiguration.ConnectionSettings;

import com.BabelORM.DBConfiguration.DBConfig.BabelConSettings;
import com.BabelORM.Settings.BabelSettings;
import com.mysql.cj.jdbc.Driver;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class BabelJDBC {

    protected static HikariDataSource HDS = null;

    public Connection getBabelConnectionPool() throws SQLException {
        return (BabelSettings.HIKARI_CONNECTION_POOL) ? getConnectionToHikari() : getStandardHikari();
    }

    public Connection getBabelRootConnectionPool() throws SQLException {
        return (BabelSettings.HIKARI_CONNECTION_POOL) ? getRootBabelHikariConnection() : getRootConnectionPoolSettings();
    }

    public void endConnectionPoolSession(Connection con) throws SQLException {
        this.endSession(con);
    }

    public void closeDB() {
        if(BabelSettings.HIKARI_CONNECTION_POOL) {
            HDS.close();
        }
    }

    protected Connection getConnectionToHikari() throws SQLException {
        if(HDS == null) runConnectionPool();
        return HDS.getConnection();
    }

    protected  Connection getRootBabelHikariConnection() throws SQLException {
        return getRootConnectionPoolSettings();
    }

    protected Connection getStandardHikari() throws SQLException {
        System.out.println("Babel is connecting to server...");

        return DriverManager.getConnection(BabelConSettings.returnDBCon(),
                BabelSettings.USERNAME, BabelSettings.PASSWORD);
    }

    protected Connection getRootConnectionPoolSettings() throws SQLException {
        if(BabelSettings.getINST().PERST == BabelConSettings.DB_TYPE.MARIADB) {
            System.out.println("Babel is connection to MariaDB session using root credentials");
        }
        else if(BabelSettings.getINST().PERST == BabelConSettings.DB_TYPE.MYSQL) {
            System.out.println("Babel is connection to MySQL session using root credentials");
        }

        return DriverManager.getConnection(BabelConSettings.returnRootDBCon(),
                BabelSettings.rootUSER, BabelSettings.rootPASSWORD);
    }

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

    private void endSession(Connection con) throws SQLException {
        if(con != null && !con.isClosed()) {
            con.close();
        }
    }


}
