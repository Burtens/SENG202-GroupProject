package seng202.group8.datacontroller;


import seng202.group8.AlertHelper;
import seng202.group8.data.Data;
import seng202.group8.io.ConstraintsError;
import seng202.group8.io.Database;
import seng202.group8.io.SortOrder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Interface for data controllers, which are responsible for all database operations for a specific data type,
 * such as getting rows and updating rows.
 * <p>
 * The DataController should follow the singleton pattern (interfaces can't declare static methods)
 */
public abstract class DataController<DataType extends Data> {
    /**
     * If subscribing to object using this 'id', will subscribe to any changes to the DB for that data type.
     */
    public static final int OBSERVE_ALL = -2;
    /**
     * Hashmap where the key is the ID of the object being observed
     */
    protected HashMap<Integer, HashSet<DataObserver<DataType>>> observers;

    /**
     * Gets the prepared statement used by `executeBatch`
     *
     * @return prepared statement for batch inserts
     */
    protected abstract PreparedStatement getBatchAddToDatabaseStatement();

    public DataController() {
        observers = new HashMap<>();
    }


    /**
     * Function called when DB is switched.
     * This is necessary as prepared statements need to be regenerated
     * @return true if completed successfully
     */
    public abstract boolean onDBChange();

    /**
     * Deletes the row in the database with the given id, regardless of if it exists or not
     *
     * @param id id of the row to delete
     * @throws SQLException Error connecting to database, or some similar unrecoverable error
     */
    public abstract void deleteFromDatabase(int id) throws SQLException;

    /**
     * Deletes the row in the given table with the given id, regardless of if it exists or not.
     * Does NOT notify observers
     *
     * @param tableName The name of the table that the row is to be deleted from
     * @param id        id of the row to delete
     * @throws SQLException Error connecting to database, or some similar unrecoverable error
     */
    public void deleteFromDatabase(String tableName, int id) throws SQLException {
        Database.establishConnection();
        String SQLQuery = String.format("DELETE FROM %s WHERE ID = ? ", tableName);

        try(PreparedStatement statement = Database.databaseConnection.prepareStatement(SQLQuery)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    /**
     * Attempts to retrieve an entity from the database with a given ID
     *
     * @param id ID of the entity to get
     * @return Data object corresponding to entity with the given ID, or null if it could not be found
     * @throws SQLException Error connecting to database, or some similar unrecoverable error
     */
    public abstract DataType getEntity(int id) throws SQLException;

    /**
     * Queries the database for a list of sorted, filtered, paginated data
     *
     * @param sortColumn The column to sort by
     * @param order      The order (eg ascending, descending) to sort by
     * @param numRows    Maximum numbers of rows to return
     * @param offset     Offset for the rows being returned (e.g. numRows=50, offset=50 means it gets the 50-99th rows for the given sort order)
     * @return list of data from the given table, sorted, filtered and paginated by the given arguments
     * @throws SQLException Error connecting to database, or some similar unrecoverable error
     */
    public abstract List<DataType> getSortedFilteredEntities(String sortColumn, SortOrder order, int numRows, int offset) throws SQLException;

    /**
     * Inserts a data object to the database
     *
     * @param data      data object to save
     * @param returnNew if it should return the updated object with correct ID
     * @return data object with the correct ID if returnNew is true
     * @throws SQLException     if an SQL error occurs
     * @throws ConstraintsError if there are one or more invalid values
     */
    protected abstract DataType addToDatabase(DataType data, boolean returnNew) throws SQLException, ConstraintsError;

    /**
     * Inserts a data object to the database
     *
     * @param data data object to save
     * @return data object with the correct ID
     * @throws SQLException     if an SQL error occurs
     * @throws ConstraintsError if there are one or more invalid values
     */
    protected abstract DataType addToDatabase(DataType data) throws SQLException, ConstraintsError;

    /**
     * Updates a data object in the database
     *
     * @param data data object to update
     * @throws SQLException     if an SQL error occurs
     * @throws ConstraintsError if there are one or more invalid values
     */
    protected abstract void updateInDatabase(DataType data) throws SQLException, ConstraintsError;

    /**
     * Saves or updates the data object to the database, and notifies observers
     * @param data      data object to update
     * @param returnNew whether the program should return the new object that it has created
     * @return if a save and returnNew, returns the a new data object with the right ID
     * If a update, returns the existing object
     * @throws SQLException     if SQL error occurs
     * @throws ConstraintsError if insert or update fails due to uniqueness constraint being violated
     */
    public DataType save(DataType data, boolean returnNew) throws SQLException, ConstraintsError {
        DataType saved = data;
        if (data.isMemoryOnly()) {
            saved = addToDatabase(data, returnNew);
            if (!returnNew) {
                return null;
            }
        } else {
            updateInDatabase(data);
        }

        notifyObservers(saved.getId());
        return saved;
    }

    /**
     * Overload for {@link #save(Data, boolean)} with returnNew set to true
     *
     * @param data data object to update
     * @return if a save, returns the a new data object with the right ID, if update, returns the given data object
     * @throws SQLException     if SQL error occurs
     * @throws ConstraintsError if insert or update fails due to uniqueness constraint being violated
     */
    public DataType save(DataType data) throws SQLException, ConstraintsError {
        return save(data, true);
    }

    /**
     * Executes the batched SQL Statements in the batchAddToDatabaseStatement PreparedStatement
     *
     * @param isTesting If this is being run in an automated JUnit test. If true, it will not modify auto-commit settings
     * @return the result from `executeBatch`: an array denoting the number of rows modified for each batch statement 
     */
    protected int[] executeBatch(boolean isTesting) {
        int[] results;
        PreparedStatement batchAddToDatabaseStatement = getBatchAddToDatabaseStatement();
        try {
            if (batchAddToDatabaseStatement == null || batchAddToDatabaseStatement.isClosed()) {
                throw new Error("Batch add to database statement has not been initialized");
            }
            if (!isTesting) {
                Database.databaseConnection.setAutoCommit(false);
            }
            results = batchAddToDatabaseStatement.executeBatch();

            if (!isTesting) {
                Database.databaseConnection.commit();
                Database.databaseConnection.setAutoCommit(true);
            }
            batchAddToDatabaseStatement.clearBatch();
        } catch (SQLException e) {
            // I dont think this can ever happen because of the 'OR IGNORE'
            AlertHelper.showGenericErrorAlert(e, true,
                "Batch Operation Failed",
                "An error occurred while executing a batch import operation",
                null,
                null
            );
            
            throw new ConstraintsError("batch broke :(");
        }
        return results;
    }

    /**
     * Executes the batched SQL Statements in the batchAddToDatabaseStatement PreparedStatement
     *
     * @return the number of rows that were affected
     */
    public int[] executeBatch() {
        return executeBatch(false);
    }


    /**
     * Checks if the ID is valid - errors specific to observer method
     *
     * @param id The ID of the item the observer is observing
     * @Throws IllegalArgumentException if Id is invalid
     */
    private void checkIdObserverMethods(int id) {
        if (id == Data.UNKNOWN_ID) {
            throw new IllegalArgumentException("Cannot observe objects that are not in the database");
        } else if (id <= 0 && id != OBSERVE_ALL) {
            throw new IllegalArgumentException("ID must be a positive integer");
        }
    }

    /**
     * Adds as an observer to an object with the given ID. If the object is already an observer, it will not be added again
     *
     * @param id       ID of the object to observe
     * @param observer the observer object
     */
    public void addObserver(int id, DataObserver<DataType> observer) {
        checkIdObserverMethods(id);

        if (observers.containsKey(id)) {
            observers.get(id).add(observer); // Add to the set
        } else {
            HashSet<DataObserver<DataType>> observerSet = new HashSet<>(); // Create a new set
            observerSet.add(observer);
            observers.put(id, observerSet);
        }
    }

    /**
     * Removes an observer from the object with the given ID
     *
     * @param id       ID of the object to observe
     * @param observer the observer object
     */
    public void removeObserver(int id, DataObserver<DataType> observer) {
        checkIdObserverMethods(id);

        if (observers.containsKey(id)) {
            observers.get(id).remove(observer);
            if (observers.get(id).isEmpty()) {
                // If no one is observing that object, get rid of it
                observers.remove(id);
            }
        }
    }

    /**
     * Notifies all observers, except the one given, that a change has occurred in an object
     * May be useful if the object modifying the data object is itself an observer
     *
     * @param id     ID of the object of interest
     * @param except observer that is not notified of the change
     * @throws SQLException if error getting entity from database
     */
    public void notifyObservers(int id, DataObserver<DataType> except) throws SQLException {
        DataType data = getEntity(id);
        if (data == null) {
            return;
        }
        notifyObservers(data, except);
    }

    /**
     * Notifies all observers, except the one given, that a change has occurred in an object
     * May be useful if the object modifying the data object is itself an observer
     *
     * @param data   data object of interest
     * @param except observer that is not notified of the change
     * @throws SQLException if error getting id of data from database
     */
    public void notifyObservers(DataType data, DataObserver<DataType> except) {
        checkIdObserverMethods(data.getId());

        HashSet<DataObserver<DataType>> globalObservers = observers.get(OBSERVE_ALL);
        if (globalObservers != null) {
            for (DataObserver<DataType> observer : globalObservers) {
                if (observer != except) {
                    observer.dataChangedEvent(data);
                }
            }
        }

        if (observers.containsKey(data.getId())) {
            // If data object null (not found) or there are no observers, don't bother
            for (DataObserver<DataType> observer : observers.get(data.getId())) {
                if (observer != except && (globalObservers == null || !globalObservers.contains(observer))) {
                    observer.dataChangedEvent(data);
                }
            }
        }

    }

    /**
     * Notifies the observers that are observing the global state of this data controller (ie observing the
     * OBSERVE_ALL id).
     * @param data The data object of interest. Null if there is no single object of interest (eg. a change across the entire datacontroller)
     */
    public void notifyGlobalObservers(DataType data) {
        HashSet<DataObserver<DataType>> globalObservers = observers.get(OBSERVE_ALL);
        if (globalObservers != null) {
            for (DataObserver<DataType> observer : globalObservers) {
                observer.dataChangedEvent(data);
            }
        }
    }

    /**
     * Notifies all observers that a change has occurred in an object
     *
     * @param id ID of the object of interest
     * @throws SQLException if error getting entity by id from database
     */
    public void notifyObservers(int id) throws SQLException {
        DataType data = getEntity(id);
        if (data == null) {
            return;
        }
        notifyObservers(data);
    }

    /**
     * Notifies all observers that a change has occurred in an object
     *
     * @param data data object to notify
     */
    public void notifyObservers(DataType data) {
        checkIdObserverMethods(data.getId());
        notifyGlobalObservers(data);
        HashSet<DataObserver<DataType>> globalObservers = observers.get(OBSERVE_ALL);

        if (observers.containsKey(data.getId())) {
            for (DataObserver<DataType> observer : observers.get(data.getId())) {
                if (globalObservers == null || !globalObservers.contains(observer)) {
                    // If both globally observing and observing the specific object, don't call it twice
                    observer.dataChangedEvent(data);
                }
            }
        }
    }

    /**
     * Notifies observers that the object was deleted from the database
     *
     * @param id ID of the data object (before it was deleted). Can be OBSERVE_ALL
     */
    public void notifyObserversOfDeletion(int id) {
        checkIdObserverMethods(id);

        HashSet<DataObserver<DataType>> globalObservers = observers.get(OBSERVE_ALL);

        if (globalObservers != null) {
            for (DataObserver<DataType> observer : globalObservers) {
                observer.dataChangedEvent(null);
                // Passing null on deletion event for global observer
            }
        }
    }


    /**
     * Method which tries to close a statement
     *
     * @param statement prepared statement to close, or null
     */
    protected void tryClose(PreparedStatement statement) {
        try {
            if (statement != null && !statement.isClosed()) {
                statement.close();
            }
        } catch (SQLException throwables) {
            AlertHelper.showErrorAlert(throwables, "An error occurred closing a prepared statement");
        }
    }

}
