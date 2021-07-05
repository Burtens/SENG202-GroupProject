package seng202.group8.viewcontrollers.detailcontrollers;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import seng202.group8.AlertHelper;
import seng202.group8.data.Data;
import seng202.group8.datacontroller.DataConstraintsException;
import seng202.group8.datacontroller.DataController;
import seng202.group8.io.ConstraintsError;

import java.sql.SQLException;

/**
 * This abstract class controls a single details pane, allowing selected objects' details to be displayed
 * and possibly edited. This is responsible for handling updates and modifications of the selected entity.
 */
public abstract class DetailViewController<DataType extends Data> {
    protected DataType selectedItem;
    protected DataController<DataType> dataController;

    protected boolean isCreatingNew = false;  // Flag to store whether the details view is currently being used to create a new item

    @FXML
    StackPane mainPane;
    //FXML Components for Edit Box
    @FXML
    VBox editBox;
    //FXML Components for View Box
    @FXML
    VBox viewBox;

    /**
     * Sets the data entity to be displayed in the details pane that this class controls.
     * Called when an entry in a data view is selected
     *
     * @param selectedItem The newly selected item to be displayed
     */
    public void setSelectedItem(DataType selectedItem) {
        this.selectedItem = selectedItem;
        mainPane.setVisible(true);
    }

    /**
     * Hides the selected item in the view by making the view invisible.
     */
    public void hideSelectedItem() {
        mainPane.setVisible(false);
    }

    /**
     * Method used to load an object into the detail view. Will initialise all aspects of the
     * detail view for the user to see.
     */
    public abstract void load();

    /**
     * Clears all of the fields in the editBox. Used when creating a new item as all fields should be empty
     */
    public abstract void clearAllEditFields();

    /**
     * Fills in all of the fields in the editBox with the attributes of the selected item
     */
    public abstract void populateAllEditFields();

    /**
     * Attempts to update the currently select item's attributes with the values the user has entered into the edit fields.
     * Returns a DataConstraintsException containing all the constraints that were violated when attempting to update the item's attributes
     *
     * @return a DataConstraintsException containing all the constraints that were violated when attempting to update the item's attributes
     */
    public abstract DataConstraintsException updateSelectedItem();

    /**
     * Attempts to create a new item with the values the user has entered into the edit fields.
     * Return a DataConstraintsException containing all the constraints that were violated when attempting to create the new item
     *
     * @return a DataConstraintsException containing all the constraints that were violated when attempting to create the new item
     */
    public abstract DataConstraintsException createItem();

    /**
     * Method called when user clicks the new item button.
     * Loads a blank edit screen, and begins the process of creating a new item.
     */
    public void startCreateNew() {
        isCreatingNew = true;
        clearAllEditFields();
        mainPane.setVisible(true);
        viewBox.setVisible(false);
        editBox.setVisible(true);
    }

    /**
     * Method called when use clicks the edit button, will load edit screen.
     * This will also load initial data into the edit fields.
     */
    public void startEdit() {
        populateAllEditFields();
        viewBox.setVisible(false);
        editBox.setVisible(true);
    }

    /**
     * Method is called when user decides they want to cancel their edit. Won't save any data.
     * Returns user back to detail screen.
     */
    public void cancelEdit() {
        // If the selected item is null then no item has been selected (the pane has probably only been used to create new entities).
        // Thus the details view should be invisible, as there are no details to display
        // Otherwise, make the details pane display the currently selected object
        viewBox.setVisible(selectedItem != null);
        editBox.setVisible(false);
        isCreatingNew = false;
    }

    /**
     * Method is called when user wants to save edited data.
     *
     * @return Returns True if the updated data was valid and if the data was saved.
     * @throws SQLException If something horribly wrong happened in the database
     */
    public boolean saveEdit() throws SQLException {
        DataType previousItem = selectedItem;   // Save the currently shown item to revert back to, in case the save fails

        DataConstraintsException e;
        if (isCreatingNew) {    // If details pane is being used to create a new item
            e = createItem();
        } else {    // If pane is being used to update and existing item's information
            e = updateSelectedItem();
        }

        if (e != null) {
            AlertHelper.showErrorAlert(e);
            return false;
        } else {
            try {
                selectedItem = dataController.save(selectedItem);
            } catch (ConstraintsError constraintsError) {
                AlertHelper.showErrorAlert(constraintsError);
                selectedItem = previousItem;    // Revert to originally selected item;
                return false;  // Cancel the save. Return false in order to notify the caller that the save failed
            }
            viewBox.setVisible(true);
            editBox.setVisible(false);
            load();
            isCreatingNew = false;
            return true;
        }
    }

    /**
     * This method deletes the item displayed in the detail controller and clears the view
     *
     * @throws SQLException If something horribly wrong happened in the database
     */
    public void deleteSelectedItem() throws SQLException {
        dataController.deleteFromDatabase(selectedItem.getId());
        selectedItem = null;
        viewBox.setVisible(false);
    }
}
