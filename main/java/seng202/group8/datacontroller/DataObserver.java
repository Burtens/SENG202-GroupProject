package seng202.group8.datacontroller;

import seng202.group8.data.Data;

/**
 * Interface that observers to Data objects should follow. Events are triggered on data modification and deletion
 * <p>
 * If the observer subscribes to OBSERVE_ALL, all events from this will go to `dataChangedEvent` for insertions, modifications and deletions (with the last receiving NULL)
 */
public interface DataObserver<DataType extends Data> {

    /**
     * Method that is called when a data object the observer has subscribed to is modified OR the subscriber is a global subscriber and a data object has been changed/added/deleted
     *
     * @param data data object that was modified or NULL if the observer has subscribed to OBSERVE_ALL and an object was deleted
     */
    void dataChangedEvent(DataType data);
}
