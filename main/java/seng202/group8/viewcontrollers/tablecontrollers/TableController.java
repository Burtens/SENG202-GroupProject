package seng202.group8.viewcontrollers.tablecontrollers;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import seng202.group8.AlertHelper;
import seng202.group8.data.Data;
import seng202.group8.data.filters.FilterChangeObserver;
import seng202.group8.datacontroller.DataController;
import seng202.group8.datacontroller.DataObserver;
import seng202.group8.datacontroller.FiltersController;
import seng202.group8.io.SortOrder;
import seng202.group8.viewcontrollers.DataViewController;
import seng202.group8.viewcontrollers.detailcontrollers.DetailRootController;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class holding methods that are commonly used across all tableControllers. Also holds the currently viewible objects
 * and sort settings for the tableview.
 */

public abstract class TableController<DataType extends Data> implements DataObserver<DataType>, FilterChangeObserver {

    protected DataController<DataType> dataController;
    protected ObservableList<DataType> currItems = FXCollections.observableArrayList(new ArrayList<>());
    protected SortOrder sortOrder = null;
    protected String sortColumn = null;
    protected int currOffset = 0;
    protected DetailRootController detailRootController;    // The controller for the details panes, we need to send selected items to this controller
    protected DataViewController dataViewController;


    /**
     * Constructs the TableController. Will add itself as an observer to both the relevant DataController and the global Filters
     *
     * @param dataViewController The DataViewController that owns this TableController
     */
    public TableController(DataViewController dataViewController) {
        getAndObserveDataController();
        FiltersController.getSingleton().addObserver(this);
        this.dataViewController = dataViewController;
    }

    /**
     * Sets sort order of current TableView
     * @param sortOrder order to be sorted
     */
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Sets sort column of current TableView
     * @param sortColumn column to be sorted
     */
    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }


    /**
     * Calls specified dataController and gets an ObservableList-DataType which contains objects to store in the TableView.
     * Objects are then loaded into the Controllers TableView object to allow user to view them.
     *
     * @param numRows   Amount of Rows User wishes to see
     * @param isLoading a boolean to identify if the table is being initially loaded
     * @param event a char representing what is happening to the table ('N' for next page, 'P' for previous page, 'S' for stay on page)
     * @see javafx.scene.control.TableView
     */
    public void update(int numRows, boolean isLoading, char event) {
        List<DataType> newItems = null;
        //A value to determine which button should be disabled (0: none, 1: prevButton, 2: nextButton, 3: Both)
        int buttonDisable = 0;

        try {
            if (event == 'N') {
                currOffset += currItems.size();
            } else if (event == 'P') {
                currOffset -= numRows;
            }

            if ((currOffset == 0 && !isLoading) || currOffset < 0 || isLoading) {
                currOffset = 0;
                buttonDisable = 1;
            }

            newItems = dataController.getSortedFilteredEntities(sortColumn, sortOrder, Math.abs(numRows) + 1, currOffset);
            if (newItems.size() < numRows + 1) {
                if (buttonDisable == 1) {
                    buttonDisable = 3;
                } else {
                    buttonDisable = 2;
                }
            } else {
                newItems.remove(newItems.size() - 1);
            }
            currItems = FXCollections.observableArrayList(newItems);
            dataViewController.setControlButtonsEnabled(buttonDisable, this);
        } catch (SQLException e) {
            //Maybe alert the user that the program was unable to get data.
            AlertHelper.showErrorAlert(e, "Unable to fetch data from the database");
        }
    }


    /**
     * Gives this table controller a reference to the detail root controller.
     * The detail root controller is needed as selected items in the table need to be sent to the details view.
     *
     * @param controller The detail root controller this table controller will be sending items to
     */
    public void setDetailRootController(DetailRootController controller) {
        this.detailRootController = controller;
    }

    /**
     * Is called when user selects an item from the TableView. Gets the currently selected item and
     * sends the data to the current detail view to allow the user to view the item.
     */
    public abstract void viewRow();

    /**
     * Gets the singleton instance of the relevant DataController, and adds itself as an observer to the DataController.
     * Should be called when the controller is initialised
     */
    public abstract void getAndObserveDataController();

    /**
     * Called when a change in the filters has applied - part of the FilterChangeObserver interface.
     * Will re-load the table, with the new filters applied.
     *
     */
    public void filterChangedEvent() {
        update((int) DataViewController.getRowsSliderValue(), true, 'S');
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dataChangedEvent(Data data) {
        update((int) DataViewController.getRowsSliderValue(), false, 'S');
    }

}
