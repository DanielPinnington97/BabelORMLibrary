package com.BabelORM.BabelUser;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BabelSQLResultsFilter {

    public String name = "";

    public Class primaryDataType;

    public String comVal = "";

    public Type secondaryDataType;

    public String secondary = null;

    public final static String Null = "NULL";

    public CONDITIONALS conditionals;

    public enum CONDITIONALS {
        AND, OR, NOT
    }

    public List<GROUPS> groups = new ArrayList<>();

    public enum GROUPS {
        OPEN_BRACKETS, CLOSE_BRACKETS
    }

    public Operators operators;

    public enum Operators {
        UNARY, EQUAL, NOT_EQUAL, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, BETWEEN, LIKE, IN, IS, IS_NOT,
    }

    public String OperatorString() {
        switch (this.operators) {
            case EQUAL:
                return "=";
            case NOT_EQUAL:
                return "<>";
            case GREATER_THAN:
                return ">";
            case LESS_THAN:
                return "<";
            case GREATER_THAN_OR_EQUAL:
                return ">=";
            case LESS_THAN_OR_EQUAL:
                return "<=";
            case IN:
                return "IN";
            case BETWEEN:
                return "BETWEEN";
            case LIKE:
                return "LIKE";
            case IS:
                return "IS";
            case IS_NOT:
                return "IS NOT";
            default:
                return null;
        }
    }

    public BabelSQLResultsFilter() {
        conditionals = CONDITIONALS.AND;
        operators = Operators.EQUAL;
    }
}
