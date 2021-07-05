package seng202.group8.viewcontrollers.detailcontrollers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import seng202.group8.data.Airline;
import seng202.group8.datacontroller.AirlineDataController;
import seng202.group8.datacontroller.DataConstraintsException;
import seng202.group8.io.Database;

/**
 * A concrete subclass of DetailController that controls an airline details pane
 */
public class AirlineDetailViewController extends DetailViewController<Airline> {

    //FXML Components for View Box
    @FXML
    private VBox viewBox;

    @FXML
    private Label codeLabel;

    @FXML
    private Label nameLabel;

    @FXML
    private Label callsignLabel;

    @FXML
    private Label countryLabel;

    @FXML
    private StackPane mainPane;

    @FXML
    private TextField nameEditField;

    @FXML
    private TextField IATAEditField;

    @FXML
    private TextField ICAOEditField;

    @FXML
    private TextField callsignEditField;

    @FXML
    private ChoiceBox<String> countryEditSelector;

    /**
     * Initialises the detail view, attempting to load a list of all countries from the database and giving an Alert if
     * countries could not be loaded
     */
    @FXML
    private void initialize() {
        dataController = AirlineDataController.getSingleton();  // Very important - must get the data controller before we do anything!

        countryEditSelector.setItems(FXCollections.observableArrayList(Database.getAllCountryNames()));
    }

    /**
     * {@inheritDoc}
     */
    public void load() {
        Airline item = selectedItem;
        mainPane.setVisible(true);
        viewBox.setVisible(true);

        codeLabel.setText(item.getCode());

        //Dynamically Resize Name
        //First get width of text if it was at normal font
        Text tempText = new Text(item.getName());
        tempText.setFont(new Font(nameLabel.getFont().getName(), 34));
        double textWidth = tempText.getLayoutBounds().getWidth() + 5;   // Add a little bit of margin

        double scaleFactor = 1;
        // If name is too long, resize it
        if (textWidth > mainPane.getWidth()) {
            scaleFactor = mainPane.getWidth() / textWidth;
        }
        nameLabel.setFont(new Font(nameLabel.getFont().getName(), 34 * scaleFactor));

        nameLabel.setText(item.getName());
        countryLabel.setText((item.getCountry()));
        callsignLabel.setText((item.getCallsign() == null? "No callsign": item.getCallsign()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAllEditFields() {
        nameEditField.setText("");
        IATAEditField.setText("");
        ICAOEditField.setText("");
        callsignEditField.setText("");
        countryEditSelector.getSelectionModel().clearSelection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void populateAllEditFields() {
        nameEditField.setText(selectedItem.getName());
        IATAEditField.setText(selectedItem.getIata());
        ICAOEditField.setText(selectedItem.getIcao());
        callsignEditField.setText(selectedItem.getCallsign());
        countryEditSelector.setValue(selectedItem.getCountry());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataConstraintsException updateSelectedItem() {
        Airline item = selectedItem;
        DataConstraintsException e = null;

        e = DataConstraintsException.attempt(e, () -> item.setName(nameEditField.getText()));
        e = DataConstraintsException.attempt(e, () -> item.setIata(IATAEditField.getText()));
        e = DataConstraintsException.attempt(e, () -> item.setIcao(ICAOEditField.getText()));
        e = DataConstraintsException.attempt(e, () -> item.setCallsign(callsignEditField.getText()));
        e = DataConstraintsException.attempt(e, () -> item.setCountry(countryEditSelector.getValue()));
        return e;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataConstraintsException createItem() {
        DataConstraintsException e = null;

        e = DataConstraintsException.attempt(e, () -> {
            selectedItem = new Airline(nameEditField.getText(), callsignEditField.getText(), IATAEditField.getText(),
                    ICAOEditField.getText(), countryEditSelector.getValue());
        });
        return e;
    }
}
