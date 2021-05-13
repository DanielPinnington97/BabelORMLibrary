package com.BabelORM.DBConfiguration.ConnectionSettings;

import java.sql.Connection;
import java.sql.SQLException;

public class MariaDB extends BabelJDBC {

    private static final MariaDB MARIA_DB_INST = new MariaDB();

    private MariaDB() {}

    public  static MariaDB getInstance() {
        return MARIA_DB_INST;
    }

    @Override
    public Connection getBabelConnectionPool() throws SQLException {
        return super.getBabelConnectionPool();
    }

    @Override
    public Connection getBabelRootConnectionPool() throws SQLException {
        return  super.getBabelRootConnectionPool();
    }

    @Override
    public void endConnectionPoolSession(Connection con) throws SQLException {
        super.endConnectionPoolSession(con);
    }
}
