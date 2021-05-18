package com.BabelORM.DBConfiguration.DBConfig;

import com.BabelORM.DBConfiguration.BabelQueries.BabelHSQLQueries;
import com.BabelORM.DBConfiguration.BabelQueries.BabelMariaDBQueries;
import com.BabelORM.DBConfiguration.BabelQueries.BabelMySQLQueries;
import com.BabelORM.DBConfiguration.BabelQueries.BabelPostgresQueries;
import com.BabelORM.DBConfiguration.Exceptions.BabelPersistenceConfig;
import com.BabelORM.Settings.BabelSettings;

public abstract class BabelDBManager implements BabelPersistenceConfig {

    public static BabelPersistenceConfig DBCONFIG = null;

    public BabelDBManager() {

        if(BabelSettings.PERST == BabelConSettings.DB_TYPE.MARIADB) {
            DBCONFIG = BabelMariaDBQueries.getINST();
        }

        if(BabelSettings.PERST == BabelConSettings.DB_TYPE.MYSQL) {
            DBCONFIG = BabelMySQLQueries.getINST();
        }

        if(BabelSettings.PERST == BabelConSettings.DB_TYPE.POSTGRES) {
            DBCONFIG = BabelPostgresQueries.getINST();
        }

        if(BabelSettings.PERST == BabelConSettings.DB_TYPE.HSQL) {
            DBCONFIG = BabelHSQLQueries.getINST();
        }
    }
}
