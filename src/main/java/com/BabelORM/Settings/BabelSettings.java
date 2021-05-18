package com.BabelORM.Settings;

import com.BabelORM.DBConfiguration.DBConfig.BabelConSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

public class BabelSettings {

    private static final BabelSettings BS_INST = new BabelSettings();

    public List<Class> mCLass = new ArrayList<>();

    public static BabelConSettings.DB_TYPE PERST = BabelConSettings.DB_TYPE.HSQL;
    public static BabelConSettings.DB_PROT PROT = BabelConSettings.DB_PROT.JDBC_HSQL;
    public static String DBHOST = "localhost";
    public static String DBNAME = "BabelORM";
    public static int DBPORT = 3306;
    public static boolean root = true;
    public static BabelConSettings.DB_ENCOD ENCODING = BabelConSettings.DB_ENCOD.UTF8;

    public static Boolean SECURE = false;

    //USER INFORMATION --> CHANGE PASSWORD AS APPROPRIATE
    public static String USERNAME = "danielPinnington";
    public static String PASSWORD = "TEST123";

    //ROOT USER INFORMATION
    public static  String rootUSER = "root";
    public static  String rootPASSWORD = "";

    public static String HSQLDB_LOCATION = "hsql/";
    public static boolean HSQLDB = true;

    //CONNECTION POOL SETTINGS FOR HIKARI CONNECTION POOL

    public static Boolean HIKARI_CONNECTION_POOL = false;
    public static Boolean HIKARI_AUTO = true;

    public static Integer TIMEOUT_CONNECTION = 50000;
    public static Integer IDLE_TIMEOUT = 20000;
    public static Integer POOL_MAX_LIFESPAN = 250000;
    public static Integer POOL_MAX_SIZE = 5;
    public static Integer IDLE_MIN = POOL_MAX_SIZE;

    public static String SQL_DRIVER = "";
    public static String HIKARI_CLASSNAME_DS = "";

    public static ThreadFactory CONNECTION_POOL_THREADER = null;

    private BabelSettings() {
    }

    public static BabelSettings getINST() {
        return BS_INST;
    }
}
