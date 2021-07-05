package seng202.group8.viewcontrollers.detailcontrollers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import seng202.group8.AlertHelper;
import seng202.group8.data.*;
import seng202.group8.datacontroller.*;
import seng202.group8.viewcontrollers.NumberSpinnerHelper;
import seng202.group8.viewcontrollers.dialogs.DatePickerDialog;
import seng202.group8.viewcontrollers.dialogs.TimePickerDialog;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * A concrete subclass of DetailController that controls a route details pane
 */
public class RouteDetailViewController extends DetailViewController<Route> {
    @FXML
    Label sourceCodeLabel;
    @FXML
    Label destinationCodeLabel;
    @FXML
    Text sourceNameLabel;
    @FXML
    Text destNameLabel;
    @FXML
    Label priceLabel;
    @FXML
    Label durationHoursLabel;
    @FXML
    Label durationMinutesLabel;
    @FXML
    Label equipmentLabel;
    @FXML
    Label airlineLabel;
    @FXML
    ListView<Integer> takeoffTimesList;
    @FXML
    Pane errorBox;
    @FXML
    Label errorText;

    @FXML
    TextField sourceCodeField;
    @FXML
    TextField destinationField;
    @FXML
    TextField airlineField;
    @FXML
    TextField equipmentField;
    @FXML
    Spinner<Integer> durationSpinner;
    @FXML
    Spinner<Integer> priceSpinner;
    @FXML
    ListView<Integer> editTakeoffTimesList;

    private static String minutesToString(Integer minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    @FXML
    private void initialize() {
        dataController = RouteDataController.getSingleton();

        // Must set value factories for spinners to use them
        durationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000000, 0, 30));
        NumberSpinnerHelper.addIntegerFormat(durationSpinner);
        priceSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 0, 50));
        NumberSpinnerHelper.addIntegerFormat(priceSpinner);

        // Custom cell factory for the takeoff times list, so that minutes in Integer is displayed as HH:MM
        takeoffTimesList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    setText(RouteDetailViewController.minutesToString(item));
                } else {
                    setText(null);
                }
            }
        });
        editTakeoffTimesList.setCellFactory(takeoffTimesList.getCellFactory()); // The two lists should have the same cell factory
    }

    /**
     * Triggered when the add takeoff time button is pressed. Opens a dialog for the user to enter a time, and, if
     * successful, will add the entered time to the route
     */
    @FXML
    private void addTakeoffTime() {
        Integer minutes = TimePickerDialog.showAndGetTime();
        if (minutes != null) {
            if (editTakeoffTimesList.getItems().contains(minutes)) {    // Route takeoff times already includes this time
                AlertHelper.showGenericErrorAlert(null, false,
                    "Invalid takeoff time",
                    "The route already contains the same takeoff time",
                    null,
                    null
                );
            } else {
                editTakeoffTimesList.getItems().add(minutes);
            }
        }
    }

    /**
     * Triggered when the delete takeoff time button is pressed. Will delete the takeoff time selected by the user,
     * or do nothing if there is no takeoff time selected.
     */
    @FXML
    private void deleteSelectedTime() {
        Integer selectedItem = editTakeoffTimesList.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            editTakeoffTimesList.getItems().remove(selectedItem);
        }
    }

    /**
     * Triggered when the add to trip button is pressed. If a takeoff time has been selected, will open a calendar dialog
     * and add the flight with the time and date entered. If no takeoff time has been selected, an error popup appears.
     */
    @FXML
    private void addToTrip() {
        TripDataController tripDC = TripDataController.getSingleton();
        Trip trip = tripDC.getCurrentlyOpenTrip();
        if (trip == null) {
            AlertHelper.showGenericErrorAlert(null, false,
                "Trip Error",
                "No trip is currently open",
                "To open a trip, go to the trip tab and click the 'Load Trip' button",
                null
            );

            return;
        }
        Integer flightTime = takeoffTimesList.getSelectionModel().getSelectedItem();

        if (flightTime == null) {
            AlertHelper.showGenericErrorAlert(null, false,
                    "Trip Error",
                    "No takeoff time selected",
                    "Adding a flight to a trip requires the takeoff time to be selected",
                    null
            );
        } else {
            LocalDate date = DatePickerDialog.showAndGetDate();
            if (date != null) {
                try {
                    TripFlight newFlight = new TripFlight(selectedItem.getSourceAirportCode(), selectedItem.getDestinationAirportCode(), selectedItem.getAirlineCode(), flightTime, date, "");
                    // Check if flight can be added
                    String flightClashResult = tripDC.canAddFlightWithoutClash(trip.getFlights(), newFlight);
                    if (flightClashResult == null) {
                        // Can't rely on object since that might not be up to date
                        trip.addFlight(newFlight);
                        tripDC.save(trip); // This updates currentlySelectedTrip automatically

                        AlertHelper.showGenericAlert(null, false, Alert.AlertType.INFORMATION,
                            "Trip Update",
                            String.format("The flight was successfully added to the trip '%s'", trip.getName()),
                            null,
                            null
                        );
                    } else {
                        AlertHelper.showGenericErrorAlert(null, false,
                            "Trip error",
                            "Flight conflicts with an existing flight in the trip; change the date and/or takeoff time",
                            flightClashResult,
                            null
                        );
                    }
                } catch (SQLException exception) {
                    AlertHelper.showErrorAlert(exception);
                } catch (DataConstraintsException e) {
                    AlertHelper.showErrorAlert(e);
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        mainPane.setVisible(true);
        viewBox.setVisible(true);

        try {
            String errorMessages = RouteDataController.getSingleton().routeSanityCheck(selectedItem);
            if (errorMessages != null) {
                errorBox.setVisible(true);
                errorText.setText(errorMessages);
            } else {    // Hooray, no errors!
                errorBox.setVisible(false);
            }

            // Try to display the airline name, but fall back to code if airline does not exist in the DB
            Airline airline = AirlineDataController.getSingleton().getEntity(selectedItem.getAirlineCode());
            if (airline == null) {
                airlineLabel.setText(selectedItem.getAirlineCode() + " (code)");   // Airline doesn't exist, can only show airline code
            } else {
                airlineLabel.setText(airline.getName());   // Hooray, can show airline name
            }
            // Try to display the source and dest airport names. If the airports don't exist, make the text empty
            Airport source = AirportDataController.getSingleton().getEntity(selectedItem.getSourceAirportCode());
            Airport dest = AirportDataController.getSingleton().getEntity(selectedItem.getDestinationAirportCode());
            sourceNameLabel.setText((source != null) ? "From: " + source.getName() : "");
            destNameLabel.setText((dest != null) ? "To: " + dest.getName() : "");

        } catch (SQLException sqlException) {
            AlertHelper.showErrorAlert(sqlException);
        }

        sourceCodeLabel.setText(selectedItem.getSourceAirportCode());
        destinationCodeLabel.setText(selectedItem.getDestinationAirportCode());
        priceLabel.setText(String.valueOf(selectedItem.getPrice()));
        durationHoursLabel.setText(String.valueOf(selectedItem.getFlightDuration() / 60));
        durationMinutesLabel.setText(String.valueOf(selectedItem.getFlightDuration() % 60));
        equipmentLabel.setText(selectedItem.getPlaneTypesRaw());
        takeoffTimesList.setItems(FXCollections.observableArrayList(selectedItem.getTakeoffTimes()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAllEditFields() {
        sourceCodeField.setText("");
        destinationField.setText("");
        airlineField.setText("");
        equipmentField.setText("");
        durationSpinner.getValueFactory().setValue(0);
        priceSpinner.getValueFactory().setValue(0);
        editTakeoffTimesList.getItems().clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void populateAllEditFields() {
        sourceCodeField.setText(selectedItem.getSourceAirportCode());
        destinationField.setText(selectedItem.getDestinationAirportCode());
        airlineField.setText(selectedItem.getAirlineCode());
        equipmentField.setText(selectedItem.getPlaneTypesRaw());
        durationSpinner.getValueFactory().setValue(selectedItem.getFlightDuration());
        priceSpinner.getValueFactory().setValue(selectedItem.getPrice());

        // Populate edit takeoff times list
        editTakeoffTimesList.setItems(FXCollections.observableArrayList(selectedItem.getTakeoffTimes()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataConstraintsException updateSelectedItem() {
        DataConstraintsException e = null;

        e = DataConstraintsException.attempt(e, () -> selectedItem.setSourceAirportCode(sourceCodeField.getText()));
        e = DataConstraintsException.attempt(e, () -> selectedItem.setDestinationAirportCode(destinationField.getText()));
        e = DataConstraintsException.attempt(e, () -> selectedItem.setAirlineCode(airlineField.getText()));
        e = DataConstraintsException.attempt(e, () -> selectedItem.setPlaneTypes(equipmentField.getText()));
        e = DataConstraintsException.attempt(e, () -> selectedItem.setFlightDuration(durationSpinner.getValue()));
        e = DataConstraintsException.attempt(e, () -> selectedItem.setPrice(priceSpinner.getValue()));
        e = DataConstraintsException.attempt(e, () -> selectedItem.setTakeoffTimes(editTakeoffTimesList.getItems()));

        return e;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataConstraintsException createItem() {
        DataConstraintsException e = null;
        e = DataConstraintsException.attempt(e, () -> {
            selectedItem = new Route(airlineField.getText(), sourceCodeField.getText(),
                    destinationField.getText(), new String[]{equipmentField.getText()}, priceSpinner.getValue(), false,
                    durationSpinner.getValue(), editTakeoffTimesList.getItems());
        });
        return e;
    }

    /**
     * Method is called when user wants to save edited data. If a new route is being created, will check if the takeoff times list has been
     * left empty. If so, an alert will appear informing the user that the takeoff times will be randomly generated.
     *
     * @return Returns True if the updated data was valid and if the data was saved.
     * @throws SQLException If something horribly wrong happened in the database
     */
    @Override
    public boolean saveEdit() throws SQLException {
        if (isCreatingNew && editTakeoffTimesList.getItems().size() == 0) {  //If they have not entered any takeoff times for a new route

            Optional<ButtonType> result = AlertHelper.showGenericAlert(null, false, Alert.AlertType.WARNING,
                "Save Route",
                "Warning - empty takeoff times",
                "You have not entered any takeoff times. Takeoff times will be generated by our random algorithm.",
                null
            );

            if (result.get() != ButtonType.OK) {
                return false;   // Save has failed if the user did not click OK
            }
        }

        return super.saveEdit();
    }
}
