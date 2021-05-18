package com.BabelORM.Settings;

import com.BabelORM.DBConfiguration.DBConfig.BabelConSettings;
import com.BabelORM.DBConfiguration.Exceptions.BabelDBConfig;

import java.sql.SQLException;
import java.util.List;

public class BabelTesting {

    static BabelSettings s = BabelSettings.getINST();

    public static void startBabel(List<Class> success) {

        int t = 0;

        if(BabelSettings.DBHOST == null) {
            t++;
            System.out.println("No host has been set");
        }
        if(BabelSettings.DBNAME == null) {
            t++;
            System.out.println("No DB name has been set");
        }
        if(BabelSettings.USERNAME == null) {
            t++;
            System.out.println("No user has been set");
        }
        if(BabelSettings.PASSWORD == null) {
            t++;
            System.out.println("The password has been set incorrectly or does not match");
        }
        if(BabelSettings.PERST == null) {
            t++;
            System.out.println("The database has not been set to persist correctly");
        }
        if(BabelSettings.ENCODING == null) {
            t++;
            BabelSettings.ENCODING = BabelConSettings.DB_ENCOD.UTF8;
            System.out.println("Check database encoding" + BabelSettings.ENCODING);
        }
        if(BabelSettings.PROT == null) {
            t++;
            if(BabelSettings.PERST == BabelConSettings.DB_TYPE.MYSQL) {
                BabelSettings.PROT = BabelConSettings.DB_PROT.JDBC_MYSQL;
            }
            System.out.println("No protocol has been set check the installed jdbc driver" + BabelSettings.PROT.toString());
        }

        BabelSettings.getINST().mCLass.addAll(success);

        if(t == 0) {
            System.out.println("--------------------------------------------------------------");
            System.out.println("-------------BabelORM is Trying to initialise-----------------");
            System.out.println("--------------------------------------------------------------");
            System.out.println("Type of database used: " + BabelSettings.PERST);
            System.out.println("Type of protocol used: " + BabelSettings.PROT);
            System.out.println("Type of endoding used: " + BabelSettings.ENCODING);
            System.out.println("Host settings: " + BabelSettings.DBHOST);
            System.out.println("Database names: " + BabelSettings.DBHOST);
            System.out.println("--------------------------------------------------------------");
            try{
                if(BabelSettings.root) {
                    BabelDBConfig.getINST().checkDB();
                }
                BabelDBConfig.getINST().checkT();
                BabelDBConfig.getINST().checkF();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Babel could not be started: failed to construct DB");
        }

    }
}
