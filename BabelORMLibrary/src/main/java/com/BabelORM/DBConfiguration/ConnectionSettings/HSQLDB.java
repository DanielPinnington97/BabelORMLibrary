package com.BabelORM.DBConfiguration.ConnectionSettings;

import com.BabelORM.DBConfiguration.DBConfig.BabelConSettings;
import com.BabelORM.Settings.BabelSettings;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class HSQLDB  {

    private static final HSQLDB HSQLDB_INSTANCE = new HSQLDB();

    private Server hsqlSERVER;

    public static HSQLDB getInstance() {
        return HSQLDB_INSTANCE;
    }

    public Connection getPoolSession() throws SQLException {

        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        }
        catch (ClassNotFoundException e) {
            System.err.println("HSQL driver loading failed");
            e.printStackTrace();
        }

        HsqlProperties hsqlProperties = new HsqlProperties();
        if(BabelSettings.HSQLDB) {
            hsqlProperties.setProperty("server.database.0", "file:" + BabelSettings.HSQLDB_LOCATION + ";");
        }
        else {
            hsqlProperties.setProperty("server.database.0", "mem:" + BabelSettings.DBNAME + ";");
        }
        hsqlProperties.setProperty("server.dname.0", BabelSettings.DBNAME);
        hsqlProperties.setProperty("server.port", BabelSettings.DBPORT);
        hsqlSERVER = new Server();

        try {
            hsqlSERVER.setProperties(hsqlProperties);
            hsqlSERVER.setLogWriter(null);
            hsqlSERVER.setErrWriter(null);
            hsqlSERVER.start();
        } catch (IOException | ServerAcl.AclFormatException e) {
            e.printStackTrace();
        }

        if(hsqlSERVER.getState() > 0) {

            try {
                return DriverManager.getConnection(BabelConSettings.returnDBCon(),
                        BabelSettings.USERNAME, BabelSettings.PASSWORD);
            }
            catch (SQLException e) {
                System.out.println("Error: " + e.getErrorCode());

                if(e.getErrorCode() == 1049) {
                    System.out.println("DB does not exit");
                } else {
                    System.out.println(e.getStackTrace());
                }
            }
        }
        else {
            System.out.println("HSQL not running");
        }
        return null;
    }

}
