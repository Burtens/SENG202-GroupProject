package seng202.group8.viewcontrollers.detailcontrollers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import seng202.group8.AlertHelper;
import seng202.group8.data.Data;
import seng202.group8.viewcontrollers.DataViewController;
import seng202.group8.viewcontrollers.RootController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Controller for the root details pane, ie the pane that contains all detail views as its children
 */
public class DetailRootController {
    //Content Pane For Details
    @FXML
    private AnchorPane detailsContentPane;

    @FXML
    private StackPane editControlPane;

    @FXML
    private HBox editButtonsBox;

    @FXML
    private HBox defaultButtonsBox;

    private DetailViewController<Data> currDetailViewController;

    private RootController rootController;

    /**
     * Sets the current detailView for the selected data Object
     *
     * @param detailType the type of objects the detail view is expected to show
     * @throws IOException If the FXML could not be loaded
     */
    public void setDetailsContentType(String detailType) throws IOException {
        editControlPane.setDisable(true);
        FXMLLoader contentLoader = null;
        StackPane content = null;
        //Should only be ever one child node. Removes this if it exists
        if (detailsContentPane.getChildren().size() > 0) {
            detailsContentPane.getChildren().remove(0);
        }

        //This is the default option so is always preset.
        defaultButtonsBox.setVisible(true);
        switch (detailType) {
            case "airport":
                contentLoader = new FXMLLoader(getClass().getResource("/seng202/group8/airportDetailView.fxml"));
                content = contentLoader.load();
                currDetailViewController = contentLoader.getController();
                break;
            case "airline":
                contentLoader = new FXMLLoader(getClass().getResource("/seng202/group8/airlineDetailView.fxml"));
                content = contentLoader.load();
                currDetailViewController = contentLoader.getController();
                break;
            case "route":
                contentLoader = new FXMLLoader(getClass().getResource("/seng202/group8/routeDetailView.fxml"));
                content = contentLoader.load();
                currDetailViewController = contentLoader.getController();
                break;
        }

        //Small Check if contentLoader is null. This is just in case loader doesnt load.
        if (contentLoader != null) {
            content.setVisible(false);
            detailsContentPane.getChildren().add(content);
        }
    }

    /**
     *
     */
    public void startCreateNew() {
        DataViewController.setTableViewEnabled();
        defaultButtonsBox.setVisible(false);
        editButtonsBox.setVisible(true);
        editControlPane.setDisable(false);
        currDetailViewController.startCreateNew();
    }

    /**
     * Is called when user selects "edit" button on details screen. Disables current table view to stop user from
     * selecting another object and enables and shows the edit buttons.
     * Will also call the current detailController to setup values for editing.
     */
    @FXML
    private void startEdit() {
        DataViewController.setTableViewEnabled();
        currDetailViewController.startEdit();
        defaultButtonsBox.setVisible(false);
        editButtonsBox.setVisible(true);
    }

    /**
     * Is called when user selects "cancel" button while editing data. re-enables current table view allowing the user to
     * select another data type. Also disables the edit control buttons.
     * Will also call the current detailController to restore all edited values.
     */
    @FXML
    private void cancelEdit() {
        DataViewController.setTableViewEnabled();
        defaultButtonsBox.setVisible(true);
        editButtonsBox.setVisible(false);
        currDetailViewController.cancelEdit();
    }

    /**
     * Is called when user selects "save" button while editing data.
     * If the save was valid it will re-enable current table view allowing the user to
     * select another data type and disable the edit control buttons.
     */
    @FXML
    private void saveEdit() throws SQLException {
        boolean valid = currDetailViewController.saveEdit();
        if (valid) {
            DataViewController.setTableViewEnabled();
            defaultButtonsBox.setVisible(true);
            defaultButtonsBox.setDisable(false);    // Default buttons is set disabled by default to prevent editing/deleting a null object, so enable it
            editButtonsBox.setVisible(false);
        }
    }

    /**
     * Is called when user selects "delete" button.
     * If the save was valid it will re-enable current table view allowing the user to
     * select another data type and disable the edit control buttons.
     */
    @FXML
    private void deleteSelectedItem() {
        Optional<ButtonType> result = AlertHelper.showGenericAlert(null, false, Alert.AlertType.CONFIRMATION,
            "Confirm Delete Operation",
            "Are you sure you want to delete this?",
            "This operation is unrecoverable",
            null
        );
        
        if (result.get() == ButtonType.OK) {
            try {
                currDetailViewController.deleteSelectedItem();
                defaultButtonsBox.setDisable(true); // Disable buttons so can't edit/delete an empty pane
            } catch (SQLException sqlException) {
                AlertHelper.showErrorAlert(sqlException);
            }
        }
    }

    /**
     * A method to load a selected object into the detail view
     *
     * @param item the data Object to view.
     */
    public void setDetailViewObject(Data item) {
        editControlPane.setDisable(false);
        currDetailViewController.setSelectedItem(item);
        currDetailViewController.load();
        defaultButtonsBox.setDisable(false);    // Default buttons is set disabled by default to prevent editing/deleting a null object, so enable it here
        rootController.showDetailsTab();    // Force the root controller to switch to the details tab
    }

    /**
     * Clears the detail view's selected object so that nothing is displayed in the detail view.
     * Will also disable any control buttons for the detail view to prevent editing an empty object.
     */
    public void clearDetailViewObject() {
        editControlPane.setDisable(true);
        currDetailViewController.hideSelectedItem();
        defaultButtonsBox.setDisable(true);    // Disabled to prevent editing/deleting a null object
    }

    /**
     * Constructs this detail root controller, given the root controller that 'owns' this detail controller. The root
     * controller is needed as this controller will cause the root controller to switch to the details tab whenever
     * the detail pane displays a new object.
     * @param rootController The root controller that 'owns' this detail controller
     */
    public DetailRootController(RootController rootController) {
        this.rootController = rootController;
    }

}
