package com.BabelORM.DBConfiguration.BabelQueries;

import com.BabelORM.Annotation.BabelAnnotation;
import com.BabelORM.BabelUser.BabelSQLResultsFilter;
import com.BabelORM.BabelUser.BabelStatementsSQL;
import com.BabelORM.BabelUser.BabelUserDetails;
import com.BabelORM.DBConfiguration.ConnectionSettings.MariaDB;
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

public class BabelMariaDBQueries implements BabelPersistenceConfig {

    private static final BabelMariaDBQueries DBINST = new BabelMariaDBQueries();

    private BabelMariaDBQueries() {}

    public static BabelMariaDBQueries getINST() {
        return DBINST;
    }

    @Override
    public void checkDB() throws SQLException {

        Connection con = null;
        Statement s = null;
        ResultSet rs = null;

        try {
            con = MariaDB.getInstance().getBabelRootConnectionPool();
            s = con.createStatement();
            if(s !=null) {
                rs = s.executeQuery("SELECT * FROM INFORMATION_SCHEMA WHERE SCHEMA_NAME = '" + BabelSettings.DBNAME + " ':' ");
                if(rs !=null) {
                    String DBCreate = "CREATE DATABASE IF NOT EXISTS " + BabelSettings.DBNAME + " CHARACTER SET" + BabelSettings.ENCODING + ";";
                    String DBPrivileges = "GRANT ALL ON " + BabelSettings.DBNAME + ".* to '" + BabelSettings.USERNAME + "'@'" + BabelSettings.DBHOST
                            + "' IDENTIFIED BY '" + BabelSettings.PASSWORD + "'";

                    if(s != null) {
                        s.executeUpdate(DBCreate);
                        s.executeUpdate(DBPrivileges);
                        System.out.println("DB did not exist, Babel will attempt to create it");
                        System.out.println("DB created by Babel");
                    }
                }
            }

        }
        catch (SQLException e) {
            System.out.println("Error creating DB");
            e.getErrorCode();
        }
        if (rs != null) {
            rs.close();
        }
        if (s != null) {
            s.close();
        }
        if (con != null) {
            MariaDB.getInstance().endConnectionPoolSession(con);
        }

    }

    @Override
    public void checkT() throws SQLException {

        for(Class c: BabelSettings.getINST().mCLass) {

            if(!c.isAnnotationPresent(BabelAnnotation.class)) {
                Connection con = null;
                Statement s = null;
                ResultSet rs = null;

                try{
                    con = MariaDB.getInstance().getBabelConnectionPool();
                    s = con.createStatement();
                    if(s != null) {
                        rs = s.executeQuery("SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA = '"
                            + BabelSettings.DBNAME + "' AND TABLE_NAME = '" + c.getSimpleName() + "';");
                        if(rs != null && !rs.next()) {
                            System.out.println("attempting to create a table " + c.getSimpleName());

                            if(c.getFields().length > 0) {
                                StringBuilder mariaSQL = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                                    .append(BabelSettings.DBNAME).append(".")
                                        .append(c.getSimpleName()).append(" (");
                                Arrays.stream(c.getFields()).filter(field -> !field.isAnnotationPresent(BabelAnnotation.class))
                                        .forEach(field -> mariaSQL.append(" ").append(BabelFieldandData(field)).append(","));

                                String query = mariaSQL.deleteCharAt(mariaSQL.length()-1).append("):").toString();
                                System.out.println("Babel table created: " + c.getSimpleName() + query );
                                s.executeUpdate(query);
                            } else {
                                System.out.println("Babel has not been able to create a table object" + c.getSimpleName());
                            }
                        }
                    }
                }
                catch (SQLException e) {
                    System.out.println("Babel has encountered an error" + e.getErrorCode() + e.getSQLState() + e.getMessage());
                }
                catch (Exception exception) {
                    exception.printStackTrace();
                }
                finally {
                    if (rs != null) {
                        rs.close();
                    }
                    if (s != null) {
                        s.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                }
            }
        }

    }

    @Override
    public void checkF() throws SQLException {

        for(Class f: BabelSettings.getINST().mCLass) {

            if(!f.isAnnotationPresent(BabelAnnotation.class)) {

                List<Field> fields = Arrays.stream(f.getFields()).filter(field -> !field.isAnnotationPresent(BabelAnnotation.class))
                        .collect(Collectors.toList());

                if(fields.size() > 0) {

                    Connection con = null;
                    Statement s = null;
                    ResultSet rs = null;

                    try {
                        con = MariaDB.getInstance().getBabelConnectionPool();
                        s = con.createStatement();
                        for(Field field : fields) {
                            String exist = "SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '"
                                    + BabelSettings.DBNAME + "' AND TABLE_NAME = '" + f.getSimpleName() + "' AND COLUMN_NAME= '"
                                    + field.getName() + "';";
                            if (s != null && (rs = s.executeQuery(exist)) != null && !rs.next()) {
                                System.out.println("Babel did not detect a duplicate..." + field.getName() + f.getSimpleName());
                                System.out.println("Babel is creating a new field...");

                                String update = "ALTER TABLE " + BabelSettings.DBNAME + "." + f.getSimpleName() + " ADD COLUMN " + BabelFieldandData(field);
                                s.executeUpdate(update);
                            }
                        }
                    }
                    catch (SQLException sqlE) {
                        System.out.println("Babel failed to review the field");
                        System.out.println("Encountered " + sqlE.getMessage() + sqlE.getSQLState() + sqlE.getErrorCode());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        if(rs != null) {
                            rs.close();
                        }
                        if (s != null) {
                            s.close();
                        }
                        if (con != null) {
                            MariaDB.getInstance().closeDB();
                        }
                    }

                }
            }
        }

    }


    @Override
    public <T extends BabelUserDetails<BabelSystems>> List<T> selectQuery(Class c, BabelStatementsSQL sql) {

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
                con = MariaDB.getInstance().getBabelConnectionPool();
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
                    MariaDB.getInstance().closeDB();
                }
            }
        }
        return tArrayList;
    }

    @Override
    public <T> Long insert(Class<T> userState, T s) {

        long newId = -1L;

        if(!userState.isAnnotationPresent(BabelAnnotation.class)) {
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                //Remove @FdfIgnore Fields
                List<Field> f = Arrays.stream(userState.getFields()).filter(field -> !field.isAnnotationPresent(BabelAnnotation.class)
                        && !field.getName().equals("rid")).collect(Collectors.toList());
                //Start Sql Statement
                StringBuilder query = new StringBuilder("INSERT INTO ").append(BabelSettings.DBNAME).append(".")
                        .append(userState.getSimpleName()).append(" ("),
                        val = new StringBuilder();
                f.forEach(field -> {
                    query.append(" ").append(field.getName()).append(",");
                    val.append(" ?,");
                });
                query.deleteCharAt(query.length()-1).append(") VALUES (")
                        .append(val.deleteCharAt(val.length()-1).toString()).append(");");
                //Create Connection
                con = MariaDB.getInstance().getBabelConnectionPool();
                ps = con.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
                int count = 1;
                for (Field field : f) {
                    try {
                        if (field.getType() == String.class) {
                            if (field.get(s) != null) {
                                ps.setString(count, field.get(s).toString());
                            } else {
                                ps.setNull(count, Types.VARCHAR);
                            }
                        } else if (field.getType() == int.class || field.getType() == Integer.class) {
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
                                ps.setNull(count, Types.TINYINT);
                            }
                        } else if (field.getType() == char.class || field.getType() == Character.class) {
                            if (field.get(s) != null) {
                                ps.setString(count, field.get(s).toString().substring(0, 1));
                            } else {
                                ps.setNull(count, Types.CHAR);
                            }
                        } else if (field.getType() == UUID.class) {
                            if (field.get(s) != null) {
                                ps.setString(count, field.get(s).toString());
                            } else {
                                ps.setNull(count, Types.VARCHAR);
                            }
                        } else if (field.getType() == Date.class) {
                            if (field.get(s) != null) {
                                ps.setTimestamp(count, new Timestamp(((Date) field.get(s)).getTime()));
                            } else {
                                ps.setNull(count, Types.DATE);
                            }
                        } else if (field.getType() != null && field.getType().isEnum()) {
                            if (field.get(s) != null) {
                                ps.setString(count, field.get(s).toString());
                            } else {
                                ps.setNull(count, Types.VARCHAR);
                            }
                        } else if (Class.class.isAssignableFrom(field.getType())) {
                            if (field.get(s) != null) {
                                ps.setString(count, BabelReflectionSettings.className(field.get(s).toString()));
                            } else {
                                ps.setNull(count, Types.VARCHAR);
                            }
                        } else if (field.getGenericType() instanceof ParameterizedType && field.get(s) != null) {
                            ParameterizedType pt = (ParameterizedType) field.getGenericType();
                            if(pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0].toString()
                                    .matches(".*?((L|l)ong|Integer|int|(D|d)ouble|(F|f)loat|(B|b)oolean|String).*")) {
                                ps.setString(count, field.get(s).toString());
                            } else try {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ObjectOutputStream oos = new ObjectOutputStream(baos);
                                oos.writeObject(field.get(s));
                                oos.close();
                                ps.setString(count,
                                        Base64.getEncoder().encodeToString(baos.toByteArray()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if(field.get(s) != null) {
                            try {
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                                objectOutputStream.writeObject(field.get(s));
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
                System.out.println("babel insert " + ps);
                ps.execute();
                rs = ps.getGeneratedKeys();
                if(rs != null && rs.next()) {
                    newId = rs.getLong(1);
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
                    MariaDB.getInstance().closeDB();
                }
            }
        }
        return newId;
    }

    @Override
    public <T> void update(Class<T> c, T s) {

        if (!c.isAnnotationPresent(BabelAnnotation.class)) {
            Connection con = null;
            PreparedStatement ps = null;

            try {

                List<Field> f = Arrays.stream(c.getFields()).filter(field -> !field.isAnnotationPresent(BabelAnnotation.class)
                && !field.getName().equals("rID")).collect(Collectors.toList());

                StringBuilder query = new StringBuilder("UPDATE").append(BabelSettings.DBNAME).append(".")
                        .append(c.getSimpleName()).append(" SET");
                f.forEach(field -> {
                    try {
                        query.append(" ").append(" WHERE rID = ").append(c.getField("rID").get(s)).append(";");
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                });

                con = MariaDB.getInstance().getBabelConnectionPool();
                ps = con.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);

                int count = 1;
                for (Field field: f) {
                    try {
                        if (field.getType() == String.class) {
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
                            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                            if(parameterizedType.getActualTypeArguments().length == 1 && parameterizedType.getActualTypeArguments()[0].toString()
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
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                                objectOutputStream.writeObject(field.get(s));
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
                System.out.println("Babel update " + ps);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
        } finally {
                if (ps != null) try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if (con != null) {
                    MariaDB.getInstance().closeDB();
                }
    }
}

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
                    /*if(clause.conditional == WhereClause.CONDITIONALS.AND) {
                        sql += " AND";
                    }
                    else if (clause.conditional == WhereClause.CONDITIONALS.OR) {
                        sql += " OR";
                    }
                    else if (clause.conditional == WhereClause.CONDITIONALS.NOT) {
                        sql += " NOT";
                    }*/
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
