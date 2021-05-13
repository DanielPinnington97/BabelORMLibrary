package com.BabelORM.DBConfiguration.DBConfig;

import com.BabelORM.Settings.BabelSettings;

public class BabelConSettings {

    public enum DB_TYPE {
        MARIADB, MYSQL, POSTGRES, HSQL
    }

    public enum DB_PROT {
        JDBC_MARIADB, JDBC_MYSQL, JDBC_POSTGRES, JDBC_HSQL
    }

    public enum DB_ENCOD {
        UTF8
    }

    public static String returnDBCon() {
        String protString = "";
        String encodString = "";
        String con = "";

        //MARIADB SETTINGS
        if (BabelSettings.PROT == BabelConSettings.DB_PROT.JDBC_MARIADB) {
            protString = "jdbc:mariadb://";
            con = protString + BabelSettings.DBHOST + "/" + BabelSettings.DBNAME;
        }

        //MYSQL SETTINGS
        if (BabelSettings.PROT == BabelConSettings.DB_PROT.JDBC_MYSQL) {
            protString = "jdbc:mysql://";

            if (BabelSettings.ENCODING == BabelConSettings.DB_ENCOD.UTF8) {
                if (BabelSettings.SECURE == true) {
                    encodString = "?characterEncoding=UTF-8&autoReconnect=true&useSSL=true";
                } else {
                    encodString = "?characterEncoding=UTF-8&autoReconnect=true&useSSL=false";
                }
            } else {
                if (BabelSettings.SECURE == true) {
                    encodString = "?autoReconnect=true&useSSL=true";
                } else {
                    encodString = "?autoReconnect=true&useSSL=false";
                }
            }

            con = protString + BabelSettings.DBHOST + "/" + BabelSettings.DBNAME + encodString;
        }

        //POSTGRES SETTINGS
        if (BabelSettings.PROT == DB_PROT.JDBC_POSTGRES) {
            protString = "jdbc:postgresql://";

            if (BabelSettings.ENCODING == BabelConSettings.DB_ENCOD.UTF8) {
                encodString = "?characterEncoding=UTF-8";
            }

            con = protString + BabelSettings.DBHOST + "/" + BabelSettings.DBNAME.toLowerCase() + encodString;
        }

        //HSQL SETTINGS
        if (BabelSettings.PROT == DB_PROT.JDBC_HSQL) {
            if (BabelSettings.HSQLDB) {
                protString = "jdbc:hsqldb:file:" + BabelSettings.HSQLDB_LOCATION + BabelSettings.DBNAME
                        + ";sql.syntax_mys=true";
            } else {
                protString = "jdbc:hsqldb:mem:" + BabelSettings.DBNAME + ";sql.syntax_mys=true";
            }

            con = protString;
        }

        return con;

    }

    public static String returnRootDBCon() {
        String protString = "";
        String encodString = "";
        String query = "?";
        String security = "";
        String con = "";

        if (BabelSettings.PROT == DB_PROT.JDBC_MARIADB) {

            protString = "jdbc:mariadb://";
            con = protString + BabelSettings.DBHOST;
        }

        if (BabelSettings.PROT == DB_PROT.JDBC_MYSQL) {

            protString = "jdbc:mysql://";

            if (BabelSettings.ENCODING == DB_ENCOD.UTF8) {
                if (BabelSettings.SECURE = true) {
                    encodString = "/?characterEncoding=UTF-8&autoReconnect=true&useSSL=true";
                } else {
                    encodString = "/?characterEncoding=UTF-8&autoReconnect=true&useSSL=false";
                }
            } else {
                if (BabelSettings.SECURE == true) {
                    encodString = "/?autoReconnect=true&useSSL=true";
                } else {
                    encodString = "/?autoReconnect=true&useSSL=false";
                }
            }

            con = protString + BabelSettings.DBHOST + encodString;
        }

        if (BabelSettings.PROT == DB_PROT.JDBC_POSTGRES) {
            protString = "jdbc:postgresql://";

            if(BabelSettings.ENCODING == DB_ENCOD.UTF8) {
                encodString = "/?characterEncoding=UTF-8";
            }

            con = protString + BabelSettings.DBHOST + encodString;
        }

        if (BabelSettings.PROT == DB_PROT.JDBC_HSQL) {
            if(BabelSettings.HSQLDB) {
                protString = "jdbc:hsqldb:file:" + BabelSettings.HSQLDB_LOCATION + BabelSettings.DBNAME
                        + ";sql.syntax_mys=true";
            }
            else {
                protString = "jdbc:hsqldb:mem:" + BabelSettings.DBNAME + ";sql.syntax_mys=true";
            }

            con = protString;
        }

        return con;
    }
}
