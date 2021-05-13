package com.BabelORM.DBConfiguration.BabelQueries;

import com.BabelORM.Annotation.BabelAnnotation;
import com.BabelORM.BabelUser.BabelSQLResultsFilter;
import com.BabelORM.BabelUser.BabelStatementsSQL;
import com.BabelORM.BabelUser.BabelUserDetails;
import com.BabelORM.DBConfiguration.ConnectionSettings.MySQLDB;
import com.BabelORM.DBConfiguration.Exceptions.BabelPersistenceConfig;
import com.BabelORM.Settings.BabelSettings;
import com.BabelORM.Settings.BabelSystems;
import com.BabelORM.Utilities.BabelReflectionSettings;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

public class BabelMySQLQueries implements BabelPersistenceConfig {

    private static final BabelMySQLQueries MY_SQL_QUERIES_INST = new BabelMySQLQueries();

    private BabelMySQLQueries() {}

        public static BabelMySQLQueries getINST() {
            return MY_SQL_QUERIES_INST;
        }

        public void checkDB() throws SQLException {
            Connection con = null;
            Statement s = null;
            ResultSet rs = null;

            try {
                con = MySQLDB.getInstance().getBabelConnectionPool();
                s = con.createStatement();
                if(s != null) {
                    rs = s.executeQuery("SELECT * FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '" + BabelSettings.DBNAME + "';");
                    if(rs != null && !rs.next()) {
                        //Database does not exist, create
                        String query = "CREATE DATABASE IF NOT EXISTS " + BabelSettings.DBNAME + " CHARACTER SET "
                                + BabelSettings.ENCODING + ";";
                        String privileges = "GRANT ALL ON " + BabelSettings.DBNAME + ".* to '" + BabelSettings.USERNAME
                                + "'@'" + BabelSettings.DBHOST + "' IDENTIFIED BY '" + BabelSettings.PASSWORD + "'";


                        if(s != null) {
                            s.executeUpdate(query);
                            s.executeUpdate(privileges);
                            System.out.println("Database did not exist... creating...");
                            System.out.println("Babel has created the DB...");
                        }
                    }
                }
            }
            catch (SQLException sqlException) {
                System.out.println("Error"+ sqlException.getErrorCode() + sqlException.getSQLState() + sqlException.getMessage());
            }
            finally {
                if (rs != null) {
                    rs.close();
                }
                if(s != null) {
                    s.close();
                }
                if(con != null) {
                    MySQLDB.getInstance().endConnectionPoolSession(con);
                }
            }
        }

        public void checkT() throws SQLException {

            for(Class c: BabelSettings.getINST().mCLass) {

                if(!c.isAnnotationPresent(BabelAnnotation.class)) {
                    Connection con = null;
                    Statement s = null;
                    ResultSet rs = null;
                    try {
                        con = MySQLDB.getInstance().getBabelConnectionPool();
                        s = con.createStatement();
                        if (s != null) {
                            rs = s.executeQuery("SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA = '"
                                    + BabelSettings.DBNAME + "' AND TABLE_NAME = '" + c.getSimpleName().toLowerCase() + "';");
                            if (rs != null && !rs.next()) {

                                if(c.getFields().length > 0) {
                                    //Table does not exist, create
                                    StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                                            .append(BabelSettings.DBNAME).append(".")
                                            .append(c.getSimpleName().toLowerCase()).append(" (");
                                    Arrays.stream(c.getFields()).filter(field -> !field.isAnnotationPresent(BabelAnnotation.class))
                                            .forEach(field -> query.append(" ").append(BabelFieldandData(field)).append(","));

                                    String createQuery = query.deleteCharAt(query.length()-1).append(");").toString();
                                    s.executeUpdate(createQuery);
                                } else {
                                    System.out.println("No table created");
                                }
                            }
                        }
                    }
                    catch (SQLException sqlException) {
                        System.out.println("Babel table error" + sqlException.getErrorCode() + sqlException.getSQLState() + sqlException.getMessage());
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    finally {
                        if (rs != null) {
                            rs.close();
                        }
                        if (s != null) {
                            s.close();
                        }
                        if(con != null) {
                            MySQLDB.getInstance().endConnectionPoolSession(con);
                        }
                    }
                }
            }
        }

        public void checkF() throws SQLException {

            for(Class c: BabelSettings.getINST().mCLass) {

                if(!c.isAnnotationPresent(BabelAnnotation.class)) {

                    List<Field> fields = Arrays.stream(c.getFields()).filter(field -> !field.isAnnotationPresent(BabelAnnotation.class)).collect(Collectors.toList());

                    if(fields.size() > 0) {

                        Connection con = null;
                        Statement s = null;
                        ResultSet rs = null;
                        try {
                            con = MySQLDB.getInstance().getBabelConnectionPool();
                            s = con.createStatement();
                            for(Field field : fields) {

                                String fieldTest = "SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '"
                                        + BabelSettings.DBNAME + "' AND TABLE_NAME = '" + c.getSimpleName().toLowerCase()
                                        + "' AND COLUMN_NAME= '" + field.getName() + "';";
                                if(s != null && (rs = s.executeQuery(fieldTest)) != null && !rs.next()) {

                                    System.out.println("Babel creating field...");
                                    String alterSql = "ALTER TABLE " + BabelSettings.DBNAME + "." + c.getSimpleName().toLowerCase()
                                            + " ADD COLUMN " + BabelFieldandData(field) + ";";
                                    System.out.println("Add field sql " + c.getSimpleName().toLowerCase() + alterSql);
                                    s.executeUpdate(alterSql);
                                }
                            }
                        }
                        catch(SQLException sqlException) {
                            System.out.println("Babel found a field error" + sqlException.getErrorCode() + sqlException.getSQLState() + sqlException.getMessage());
                        }
                        catch(Exception ex) {
                            ex.printStackTrace();
                        }
                        finally {
                            if(rs != null) {
                                rs.close();
                            }
                            if(s != null) {
                                s.close();
                            }
                            if(con != null) {
                                MySQLDB.getInstance().endConnectionPoolSession(con);
                            }
                        }
                    }
                }
            }
        }

        public <T> void update(Class<T> c, T s) {

            if(!c.isAnnotationPresent(BabelAnnotation.class)) {
                Connection con = null;
                PreparedStatement ps = null;
                try {

                    List<Field> fields = Arrays.stream(c.getFields()).filter(field -> !field.isAnnotationPresent(BabelAnnotation.class)
                            && !field.getName().equals("rid")).collect(Collectors.toList());

                    StringBuilder query = new StringBuilder("UPDATE ").append(BabelSettings.DBNAME).append(".")
                            .append(c.getSimpleName().toLowerCase()).append(" SET");
                    fields.forEach(field -> query.append(" ").append(field.getName()).append(" = ?,"));
                    query.deleteCharAt(query.length() - 1).append(" WHERE rid = ").append(c.getField("rid").get(s)).append(";");
                    //Create Connection
                    con = MySQLDB.getInstance().getBabelConnectionPool();
                    ps = con.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);

                    int count = 1;
                    for(Field field : fields) {
                        try {
                            if(field.getType() == String.class) {
                                if(field.get(s) != null) {
                                    ps.setString(count, field.get(s).toString());
                                } else {
                                    ps.setNull(count, Types.VARCHAR);
                                }
                            } else if(field.getType() == int.class || field.getType() == Integer.class) {
                                if (field.get(s) != null) {
                                    ps.setInt(count, (int) field.get(s));
                                } else {
                                    ps.setNull(count, Types.INTEGER);
                                }
                            } else if (field.getType() == Long.class || field.getType() == long.class) {
                                if (field.get(s) != null) {
                                    ps.setLong(count, (long) field.get(s));
                                } else {
                                    ps.setNull(count, Types.BIGINT);
                                }
                            } else if (field.getType() == Double.class || field.getType() == double.class) {
                                if (field.get(s) != null) {
                                    ps.setDouble(count, (double) field.get(s));
                                } else {
                                    ps.setNull(count, Types.DOUBLE);
                                }
                            } else if (field.getType() == Float.class || field.getType() == float.class) {
                                if (field.get(s) != null) {
                                    ps.setFloat(count, (float) field.get(s));
                                } else {
                                    ps.setNull(count, Types.FLOAT);
                                }
                            } else if (field.getType() == BigDecimal.class) {
                                if (field.get(s) != null) {
                                    ps.setBigDecimal(count, (BigDecimal) field.get(s));
                                } else {
                                    ps.setNull(count, Types.NUMERIC);
                                }
                            } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                                if (field.get(s) != null) {
                                    ps.setBoolean(count, (boolean) field.get(s));
                                } else {
                                    ps.setNull(count, Types.BOOLEAN);
                                }
                            } else if (field.getType() == char.class || field.getType() == Character.class) {
                                if (field.get(s) != null) {
                                    ps.setString(count, field.get(s).toString().substring(0, 1));
                                } else {
                                    ps.setNull(count, Types.CHAR);
                                }
                            } else if (field.getType() == Date.class) {
                                if (field.get(s) != null) {
                                    ps.setTimestamp(count, new Timestamp(((Date) field.get(s)).getTime()));
                                } else {
                                    ps.setNull(count, Types.DATE);
                                }
                            } else if (field.getType() == UUID.class) {
                                if(field.get(s) != null) {
                                    ps.setString(count, field.get(s).toString());
                                } else {
                                    ps.setNull(count, Types.VARCHAR);
                                }
                            } else if (field.getType() != null && field.getType().isEnum()) {
                                if(field.get(s) != null) {
                                    ps.setString(count, field.get(s).toString());
                                } else {
                                    ps.setNull(count, Types.VARCHAR);
                                }
                            } else if (Class.class.isAssignableFrom(field.getType())) {
                                if(field.get(s) != null) {
                                    String className = field.get(s).toString();
                                    ps.setString(count, BabelReflectionSettings.className(className));
                                }
                                else {
                                    ps.setNull(count, Types.VARCHAR);
                                }
                            } else if (field.getGenericType() instanceof ParameterizedType && field.get(s) != null) {
                                ParameterizedType pt = (ParameterizedType) field.getGenericType();
                                if(pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0].toString()
                                        .matches(".*?((L|l)ong|Integer|int|(D|d)ouble|(F|f)loat|(B|b)oolean|String).*")) {
                                    ps.setString(count, field.get(s).toString());
                                } else try {
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                                    objectOutputStream.writeObject(field.get(s));
                                    objectOutputStream.close();
                                    ps.setString(count,
                                            Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else if (field.get(s) != null) {
                                try {
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                                    oos.writeObject(field.get(s));
                                    oos.close();
                                    ps.setString(count,
                                            Base64.getEncoder().encodeToString(baos.toByteArray()));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                ps.setNull(count, Types.BLOB);
                            }
                            count++;
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("update complete");
                    ps.execute();
                } catch (SQLException | IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                } finally {
                    if (ps != null) try {
                        ps.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    if(con != null) {
                        try {
                            MySQLDB.getInstance().endConnectionPoolSession(con);
                        }
                        catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        public <S> Long insert(Class<S> c, S state) {
            long ID = -1L;

            if(!c.isAnnotationPresent(BabelAnnotation.class)) {
                Connection con = null;
                PreparedStatement ps = null;
                ResultSet rs = null;
                try {

                    List<Field> fields = Arrays.stream(c.getFields()).filter(field -> !field.isAnnotationPresent(BabelAnnotation.class)
                            && !field.getName().equals("rid")).collect(Collectors.toList());

                    StringBuilder query = new StringBuilder("INSERT INTO ").append(BabelSettings.DBNAME).append(".")
                            .append(c.getSimpleName().toLowerCase()).append(" ("),
                            val = new StringBuilder();
                    fields.forEach(field -> {
                        query.append(" ").append(field.getName()).append(",");
                        val.append(" ?,");
                    });
                    query.deleteCharAt(query.length()-1).append(") VALUES (")
                            .append(val.deleteCharAt(val.length()-1).toString()).append(");");

                    con = MySQLDB.getInstance().getBabelConnectionPool();
                    ps = con.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
                    int count = 1;
                    for (Field field : fields) {
                        try {
                            if (field.getType() == String.class) {
                                if (field.get(state) != null) {
                                    ps.setString(count, field.get(state).toString());
                                } else {
                                    ps.setNull(count, Types.VARCHAR);
                                }
                            } else if (field.getType() == int.class || field.getType() == Integer.class) {
                                if (field.get(state) != null) {
                                    ps.setInt(count, (int) field.get(state));
                                } else {
                                    ps.setNull(count, Types.INTEGER);
                                }
                            } else if (field.getType() == Long.class || field.getType() == long.class) {
                                if (field.get(state) != null) {
                                    ps.setLong(count, (long) field.get(state));
                                } else {
                                    ps.setNull(count, Types.BIGINT);
                                }
                            } else if (field.getType() == Double.class || field.getType() == double.class) {
                                if (field.get(state) != null) {
                                    ps.setDouble(count, (double) field.get(state));
                                } else {
                                    ps.setNull(count, Types.DOUBLE);
                                }
                            } else if (field.getType() == Float.class || field.getType() == float.class) {
                                if (field.get(state) != null) {
                                    ps.setFloat(count, (float) field.get(state));
                                } else {
                                    ps.setNull(count, Types.FLOAT);
                                }
                            } else if (field.getType() == BigDecimal.class) {
                                if (field.get(state) != null) {
                                    ps.setBigDecimal(count, (BigDecimal) field.get(state));
                                } else {
                                    ps.setNull(count, Types.NUMERIC);
                                }
                            } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                                if (field.get(state) != null) {
                                    ps.setBoolean(count, (boolean) field.get(state));
                                } else {
                                    ps.setNull(count, Types.TINYINT);
                                }
                            } else if (field.getType() == char.class || field.getType() == Character.class) {
                                if (field.get(state) != null) {
                                    ps.setString(count, field.get(state).toString().substring(0, 1));
                                } else {
                                    ps.setNull(count, Types.CHAR);
                                }
                            } else if (field.getType() == UUID.class) {
                                if (field.get(state) != null) {
                                    ps.setString(count, field.get(state).toString());
                                } else {
                                    ps.setNull(count, Types.VARCHAR);
                                }
                            } else if (field.getType() == Date.class) {
                                if (field.get(state) != null) {
                                    ps.setTimestamp(count, new Timestamp(((Date) field.get(state)).getTime()));
                                } else {
                                    ps.setNull(count, Types.DATE);
                                }
                            } else if (field.getType() != null && field.getType().isEnum()) {
                                if (field.get(state) != null) {
                                    ps.setString(count, field.get(state).toString());
                                } else {
                                    ps.setNull(count, Types.VARCHAR);
                                }
                            } else if (Class.class.isAssignableFrom(field.getType())) {
                                if (field.get(state) != null) {
                                    ps.setString(count, BabelReflectionSettings.className(field.get(state).toString()));
                                } else {
                                    ps.setNull(count, Types.VARCHAR);
                                }
                            } else if (field.getGenericType() instanceof ParameterizedType && field.get(state) != null) {
                                ParameterizedType pt = (ParameterizedType) field.getGenericType();
                                if(pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0].toString()
                                        .matches(".*?((L|l)ong|Integer|int|(D|d)ouble|(F|f)loat|(B|b)oolean|String).*")) {
                                    ps.setString(count, field.get(state).toString());
                                } else try {
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                                    objectOutputStream.writeObject(field.get(state));
                                    objectOutputStream.close();
                                    ps.setString(count,
                                            Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else if(field.get(state) != null) {
                                try {
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                                    objectOutputStream.writeObject(field.get(state));
                                    objectOutputStream.close();
                                    ps.setString(count,
                                            Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                ps.setNull(count, Types.BLOB);
                            }
                            count++;
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    ps.execute();
                    rs = ps.getGeneratedKeys();
                    if(rs != null && rs.next()) {
                        ID = rs.getLong(1);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    if (rs != null) try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    if (ps != null) try {
                        ps.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    if(con != null) {
                        try {
                            MySQLDB.getInstance().endConnectionPoolSession(con);
                        }
                        catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return ID;
        }

        public <T extends BabelUserDetails<BabelSystems>> List<T> selectQuery(Class c, BabelStatementsSQL sql) throws SQLException {

            List<T> tArrayList = new ArrayList<>();

            if(!c.isAnnotationPresent(BabelAnnotation.class)) {

                if(sql.getOrBy().length() == 0) {
                    sql.orderBY("id");
                }

                String query = sql.getSel() + sql.getGrBy() + " FROM " + BabelSettings.DBNAME + "." + c.getSimpleName()
                        + sql.getOrBy() + sql.getLim() + sql.getLim() + ";";

                Connection con = null;
                PreparedStatement ps = null;
                ResultSet rs = null;

                try {
                    con = MySQLDB.getInstance().getBabelConnectionPool();
                    ps = con.prepareStatement(query);
                    if(ps != null) {
                        rs = ps.executeQuery();
                        while(rs.next()) {

                            Object thisObject = c.newInstance();

                            for (Field field : c.getFields()) {

                                if(!field.isAnnotationPresent(BabelAnnotation.class)) {
                                    try {
                                        if (field.getType() == String.class) {
                                            try {

                                                if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                    field.setAccessible(true);
                                                    field.set(thisObject, rs.getString(field.getName()));
                                                }
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("SELECT: sql state 42S22 on column"
                                                            + " , This is because select did not include column and "
                                                            + "can be ignored: " + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select" +
                                                            ":" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        } else if (field.getType() == int.class || field.getType() == Integer.class) {
                                            try {
                                                if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                    field.setAccessible(true);
                                                    field.set(thisObject, rs.getInt(field.getName()));
                                                }
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select had sql state 42S22 (Invalid column name) on column" +
                                                            "" + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        } else if (field.getType() == long.class || field.getType() == Long.class) {
                                            try {
                                                if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                    field.setAccessible(true);
                                                    field.set(thisObject, rs.getLong(field.getName()));
                                                }
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select  sql state 42S22 (Invalid column name) on column"
                                                            + " because select did not include column and "
                                                            + "can be ignored." + field.getName() +e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select " + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        } else if (field.getType() == double.class || field.getType() == Double.class) {
                                            try {
                                                if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                    field.setAccessible(true);
                                                    field.set(thisObject, rs.getDouble(field.getName()));
                                                }
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select had sql state 42S22 (Invalid column name) on column" + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        } else if (field.getType() == float.class || field.getType() == Float.class) {
                                            try {
                                                if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                    field.setAccessible(true);
                                                    field.set(thisObject, rs.getFloat(field.getName()));
                                                }
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select statement had sql state 42S22 (Invalid column name)"
                                                            + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        } else if (field.getType() == int.class || field.getType() == Integer.class) {
                                            try {
                                                if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                    field.setAccessible(true);
                                                    field.set(thisObject, rs.getInt(field.getName()));
                                                }
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select statement had sql state 42S22 (Invalid column name) on column"
                                                            + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        }
                                        else if (field.getType() == BigDecimal.class) {
                                            try {
                                                if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                    field.setAccessible(true);
                                                    field.set(thisObject, rs.getBigDecimal(field.getName()));
                                                }
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select statement had sql state 42S22 (Invalid column name) on column"
                                                            + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        }
                                        else if (field.getType() == char.class || field.getType() == Character.class) {
                                            try {
                                                field.setAccessible(true);
                                                if (field.getName() != null && rs.getString(field.getName()) != null && rs.getString(field.getName()).length() > 0) {
                                                    field.set(thisObject, rs.getString(field.getName()).charAt(0));
                                                }
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select statement had sql state 42S22 (Invalid column name) on column"
                                                            + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        } else if (field.getType() == Date.class) {
                                            try {
                                                if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                    field.setAccessible(true);
                                                    field.set(thisObject, rs.getTimestamp(field.getName()));
                                                }
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select statement had sql state 42S22 (Invalid column name) on column"
                                                            + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            } catch (NullPointerException npe) {

                                                System.out.println("NullPointer on Date column" + field.getName() + npe.getMessage());
                                            }
                                        } else if (field.getType() == UUID.class) {
                                            try {
                                                if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                    field.setAccessible(true);
                                                    field.set(thisObject, UUID.fromString(rs.getString(field.getName())));
                                                }
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select statement had sql state 42S22 (Invalid column name) on column"
                                                            + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        } else if (field.getType() == boolean.class) {
                                            try {
                                                if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                    field.setAccessible(true);
                                                    field.set(thisObject, rs.getBoolean(field.getName()));
                                                }
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select statement had sql state 42S22 (Invalid column name)"
                                                            + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        } else if (field.getType() == Boolean.class) {
                                            try {
                                                if(field.getName() != null && rs.getString(field.getName()) != null) {
                                                    field.setAccessible(true);
                                                    field.set(thisObject, rs.getBoolean(field.getName()));
                                                }
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select statement had sql state 42S22" + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select " + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        } else if (field.getType() instanceof Class && ((Class<?>) field.getType()).isEnum()
                                                && field.getName() != null && rs.getString(field.getName()) != null) {
                                            try {
                                                field.setAccessible(true);
                                                field.set(thisObject, Enum.valueOf((Class<Enum>) field.getType(),
                                                        rs.getString(field.getName())));
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select had sql state 42S22 (Invalid column name) on column" + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        } else if (Class.class.isAssignableFrom(field.getType())
                                                && field.getName() != null && rs.getString(field.getName()) != null) {
                                            try {
                                                field.setAccessible(true);
                                                field.set(thisObject,
                                                        BabelReflectionSettings.getClassFNAME(rs.getString(field.getName())));
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {
                                                    // Invalid column name, thrown if select statement does not include column
                                                    System.out.println("Select statement had sql state 42S22 (Invalid column name) on column"
                                                            + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        } else if (field.getGenericType() instanceof ParameterizedType
                                                && field.getName() != null && rs.getString(field.getName()) != null) {
                                            ParameterizedType pt = (ParameterizedType) field.getGenericType();

                                            if (pt.getActualTypeArguments().length == 1 &&
                                                    (pt.getActualTypeArguments()[0].toString().contains("Long")
                                                            || pt.getActualTypeArguments()[0].toString().contains("long"))) {

                                                List<Long> list = new ArrayList<>();

                                                String[] strArr = rs.getString(field.getName()).substring(1,
                                                        rs.getString(field.getName()).length() - 1).split(",");

                                                for (String str : strArr) {
                                                    list.add(Long.parseLong(str.replaceAll("\\s", "")));
                                                }

                                                field.set(thisObject, list);
                                            } else if (pt.getActualTypeArguments().length == 1 &&
                                                    (pt.getActualTypeArguments()[0].toString().contains("Integer")
                                                            || pt.getActualTypeArguments()[0].toString().contains("int"))) {

                                                List<Integer> list = new ArrayList<>();

                                                String[] strArr = rs.getString(field.getName()).substring(1,
                                                        rs.getString(field.getName()).length() - 1).split(",");

                                                for (String str : strArr) {
                                                    list.add(Integer.parseInt(str.replaceAll("\\s", "")));
                                                }

                                                field.set(thisObject, list);
                                            } else if (pt.getActualTypeArguments().length == 1 &&
                                                    (pt.getActualTypeArguments()[0].toString().contains("Float")
                                                            || pt.getActualTypeArguments()[0].toString().contains("float"))) {

                                                List<Float> LIST = new ArrayList<>();

                                                String[] stringARRAY = rs.getString(field.getName()).substring(1,
                                                        rs.getString(field.getName()).length() - 1).split(",");

                                                for (String str : stringARRAY) {
                                                    LIST.add(Float.parseFloat(str.replaceAll("\\s", "")));
                                                }

                                                field.set(thisObject, LIST);
                                            } else if (pt.getActualTypeArguments().length == 1 &&
                                                    (pt.getActualTypeArguments()[0].toString().contains("Double")
                                                            || pt.getActualTypeArguments()[0].toString().contains("double"))) {

                                                List<Double> list = new ArrayList<>();

                                                String[] strArr = rs.getString(field.getName()).substring(1,
                                                        rs.getString(field.getName()).length() - 1).split(",");

                                                for (String str : strArr) {
                                                    list.add(Double.parseDouble(str.replaceAll("\\s", "")));
                                                }

                                                field.set(thisObject, list);
                                            }
                                            else if (pt.getActualTypeArguments().length == 1 &&
                                                    (pt.getActualTypeArguments()[0].toString().contains("BigDecimal"))) {

                                                List<Double> list = new ArrayList<>();

                                                String[] strArr = rs.getString(field.getName()).substring(1,
                                                        rs.getString(field.getName()).length() - 1).split(",");

                                                for (String str : strArr) {
                                                    list.add(Double.parseDouble(str.replaceAll("\\s", "")));
                                                }

                                                field.set(thisObject, list);
                                            }
                                            else if (pt.getActualTypeArguments().length == 1 &&
                                                    (pt.getActualTypeArguments()[0].toString().contains("Boolean")
                                                            || pt.getActualTypeArguments()[0].toString().contains("boolean"))) {

                                                List<Boolean> list = new ArrayList<>();

                                                String[] strArr = rs.getString(field.getName()).substring(1,
                                                        rs.getString(field.getName()).length() - 1).split(",");

                                                for (String str : strArr) {
                                                    list.add(Boolean.parseBoolean(str.replaceAll("\\s", "")));
                                                }

                                                field.set(thisObject, list);
                                            } else if (pt.getActualTypeArguments().length == 1 &&
                                                    pt.getActualTypeArguments()[0].toString().contains("String")) {

                                                List<String> list = new ArrayList<>();

                                                String[] strArr = rs.getString(field.getName()).substring(1,
                                                        rs.getString(field.getName()).length() - 1).split(",");

                                                for (String str : strArr) {
                                                    list.add(str.replaceAll("\\s", ""));
                                                }

                                                field.set(thisObject, list);
                                            } else {

                                                try {
                                                    field.setAccessible(true);

                                                    byte[] decode = Base64.getDecoder().decode(rs.getString(field.getName()));

                                                    ObjectInputStream objectInputStream = new ObjectInputStream(
                                                            new ByteArrayInputStream(decode));
                                                    Object object = objectInputStream.readObject();
                                                    objectInputStream.close();

                                                    field.set(thisObject, object);
                                                } catch (SQLException e) {
                                                    if (e.getSQLState().equals("42S22")) {

                                                        System.out.println("Select statement had sql state 42S22 (Invalid column name) on column"
                                                                + field.getName() + e.getMessage());
                                                    } else {
                                                        System.out.println("SQL error in Select"
                                                                + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                    }
                                                }
                                            }
                                        } else if (field.getName() != null && rs.getString(field.getName()) != null) {

                                            try {
                                                field.setAccessible(true);

                                                byte[] decode = Base64.getDecoder().decode(rs.getString(field.getName()));
                                                ObjectInputStream objectInputStream = new ObjectInputStream(
                                                        new ByteArrayInputStream(decode));
                                                Object object = objectInputStream.readObject();
                                                objectInputStream.close();

                                                field.set(thisObject, object);
                                            } catch (SQLException e) {
                                                if (e.getSQLState().equals("42S22")) {

                                                    System.out.println("Select statement had sql state 42S22 (Invalid column name) on column"
                                                            + field.getName() + e.getMessage());
                                                } else {
                                                    System.out.println("SQL error in Select"
                                                            + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                                }
                                            }
                                        }
                                    } catch (SQLException e) {
                                        if (e.getSQLState().equals("42S22")) {

                                            System.out.println("Select statement had sql state 42S22 (Invalid column name) on column"
                                                    + field.getName() + e.getMessage());
                                        } else {
                                            System.out.println("SQL error in Select" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                                        }
                                    }
                                }
                            }
                            T thisObject1 = (T) thisObject;
                            tArrayList.add(thisObject1);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (ps != null) {
                        try {
                            ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if(con != null) {
                        MySQLDB.getInstance().endConnectionPoolSession(con);
                    }
                }
            }
            return tArrayList;
        }

        static String BabelFieldandData(Field f) {
            String sql = "";

            if (f.getType() == String.class) {
                sql += f.getName() + " TEXT ";
            } else if (f.getType() == int.class || f.getType() == Integer.class) {
                sql += f.getName() + " INT";
            } else if (f.getType() == Long.class || f.getType() == long.class) {
                sql += f.getName() + " BIGINT";
                if (f.getName().equals("rid")) {
                    sql += " PRIMARY KEY AUTO_INCREMENT";
                }
            } else if (f.getType() == Double.class || f.getType() == double.class) {
                sql += f.getName() + " DOUBLE";
            } else if (f.getType() == Float.class || f.getType() == float.class) {
                sql += f.getName() + " FLOAT";
            }
            else if (f.getType() == BigDecimal.class) {
                sql += f.getName() + " NUMERIC(10,4)";
            } else if (f.getType() == boolean.class || f.getType() == Boolean.class) {
                sql += f.getName() + " TINYINT(1)";
            } else if (f.getType() == Date.class) {
                sql += f.getName() + " DATETIME";
                if (f.getName().equals("arsd")) {
                    sql += " DEFAULT CURRENT_TIMESTAMP";
                } else {
                    sql += " NULL";
                }
            } else if (f.getType() == UUID.class) {
                sql += f.getName() + " VARCHAR(132)";
            } else if (f.getType() == Character.class || f.getType() == char.class) {
                sql += f.getName() + " CHAR";
            } else if (f.getType() != null && f.getType().isEnum()) {
                sql += f.getName() + " VARCHAR(200)";
            } else if (Class.class.isAssignableFrom(f.getType())) {
                sql += f.getName() + " VARCHAR(200)";
            }
            else if (f.getGenericType() instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) f.getGenericType();
                if(pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0].toString()
                        .matches(".*?((L|l)ong|Integer|int|(D|d)ouble|(F|f)loat|(B|b)oolean|String).*")) {
                    sql += f.getName() + " TEXT";
                }
                else {
                    // unknown create text fields to serialize
                    System.out.println("Was not able to identify field: {} of type: {} " + f.getName() + f.getType());
                    sql += f.getName() + " BLOB";
                }
            }
            else {
                // unknown create text fields to serialize
                System.out.println("Was not able to identify field: {} of type: {} " + f.getName() + f.getType());
                sql += f.getName() + " BLOB";
            }
            return sql;
        }

        static String parseWhere(List<BabelSQLResultsFilter> where) {
        String sql = "";
        //If where clauses were passed, add them to the sql statement
        if(where != null && where.size() > 0) {
            sql += " WHERE";
            for(BabelSQLResultsFilter clause : where) {
                // if there is more then one clause, check the conditional type.
                if(where.indexOf(clause) != 0 && (where.indexOf(clause) +1) <= where.size()) {
                    sql += " " + clause.conditionals.name();
                }

                // check to see if there are any open parenthesis to apply
                if(clause.groups != null && clause.groups.size() > 0) {
                    for(BabelSQLResultsFilter.GROUPS g: clause.groups) {
                        if(g == BabelSQLResultsFilter.GROUPS.OPEN_BRACKETS) {
                            sql += " (";
                        }
                    }
                }

                // add the claus formatting the sql for the correct datatype
                if(clause.operators != BabelSQLResultsFilter.Operators.UNARY) {
                    sql += " " + clause.name + " " + clause.OperatorString() + " ";
                    if(clause.comVal.equals(BabelSQLResultsFilter.Null)) {
                        sql += clause.comVal;
                    }
                    else if(clause.primaryDataType == int.class || clause.primaryDataType == Integer.class ||
                            clause.primaryDataType == long.class || clause.primaryDataType == Long.class ||
                            clause.primaryDataType == double.class || clause.primaryDataType == Double.class ||
                            clause.primaryDataType == float.class || clause.primaryDataType == Float.class ||
                            clause.primaryDataType == BigDecimal.class) {
                        sql += clause.comVal;
                    }
                    else if (clause.primaryDataType == boolean.class || clause.primaryDataType == Boolean.class) {
                        sql += clause.comVal;
                    }
                    else {
                        sql += "'" + clause.comVal + "'";
                    }
                }

                // check to see if there are any closing parenthesis to apply
                if(clause.groups != null && clause.groups.size() > 0) {
                    for(BabelSQLResultsFilter.GROUPS grouping: clause.groups) {
                        if(grouping == BabelSQLResultsFilter.GROUPS.CLOSE_BRACKETS) {
                            sql += " )";
                        }
                    }
                }
            }
        }
        return sql;
    }
    }

