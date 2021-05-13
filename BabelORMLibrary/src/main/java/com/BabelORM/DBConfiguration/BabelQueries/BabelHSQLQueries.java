package com.BabelORM.DBConfiguration.BabelQueries;

import com.BabelORM.BabelUser.BabelStatementsSQL;
import com.BabelORM.BabelUser.BabelUserDetails;
import com.BabelORM.DBConfiguration.Exceptions.BabelPersistenceConfig;
import com.BabelORM.Settings.BabelSystems;

import java.sql.SQLException;
import java.util.List;

public class BabelHSQLQueries implements BabelPersistenceConfig {

    private static final BabelHSQLQueries BABEL_HSQL_QUERIES_INST = new BabelHSQLQueries();

    private BabelHSQLQueries() {}

    public static BabelHSQLQueries getINST() {return BABEL_HSQL_QUERIES_INST;}

    @Override
    public void checkDB() throws SQLException {

    }

    @Override
    public void checkT() throws SQLException {

    }

    @Override
    public void checkF() throws SQLException {

    }

    @Override
    public <T extends BabelUserDetails<BabelSystems>> List<T> selectQuery(Class c, BabelStatementsSQL sql) throws SQLException {
        return null;
    }

    @Override
    public <T> Long insert(Class<T> userState, T s) {
        return null;
    }

    @Override
    public <T> void update(Class<T> c, T s) {

    }
}
