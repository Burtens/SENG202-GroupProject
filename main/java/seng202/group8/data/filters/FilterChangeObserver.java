package seng202.group8.data.filters;

/**
 * Interface for observer pattern. Is notified when the filters' changes are applied.
 */
public interface FilterChangeObserver {
    /**
     * Notified when a change in filters is applied
     */
    void filterChangedEvent();
}
