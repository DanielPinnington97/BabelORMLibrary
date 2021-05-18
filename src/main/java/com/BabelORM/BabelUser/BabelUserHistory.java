package com.BabelORM.BabelUser;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BabelUserHistory <T extends BabelUserDetails> {

    public T current;
    public List<T> hist;
    public long entID = -1;

    public BabelUserHistory() {
        current = null;
        hist = new ArrayList<>();
    }

    public BabelUserHistory(T currState, List<T> histState) {
        entID = currState.ID;
        current = currState;
        hist = histState;
    }

    public T stateFromID(long rID) {
        T thisState = null;

        if(this.current != null) {
            if(this.current.rID == rID) {
                thisState = this.current;
            }
        }
        if(this.hist != null && this.hist.size() > 0) {
            for(T histState : this.hist) {
                if(histState.rID == rID) {
                    thisState = histState;
                }
            }
        }

        return thisState;
    }

    public T recentUser() {

        if(this.current != null) {
            return this.current;
        }
        else {
            if(this.hist != null && this.hist.size() > 0){
                T recentState = null;
                for(T histStateRecord : this.hist) {
                    if(recentState == null) {
                        recentState = histStateRecord;
                    }
                    else {
                        if(histStateRecord.date2.getTime() > recentState.date2.getTime()) {
                            recentState = histStateRecord;
                        }
                    }
                }
                return recentState;
            }
        }
        return null;
    }

    public T userDateEntity(Date date) {
        T thisState = null;

        if(this.current != null) {
            if(this.current.date1.before(date) && this.current.date2.after(date)) {
                thisState = current;
            }
        }
        if(this.hist != null && this.hist.size() > 0) {
            for (T histState: this.hist) {
                if(histState.date1.before(date) && histState.date2.after(date)) {
                    thisState = histState;
                }
            }
        }
        return thisState;
    }
}
