package com.BabelORM.Settings;

import com.BabelORM.DBConfiguration.DBConfig.BabelConSettings;
import com.BabelORM.DBConfiguration.Exceptions.BabelDBConfig;

import java.sql.SQLException;
import java.util.List;

public class BabelTesting {

    static BabelSettings s = BabelSettings.getINST();

    public static void startBabel(List<Class> success) {

        int t = 0;

        if(s.DBHOST == null) {
            t++;
            System.out.println("No host has been set");
        }
        if(s.DBNAME == null) {
            t++;
            System.out.println("No DB name has been set");
        }
        if(s.USERNAME == null) {
            t++;
            System.out.println("No user has been set");
        }
        if(s.PASSWORD == null) {
            t++;
            System.out.println("The password has been set incorrectly or does not match");
        }
        if(s.PERST == null) {
            t++;
            System.out.println("The database has not been set to persist correctly");
        }
        if(s.ENCODING == null) {
            t++;
            s.ENCODING = BabelConSettings.DB_ENCOD.UTF8;
            System.out.println("Check database encoding" + s.ENCODING.toString());
        }
        if(s.PROT == null) {
            t++;
            if(s.PERST == BabelConSettings.DB_TYPE.MYSQL) {
                s.PROT = BabelConSettings.DB_PROT.JDBC_MYSQL;
            }
            System.out.println("No protocol has been set check the installed jdbc driver" + s.PROT.toString());
        }

        BabelSettings.getINST().mCLass.addAll(success);

        if(t == 0) {
            System.out.println("--------------------------------------------------------------");
            System.out.println("-------------BabelORM is Trying to initialise-----------------");
            System.out.println("--------------------------------------------------------------");
            System.out.println("Type of database used: " + s.PERST);
            System.out.println("Type of protocol used: " + s.PROT);
            System.out.println("Type of endoding used: " + s.ENCODING);
            System.out.println("Host settings: " + s.DBHOST);
            System.out.println("Database names: " + s.DBHOST);
            System.out.println("--------------------------------------------------------------");
            try{
                if(s.root) {
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
