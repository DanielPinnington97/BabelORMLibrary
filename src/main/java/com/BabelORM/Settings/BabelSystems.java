package com.BabelORM.Settings;

import com.BabelORM.BabelUser.BabelSQLResultsFilter;
import com.BabelORM.BabelUser.BabelStatementsSQL;
import com.BabelORM.BabelUser.BabelUserDetails;
import com.BabelORM.BabelUser.BabelUserHistory;
import com.BabelORM.DBConfiguration.Exceptions.BabelDBConfig;
import com.BabelORM.Utilities.BabelDateSettings;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;

@SuppressWarnings("unused")
public abstract class BabelSystems {

        public static <T extends BabelUserDetails> T save(T state, Class<T> userState, long id, long userId, long systemId) {
            return save(state, userState, userId, systemId, 1);
        }

        public static <T extends BabelUserDetails> T save(T state, Class<T> userState, long euID) {
            return save(state, userState, state.euID);
        }

        public static <T extends BabelUserDetails> T save(T state, Class<T> userState, long userId, long rID) throws SQLException {
            if(state.ID <= 0) {
                //Generate valid id for entity
                state.ID = getNewUserId(userState, rID);
            }
            else {
                //Get the current record and archive it
                T previousState = sortByCurrentID(userState, state.euID, state.rID);

                if(previousState != null) {
                    //Mark as inactive
                    previousState.bf = false;
                    //Record inactivity date
                    previousState.date2 = Calendar.getInstance().getTime();
                    //Update old state
                    BabelDBConfig.getINST().update(userState, previousState);
                }
            }
            //Set generic fields for new record, save entity, then return it.
            state.date1 = Calendar.getInstance().getTime();
            state.date2 = null;
            state.bf = true;
            state.euID = userId;
            state.rID = rID;
            return auditEntityByRid(userState, BabelDBConfig.getINST().insert(userState, state));
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> save(Class<T> userState, T state, long userId, long systemId, long tenantId) {
            return save(userState, state, userId, state.rID, 1);
        }

        public static <T extends BabelUserDetails> BabelUserDetails save(Class<T> userState, T state, long euID, long rID) {
            return save(userState, state, state.euID, state.rID);
        }


        public static <T extends BabelUserDetails> BabelUserHistory<T> save(Class<T> userState, T state) throws SQLException {
            // set the common meta fields for the new record
            state.date1 = Calendar.getInstance().getTime();
            state.date2 = null;
            state.bf = true;
            state.euID = 0;
            long rID = 0;
            state.rID = rID;
            // check to see if this if an id is assigned (existing vs new entity)
            if(state.ID <= 0) {
                // if this is a new entity, get an id for it
                state.ID = getNewUserId(userState, rID);
                if(state.ID < 0) {
                    return new BabelUserHistory<>();
                }
            }
            // get full entity for state
            BabelUserHistory<T> thisEntity = auditEntityById(userState, state.ID, rID);
            // check to see if there is an existing entity, if not, create
            // get the previous current record and move to history
            if(thisEntity.current != null) {
                T lastCurrentState = thisEntity.current;
                // set the end date
                lastCurrentState.date2 = Calendar.getInstance().getTime();
                // set the current flag
                lastCurrentState.bf = false;
                // move the state to history
                BabelDBConfig.getINST().update(userState, lastCurrentState);
            }
            // save the new state as current
            long returnedRid = BabelDBConfig.getINST().insert(userState, state);
            // get id for rid
            T entity = auditEntityByRid(userState, rID);
            // get the entity and return
            return auditEntityById(userState, entity.ID, rID);
        }

        public static <T extends BabelUserDetails> BabelUserHistory setDeleteFlag(Class<T> entityState, long id, long userId, long systemId) throws SQLException {
            return setDeleteFlag(entityState, id, userId, systemId, 1);
        }

        public static <T extends BabelUserDetails> BabelUserHistory setDeleteFlag(Class<T> userState, long id, long userId, long systemId, long tenantId) throws SQLException {
            if(id > -1) {
                // get full entity for state
                BabelUserHistory<T> thisEntity = auditEntityById(userState, id);
                // create the new state that will maintain the deletion records from the most recent state available
                T deletedState = thisEntity.stateFromID(id);
                // mark the state deleted
                deletedState.bf = true;
                // save the state
                return save(userState, deletedState);
            }
            return new BabelUserHistory();
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> removeDeleteFlag(Class<T> entityState, long id, long userId, long systemId) throws SQLException {
            return removeDeleteFlag(entityState, id, userId, systemId, 1);
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> removeDeleteFlag(Class<T> userState, long id, long userId, long systemId, long tenantId) throws SQLException {
            if(id > -1) {
                // get full entity for state
                BabelUserHistory<T> thisEntity = auditEntityById(userState, id, tenantId);
                // create the new state that will maintain the deletion records from the most recent state available
                T deletedState = thisEntity.recentUser();
                // mark the state deleted
                deletedState.bf = false;
                // save the state
                return save(userState, deletedState, userId, systemId, tenantId);
            }
            return new BabelUserHistory<>();
        }

        public static <T extends BabelUserDetails> long getRowCount(Class<T> entityState) throws SQLException {
            BabelUserDetails returnedState = BabelStatementsSQL.construct().select("max(rid) AS rid").run(entityState).stream().findAny().orElse(null);
            return (returnedState != null ? returnedState.rID : 0);
        }

        public static <T extends BabelUserDetails> long getEntityCount(Class<T> entityState) {
            List<T> returnedQuery = BabelDBConfig.getINST().selectQuery(entityState, BabelStatementsSQL.construct().select("distinct id"));
            return(returnedQuery != null ? returnedQuery.size() : 0);
        }

        public static <T extends BabelUserDetails> List<T> sqlStatementSelect(Class<T> userState, BabelStatementsSQL sqlStatement) throws SQLException {
            return BabelDBConfig.getINST().selectQ(userState, sqlStatement);
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> auditAll(Class<T> entityState) throws SQLException {
            return auditAll(entityState, 1);
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> auditAll(Class<T> entityState, long tenantId) throws SQLException {
            return handleReturnUsers(BabelStatementsSQL.construct().where(addByRid(tenantId)).run(entityState));
        }

        public static <T extends BabelUserDetails> List<T> auditAllCurrent(Class<T> entityState) throws SQLException {
            return auditAllCurrent(entityState, 1);
        }

        public static <T extends BabelUserDetails> List<T> auditAllCurrent(Class<T> entityState, long tenantId) throws SQLException {
            return BabelStatementsSQL.construct().where(setWithHistory(tenantId)).run(entityState);
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getAll(Class<T> entityState) throws SQLException {
            return getAll(entityState, 1);
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getAll(Class<T> entityState, long tenantId) throws SQLException {
            return handleReturnUsers(BabelStatementsSQL.construct().where(setWithHistory(tenantId)).run(entityState));
        }

        public static <T extends BabelUserDetails> List<T> getAllCurrent(Class<T> entityState) throws SQLException {
            return getAllCurrent(entityState, 1);
        }

        public static <T extends BabelUserDetails> List<T> getAllCurrent(Class<T> entityState, long tenantId) throws SQLException {
            return BabelStatementsSQL.construct().where(setWithHistory(tenantId)).run(entityState);
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getAllHistory(Class<T> entityState) throws SQLException {
            return getAllHistory(entityState, 1);
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getAllHistory(Class<T> entityState, long tenantId) throws SQLException {
            return handleReturnUsers(BabelStatementsSQL.construct().where(addByBf()).where(setWithHistory(tenantId)).run(entityState));
        }

        public static <T extends BabelUserDetails> List<T> getAllAtDate(Class<T> entityState, Date date, int i) {
            return getAllAtDate(entityState, date, 1);
        }

        public static <T extends BabelUserDetails> List<T> auditAllAtDate(Class<T> entityState, Date date, int i) {
            return auditAllAtDate(entityState, date, 1);
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getAllFromDate(Class<T> entityState, Date date) throws SQLException {
            return getAllFromDate(entityState, date, 1);
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getAllFromDate(Class<T> entityState, Date date, long tenantId) throws SQLException {
            return handleReturnUsers(BabelStatementsSQL.construct().where(addByDate2After(date)).where(setWithHistory(tenantId)).run(entityState));
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getAllBeforeDate(Class<T> entityState, Date date) throws SQLException {
            return getAllBeforeDate(entityState, date, 1);
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getAllBeforeDate(Class<T> entityState, Date date, long tenantId) throws SQLException {
            return handleReturnUsers(BabelStatementsSQL.construct().where(addByDate1Before(date)).where(setWithHistory(tenantId)).run(entityState));
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getAllBetweenDates(Class<T> entityState, Date startDate, Date endDate) throws SQLException {
            return getAllBetweenDates(entityState, startDate, endDate, 1);
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getAllBetweenDates(Class<T> entityState, Date startDate, Date endDate, long tenantId) throws SQLException {
            return handleReturnUsers(BabelStatementsSQL.construct().where(addByDate1Before(endDate))
                    .where(addByDate2After(startDate)).where(setWithHistory(tenantId)).run(entityState));
        }

        public static <T extends BabelUserDetails> T getEntityByRid(Class<T> entityState, long rid) throws SQLException {
            return BabelStatementsSQL.construct().where(addByBf()).where(addByRid(rid)).run(entityState).stream().findFirst().orElse(null);
        }

        public static <T extends BabelUserDetails> T auditEntityByRid(Class<T> entityState, long rid) throws SQLException {
            return BabelStatementsSQL.construct().where(addByRid(rid)).run(entityState).stream().findFirst().orElse(null);
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> auditEntityById(Class<T> entityState, long id) throws SQLException {
            return auditEntityById(entityState, id, 1);
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> auditEntityById(Class<T> entityState, long id, long tenantId) throws SQLException {
            return manageReturnedEntity(BabelStatementsSQL.construct().where(addByIdLong(id)).where(addByRid(tenantId)).run(entityState));
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> getEntityById(Class<T> entityState, long id) throws SQLException {
            return getEntityById(entityState, id, 1);
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> getEntityById(Class<T> entityState, long id, long tenantId) throws SQLException {
            return manageReturnedEntity(BabelStatementsSQL.construct().where(addByIdLong(id)).where(setWithHistory(tenantId)).run(entityState));
        }

        public static <T extends BabelUserDetails> T getEntityCurrentById(Class<T> entityState, long id) throws SQLException {
            return getEntityCurrentById(entityState, id, 1);
        }

        public static <T extends BabelUserDetails> T getEntityCurrentById(Class<T> entityState, long id, long tenantId) throws SQLException {
            return BabelStatementsSQL.construct().where(addByIdLong(id)).where(setWithHistory(tenantId)).run(entityState).stream().findAny().orElse(null);
        }

        public static <T extends BabelUserDetails> T sortByCurrentID(Class<T> userState, long id, long tenantId) throws SQLException {
            return BabelStatementsSQL.construct().where(addByIdLong(id)).where(setWithHistory(tenantId)).run(userState).stream().findAny().orElse(null);
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> getEntityHistoryById(Class<T> entityState, long id) throws SQLException {
            return getEntityHistoryById(entityState, id, 1);
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> getEntityHistoryById(Class<T> entityState, long id, long tenantId) throws SQLException {
            return manageReturnedEntity(BabelStatementsSQL.construct().where(addByBf()).where(addByIdLong(id)).where(setWithHistory(tenantId)).run(entityState));
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getEntitiesByValueForPassedField(Class<T> entityState, String fieldName, String value) {
            return getEntitiesByValueForPassedField(entityState, fieldName, value, 1);
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getEntitiesByValueForPassedField(Class<T> userState, String fieldName, String value, long tenantId) {
            if(value != null && tenantId > 0) try {
                Class passedFieldType = userState.getField(fieldName).getType();
                BabelSQLResultsFilter whereField = new BabelSQLResultsFilter();
                whereField.name = fieldName;
                whereField.operators = BabelSQLResultsFilter.Operators.EQUAL;
                whereField.comVal = value;
                whereField.primaryDataType = passedFieldType;
                return handleReturnUsers(BabelStatementsSQL.construct().where(whereField).where(setWithHistory(tenantId)).run(userState));
            } catch (NoSuchFieldException | SQLException e) {
                System.out.println("Babel unable to find field" + fieldName);
            }
            return new ArrayList<>();
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getEntitiesByValuesForPassedFields(Class<T> userState, HashMap<String, String> fieldsAndValues) throws SQLException {
            return getEntitiesByValuesForPassedFields(userState, fieldsAndValues, 1);
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> getEntitiesByValuesForPassedFields(Class<T> userState, HashMap<String, String> fieldsAndValues, long tenantId) throws SQLException {
            BabelStatementsSQL babelStatementsSQL = BabelStatementsSQL.construct();
            for(Map.Entry<String, String> fieldValuePair: fieldsAndValues.entrySet()) {
                try {
                    if(fieldValuePair.getValue() != null && tenantId > 0) {
                        Field field = userState.getField(fieldValuePair.getKey());
                        if(field != null) {
                            Class passedFieldType = field.getType();
                            BabelSQLResultsFilter babelSQLResultsFilter = new BabelSQLResultsFilter();
                            babelSQLResultsFilter.conditionals = BabelSQLResultsFilter.CONDITIONALS.AND;
                            babelSQLResultsFilter.name = fieldValuePair.getKey();
                            babelSQLResultsFilter.operators = BabelSQLResultsFilter.Operators.EQUAL;
                            babelSQLResultsFilter.comVal = fieldValuePair.getValue();
                            babelSQLResultsFilter.primaryDataType = passedFieldType;
                            babelStatementsSQL.where(babelSQLResultsFilter);
                        }
                    }
                }
                catch (NoSuchFieldException e) {
                    return new ArrayList<>();
                }
            }
            return handleReturnUsers(babelStatementsSQL.where(setWithHistory(tenantId)).run(userState));
        }

        public static <T extends BabelUserDetails> T getAtDateById(Class<T> userState, long id, Date date) throws SQLException {
            return getAtDateById(userState, id, date, 1);
        }

        public static <T extends BabelUserDetails> T getAtDateById(Class<T> entityState, long id, Date date, long tenantId) throws SQLException {
            return BabelStatementsSQL.construct().where(addByIdLong(id)).where(setWithHistory(id)).run(entityState).stream().findFirst().orElse(null);
        }

        public static <T extends BabelUserDetails> T auditAtDateById(Class<T> userState, long id, Date date) throws SQLException {
            return auditAtDateById(userState, id, date, 1);
        }

        public static <T extends BabelUserDetails> T auditAtDateById(Class<T> userState, long ID, Date date1, long rID) throws SQLException {
            return BabelStatementsSQL.construct().where(addByIdLong(ID)).where((List<BabelSQLResultsFilter>) auditAtDateById(userState, ID, date1, rID)).run(userState).stream().findFirst().orElse(null);
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> getUserFromDateById(Class<T> userState, long ID, Date date1) throws SQLException {
            return getUserFromDateById(userState, ID, date1, 1);
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> getUserFromDateById(Class<T> userState, long ID, Date date1, long rID) throws SQLException {
            return manageReturnedEntity(BabelStatementsSQL.construct().where(addByDate2After(date1)).where(addByIdLong(ID)).where(setWithHistory(rID)).run(userState));
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> getUserBeforeDateById(Class<T> userState, long ID, Date date1) throws SQLException {
            return getUserBeforeDateById(userState, ID, date1, 1);
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> getUserBeforeDateById(Class<T> userState, long ID, Date date1, long rID) throws SQLException {
            return manageReturnedEntity(BabelStatementsSQL.construct().where(addByDate1Before(date1)).where(addByIdLong(ID)).where(setWithHistory(rID)).run(userState));
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> returnUserDates(Class<T> userState, long ID, Date date1, Date date2) throws SQLException {
            return returnUserDates(userState, ID, date1, date2, 1);
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> returnUserDates(Class<T> userState, long ID, Date date1, Date date2, long userID ) throws SQLException {
            return manageReturnedEntity(BabelStatementsSQL.construct().where(addByDate1Before(date2)).where(addByDate1Before(date1))
                    .where(addByIdLong(ID)).where(setWithHistory(userID)).run(userState));
        }

        public static <T extends BabelUserDetails> List<BabelUserHistory<T>> handleReturnUsers(List<T> tList) {
            // create a List of entities
            List<BabelUserHistory<T>> babelUserHistories = new ArrayList<>();
            for(T state: tList) {
                // see if this states entityId has already been seen
                boolean b = false;
                // compare this id against existing ones
                for(BabelUserHistory<T> thisEntity : babelUserHistories) {
                    if(thisEntity.entID == state.ID) {
                        b = true;
                        addStateToEntity(state, thisEntity);
                    }
                }
                // state id was not found in existing entities, add a new one
                if(!b) {
                    // build a entity
                    BabelUserHistory<T> entity = new BabelUserHistory<>();
                    addStateToEntity(state, entity);
                    babelUserHistories.add(entity);
                }
            }
            return babelUserHistories;
        }

        public static <T extends BabelUserDetails> BabelUserHistory<T> manageReturnedEntity(List<T> newState) {
            BabelUserHistory<T> tBabelUserHistory = new BabelUserHistory<>();
            newState.forEach(state -> addStateToEntity(state, tBabelUserHistory));
            return tBabelUserHistory;
        }

        public static <T extends BabelUserDetails> void addStateToEntity(T s, BabelUserHistory<T> userHistory) {
            //Check to see if this is the first state being saved to the entity, if so set the entityId
            if(userHistory.entID == -1) {
                userHistory.entID = s.ID;
            }
            //Check to see that the id of the state passed matches the existing id for this entity, otherwise it does not belong here.
            if(userHistory.entID == s.ID) {
                //Check to see if the passed state is the current state. If it isn't, check that it isn't already in history.
                if(s.bf) {
                    userHistory.current = s;
                }
                else if(userHistory.hist.stream().noneMatch(historyState -> historyState.rID == s.rID)) {
                    userHistory.hist.add(s);
                }
            }
        }

        public static long getNewUserId(Class<? extends BabelUserDetails> userState, long rID) throws SQLException {
            BabelUserDetails babelUserDetails = BabelStatementsSQL.construct().select("max(id) AS id").where(setWithHistory(rID)).run(userState).stream().findAny().orElse(null);
            return (babelUserDetails != null && babelUserDetails.ID > 0 ? babelUserDetails.ID + 1 : 1);
        }

        protected static String readSetID(String idSet) {
            if(idSet.matches(".*\\d.*")) {
                idSet = idSet.replaceAll("-+", ",");
            }
            if(!idSet.startsWith("(")) {
                idSet = "(" + idSet;
            }
            if(!idSet.endsWith(")")) {
                idSet += ")";
            }
            return idSet;
        }

        protected static BabelSQLResultsFilter addByRid(long rid) {
            BabelSQLResultsFilter babelSQLResultsFilter = new BabelSQLResultsFilter();
            babelSQLResultsFilter.name = "rID";
            babelSQLResultsFilter.operators = BabelSQLResultsFilter.Operators.EQUAL;
            babelSQLResultsFilter.comVal = Long.toString(rid);
            babelSQLResultsFilter.primaryDataType = Long.class;
            return babelSQLResultsFilter;
        }

        protected static BabelSQLResultsFilter addByIdLong(long id) {
            BabelSQLResultsFilter longID = new BabelSQLResultsFilter();
            longID.name = "ID";
            longID.operators = BabelSQLResultsFilter.Operators.EQUAL;
            longID.comVal = Long.toString(id);
            longID.primaryDataType = Long.class;
            return longID;
        }

        protected static BabelSQLResultsFilter addByIdSet(String idSet) {
            BabelSQLResultsFilter IDSet = new BabelSQLResultsFilter();
            IDSet.name = "ID";
            IDSet.operators = BabelSQLResultsFilter.Operators.IN;
            IDSet.comVal = readSetID(idSet);
            IDSet.primaryDataType = Long.class;
            return IDSet;
        }

        protected static BabelSQLResultsFilter addByBf() {
            BabelSQLResultsFilter bf = new BabelSQLResultsFilter();
            bf.name = "bf";
            bf.operators = BabelSQLResultsFilter.Operators.NOT_EQUAL;
            bf.comVal = "true";
            bf.primaryDataType = Boolean.class;
            return bf;
        }
        protected static BabelSQLResultsFilter addByDate1Before(Date date) {
            if(date != null) {
                BabelSQLResultsFilter byDate1 = new BabelSQLResultsFilter();
                byDate1.name = "date1";
                byDate1.operators = BabelSQLResultsFilter.Operators.LESS_THAN_OR_EQUAL;
                byDate1.comVal = BabelDateSettings.DATE_FORMAT.format(date);
                byDate1.primaryDataType = Date.class;
                return byDate1;
            }
            return null;
        }
        protected static BabelSQLResultsFilter addDate1After(Date date) {
            if(date != null) {
                BabelSQLResultsFilter date1After = new BabelSQLResultsFilter();
                date1After.name = "date1";
                date1After.operators = BabelSQLResultsFilter.Operators.GREATER_THAN_OR_EQUAL;
                date1After.comVal = BabelDateSettings.DATE_FORMAT.format(date);
                date1After.primaryDataType = Date.class;
                return date1After;
            }
            return null;
        }
        protected static BabelSQLResultsFilter addDate2Before(Date date) {
            BabelSQLResultsFilter date2Before = new BabelSQLResultsFilter();
            date2Before.name = "date2";
            if(date != null) {
                date2Before.operators = BabelSQLResultsFilter.Operators.LESS_THAN_OR_EQUAL;
                date2Before.comVal = BabelDateSettings.DATE_FORMAT.format(date);
                date2Before.primaryDataType = Date.class;
            }
            else {
                date2Before.operators = BabelSQLResultsFilter.Operators.IS_NOT;
                date2Before.comVal = BabelSQLResultsFilter.Null;
            }
            return date2Before;
        }
        protected static List<BabelSQLResultsFilter> addByDate2After(Date date) {
            List<BabelSQLResultsFilter> babelSQLResultsFilters = new ArrayList<>();
            BabelSQLResultsFilter whereC = new BabelSQLResultsFilter();
            if(date != null) {
                BabelSQLResultsFilter whereEndAfter = new BabelSQLResultsFilter();
                whereEndAfter.groups.add(BabelSQLResultsFilter.GROUPS.OPEN_BRACKETS);
                whereEndAfter.name = "date2";
                whereEndAfter.operators = BabelSQLResultsFilter.Operators.GREATER_THAN_OR_EQUAL;
                whereEndAfter.comVal = BabelDateSettings.DATE_FORMAT.format(date);
                whereEndAfter.primaryDataType = Date.class;
                babelSQLResultsFilters.add(whereEndAfter);

                whereC.conditionals = BabelSQLResultsFilter.CONDITIONALS.OR;
                whereC.groups.add(BabelSQLResultsFilter.GROUPS.CLOSE_BRACKETS);
            }
            whereC.name = "date2";
            whereC.operators = BabelSQLResultsFilter.Operators.IS;
            whereC.comVal = BabelSQLResultsFilter.Null;
            babelSQLResultsFilters.add(whereC);
            return babelSQLResultsFilters;
        }
        protected static List<BabelSQLResultsFilter> addAtDate(Date date) {
            List<BabelSQLResultsFilter> whereStatement = addByDate2After(date);
            whereStatement.add(0, addByDate1Before(date));
            return whereStatement;
        }

        protected static BabelSQLResultsFilter addEUID(long euID) {
            BabelSQLResultsFilter weuID = new BabelSQLResultsFilter();
            weuID.name = "euID";
            weuID.operators = BabelSQLResultsFilter.Operators.EQUAL;
            weuID.comVal = Long.toString(euID);
            weuID.primaryDataType = Long.class;
            return weuID;
        }

        protected static List<BabelSQLResultsFilter> setWithHistory(long userID) {
            List<BabelSQLResultsFilter> babelSQLResultsFilters = new ArrayList<>();
            babelSQLResultsFilters.add(addByIdLong(userID));
            babelSQLResultsFilters.add(addByBf());
            return babelSQLResultsFilters;
        }


}