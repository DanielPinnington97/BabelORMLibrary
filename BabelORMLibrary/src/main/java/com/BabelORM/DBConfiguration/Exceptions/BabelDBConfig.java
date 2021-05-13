package com.BabelORM.DBConfiguration.Exceptions;

import com.BabelORM.BabelUser.BabelStatementsSQL;
import com.BabelORM.BabelUser.BabelUserDetails;
import com.BabelORM.DBConfiguration.DBConfig.BabelDBManager;
import com.BabelORM.Settings.BabelSystems;

import java.sql.SQLException;
import java.util.List;

public class BabelDBConfig extends BabelDBManager implements BabelPersistenceConfig {

    private static final BabelDBConfig CONFIG_INST = new BabelDBConfig();

    private BabelDBConfig() {}

    public static BabelDBConfig getINST() {

        return CONFIG_INST;
    }

    @Overide
    public void checkDB() throws SQLException {
        DBCONFIG.checkDB();
    }

    @Overide
    public void checkT() throws SQLException {
        DBCONFIG.checkT();
    }

    @Overide
    public void checkF() throws SQLException {
        DBCONFIG.checkF();
    }


    @Override
    public <T extends BabelUserDetails<BabelSystems>> List<T> selectQuery(Class c, BabelStatementsSQL sql) {
        return null;
    }

    @Overide
    public <T> Long insert(Class<T> userState, T state) {
        return DBCONFIG.insert(userState, state);
    }

    @Override
    public <T> void update(Class<T> c, T s) {

    }

    @Overide
    public <T extends BabelUserDetails<BabelSystems>> List<T> selectQ(Class c, BabelStatementsSQL sqlStatement) throws SQLException {
        return DBCONFIG.selectQuery(c, sqlStatement);
    }
}