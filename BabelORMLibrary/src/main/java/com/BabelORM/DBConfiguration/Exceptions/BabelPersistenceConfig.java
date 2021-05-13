package com.BabelORM.DBConfiguration.Exceptions;

import com.BabelORM.BabelUser.BabelStatementsSQL;
import com.BabelORM.BabelUser.BabelUserDetails;
import com.BabelORM.Settings.BabelSystems;

import java.sql.SQLException;
import java.util.List;

public interface BabelPersistenceConfig {

    void checkDB() throws SQLException;
    void checkT() throws SQLException;
    void checkF() throws SQLException;

    <T extends BabelUserDetails<BabelSystems>> List<T> selectQuery(Class c, BabelStatementsSQL sql) throws SQLException;
    <T> Long insert(Class<T> userState, T s);
    <T> void update(Class<T> c, T s);
}
