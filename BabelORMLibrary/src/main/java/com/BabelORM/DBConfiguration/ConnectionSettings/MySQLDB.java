package com.BabelORM.DBConfiguration.ConnectionSettings;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLDB extends BabelJDBC {

    private static final MySQLDB MY_SQLDB_INST = new MySQLDB();

    private MySQLDB() {

    }

    public static MySQLDB getInstance() {
        return MY_SQLDB_INST;
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
}
