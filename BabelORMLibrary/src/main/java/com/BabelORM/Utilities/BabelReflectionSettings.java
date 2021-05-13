package com.BabelORM.Utilities;

import java.lang.reflect.Field;

public class BabelReflectionSettings {

    private static final String PREFIX = "class ";

    public static String className(Class className) {

        String classString = className.toString();

        if (classString.startsWith(PREFIX)) {
            return classString.substring(PREFIX.length());
        }

        return classString;
    }

    public static String className(String className) {

        if (className.startsWith(PREFIX)) {
            return className.substring(PREFIX.length());
        }

        return className;
    }

    public static Class getClassFNAME(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
