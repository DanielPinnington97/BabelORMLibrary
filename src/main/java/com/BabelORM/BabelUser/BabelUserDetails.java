package com.BabelORM.BabelUser;


import java.io.Serializable;
import java.util.Date;

public class BabelUserDetails<T> implements Serializable {

    public long ID;
    public long rID;
    public long euID;

    public Date date1;
    public Date date2;

    public boolean bf;

    public BabelUserDetails() {

        this.ID = -1;
        this.rID = -1;
        this.euID = -1;

        this.date1 = null;
        this.date2 = null;

        this.bf = false;
    }

}
