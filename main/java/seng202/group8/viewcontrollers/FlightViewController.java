package seng202.group8.viewcontrollers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import seng202.group8.AlertHelper;
import seng202.group8.data.Airline;
import seng202.group8.data.Airport;
import seng202.group8.data.TripFlight;
import seng202.group8.datacontroller.AirlineDataController;
import seng202.group8.datacontroller.AirportDataController;
import seng202.group8.datacontroller.TripDataController;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A JavaFX component and controller using the JavaFX custom control pattern. Displays a pane containing information
 * about a flight, with ability to display warning messages in a popout.
 * See https://stackoverflow.com/questions/40911450/javafx-defining-custom-controls
 */
public class FlightViewController extends VBox {
    private TripFlight flight;
    private TripViewController ownerTrip;
    private int flightSequenceNumber;
    private Boolean errorTextSticky = false;

    @FXML
    VBox root;
    @FXML
    Text startText;
    @FXML
    Text destText;
    @FXML
    Text priceText;
    @FXML
    Text airlineText;
    @FXML
    Text startDateTime;
    @FXML
    Text endDateTime;
    @FXML
    Text startAirportNameText;
    @FXML
    Text endAirportNameText;
    @FXML
    Text flightNumberText;
    @FXML
    TextArea commentArea;
    @FXML
    Text warningText;
    @FXML
    Pane errorTextPane;
    @FXML
    Button showErrorTextButton;

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm '(UTC')");

    private void displayRouteDependentData() throws SQLException {
        ZonedDateTime landing = TripDataController.getSingleton().getUTCLandingTime(flight);
        endDateTime.setText(landing.format(dateTimeFormatter));
        priceText.setText("$" + TripDataController.getSingleton().getPrice(flight));

        // Do this after setting landing time, so if landing time fails then this doesn't run
        startDateTime.setText(flight.getUTCTakeoffDateTime().format(dateTimeFormatter));
    }

    private void displayAirportDependentData() throws SQLException {
        Airport sourceAirport = AirportDataController.getSingleton().getEntity(flight.getSourceCode());
        Airport destAirport = AirportDataController.getSingleton().getEntity(flight.getDestinationCode());
        if (sourceAirport != null) startAirportNameText.setText(sourceAirport.getName());
        if (destAirport != null) endAirportNameText.setText(destAirport.getName());
    }

    private void displayAirlineDependentData() throws SQLException {
        Airline airline = AirlineDataController.getSingleton().getEntity(flight.getAirlineCode());
        if (airline != null) airlineText.setText(airline.getName());
        else airlineText.setText("Airline Code: " + flight.getAirlineCode());
    }

    /**
     * Sets the error text that can be displayed in a popup when the user clicks a button.
     *
     * @param text The text of the error message. Null if no errors.
     */
    public void setErrorText(String text) {
        if (text == null) {
            root.setStyle(null);
            warningText.setVisible(false);
        } else {
            root.setStyle("-fx-border-color: red; -fx-border-width: 4px; -fx-background-color: mistyrose");
            warningText.setText(text);
            showErrorTextButton.setVisible(true);
        }
    }


    /**
     * Initialize the control
     */
    @FXML
    public void initialize() {
        startText.setText(flight.getSourceCode());
        destText.setText(flight.getDestinationCode());
        commentArea.setText(flight.getComment());
        flightNumberText.setText("Flight " + String.valueOf(flightSequenceNumber) + ".");
        try {
            displayRouteDependentData();
            displayAirportDependentData();
            displayAirlineDependentData();
            showErrorTextButton.setVisible(false);
        } catch (SQLException sqlException) {
            setErrorText("The route this flight references does not exist (it may have been deleted).  You may want to remove this flight.");
        }
    }

    /**
     * Handler for when the delete flight (X) button is pressed. Will delete the flight displayed from the trip that
     * contains the flight
     *
     * @throws SQLException If a serious malfunction occurred with the database
     */
    @FXML
    public void delete() throws SQLException {
        ownerTrip.deleteFlight(this, this.flight);
    }

    /**
     * Handler for when the save comment button is pressed. Will save the comment entered into the text field
     */
    @FXML
    public void saveComment() {
        flight.setComment(commentArea.getText());
        ownerTrip.saveTrip();
    }

    @FXML
    private void toggleErrorTextSticky() {
        errorTextSticky = !errorTextSticky;
        showErrorTextButton.setStyle(errorTextSticky ? "-fx-background-color: #ff5d5d": "-fx-background-color: red");
    }

    @FXML
    private void errorButtonEntered() {
        errorTextPane.setVisible(true);
    }

    @FXML
    private void errorButtonExited() {
        if (!errorTextSticky)  errorTextPane.setVisible(false);
    }

    /**
     * Creates the flight view controller and custom control.
     *
     * @param flight    The flight to be displayed
     * @param ownerTrip The trip that the displayed flight belongs to
     * @param flightSequenceNumber The 'index' (starting at 1) of the flight within its trip. For example, flightSequenceNumber = 1 if the flight is the first of the trip.
     */
    public FlightViewController(TripFlight flight, TripViewController ownerTrip, int flightSequenceNumber) {
        this.flight = flight;
        this.ownerTrip = ownerTrip;
        this.flightSequenceNumber = flightSequenceNumber;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/seng202/group8/flightView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            AlertHelper.showErrorAlertIOErrorLoadingFXML(exception, "The flight view file could not be loaded");
        }
    }
}
