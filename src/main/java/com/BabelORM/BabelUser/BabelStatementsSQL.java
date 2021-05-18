package com.BabelORM.BabelUser;

import com.BabelORM.Settings.BabelSystems;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BabelStatementsSQL {

    public Arrays userState;
    private final List<String> select;
    private final List<BabelSQLResultsFilter> where;
    private final List<String> groupBY;
    private final List<String> orderBY;
    private int lim, off;

    private BabelStatementsSQL() {
        select = new ArrayList<>();
        where = new ArrayList<>();
        groupBY = new ArrayList<>();
        orderBY = new ArrayList<>();
        lim = off = 0;
    }

    private BabelStatementsSQL(BabelStatementsSQL split) {
        this();
        select.addAll(split.select);
        where.addAll(split.where);
        groupBY.addAll(split.groupBY);
        orderBY.addAll(split.orderBY);
        lim = split.lim;
        off = split.off;
    }

    public static BabelStatementsSQL construct() { return new BabelStatementsSQL(); }

    public BabelStatementsSQL split() {return  new BabelStatementsSQL(this); }

    //select statement builder
    public BabelStatementsSQL select(String selectData) {
        if(!selectData.isEmpty()) {
            select.add(selectData);
        }
        return this;
    }

    public BabelStatementsSQL select(List<String> selectDataList) {
        select.addAll(selectDataList.stream().filter(selectData -> !selectData.isEmpty()).collect(Collectors.toList()));
        return this;
    }

    //where clause statement builder
    public BabelStatementsSQL where(BabelSQLResultsFilter whereC) {
        if(whereC != null) {
            where.add(whereC);
        }
        return this;
    }

    public BabelStatementsSQL where(List<BabelSQLResultsFilter> whereS) {
        where.addAll(whereS.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        return this;
    }

    //orderBy statement builder
    public BabelStatementsSQL orderBY(String orderDataBY) {
        if(!orderDataBY.isEmpty()) {
            orderBY.add(orderDataBY);
        }
        return this;
    }

    public BabelStatementsSQL orderBY(List<String> orderDataBY) {
        orderBY.addAll(orderDataBY.stream().filter(orderings -> !orderings.isEmpty()).collect(Collectors.toList()));
        return this;
    }

    //groupBy statement builder
    public BabelStatementsSQL groupBY(String groupDataBy) {
        if(!groupDataBy.isEmpty()) {
            groupBY.add(groupDataBy);
        }
        return this;
    }

    public BabelStatementsSQL groupBY(List<String> groupDataby)  {
        groupBY.addAll(groupDataby.stream().filter(groupBy -> !groupBy.isEmpty()).collect(Collectors.toList()));
        return this;
    }

    public BabelStatementsSQL lim(int resPerPage, int curPage) {
        off = (curPage -1) * (lim = resPerPage);
        return this;
    }

    public <T extends BabelUserDetails> List<T> run(Class<T> userState) throws SQLException {
        return BabelSystems.sqlStatementSelect(userState, this);
    }

    public String getSel() {
        StringBuilder sql = new StringBuilder();
        if(!select.isEmpty()) {
            for (int s =0; s<select.size(); s++) {
                if (s> 0) {
                    sql.append(", ");
                }
                sql.append(select.get(s));
            }
        } else {
            sql.append("*");
        }
        return sql.toString();
    }

    public String getGrBy() {
        StringBuilder sql = new StringBuilder();
        groupBY.forEach(group -> {
            if(group.indexOf(group) == 0) {
                sql.append(" GROUP BY ");
            }
            else {
                sql.append(",");
            }
            sql.append(" ").append(group);
        });
        return sql.toString();
    }

    public String getOrBy() {
        StringBuilder sql = new StringBuilder();
        orderBY.forEach(order -> {
            if(orderBY.indexOf(order) == 0) {
                sql.append(" ORDER BY ");
            }
            else {
                sql.append(",");
            }
            sql.append(" ").append(order);
        });
        return sql.toString();
    }

    public String getLim() {
        StringBuilder sql = new StringBuilder();
        if(lim > 0 && off > - 1) {
            sql.append(" LIMIT ").append(lim);
            if(off > 0) {
                sql.append(" OFFSET ").append(off);
            }
        }
        return sql.toString();
    }

    public int[] setLim() {
        int[] man = {off, lim};
        return man;
    }
}
