package seng202.group8.viewcontrollers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.apache.commons.io.FilenameUtils;
import seng202.group8.AlertHelper;
import seng202.group8.data.Trip;
import seng202.group8.data.TripFlight;
import seng202.group8.datacontroller.DataConstraintsException;
import seng202.group8.datacontroller.TripDataController;
import seng202.group8.io.ConstraintsError;
import seng202.group8.io.Export;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the trip view, which displays a currently selected trip, and can handle loading and importing trips.
 */
public class TripViewController implements TripDataController.CurrentTripObserver {
    public Button exportButton;
    public Button loadButton;
    public HBox saveLoadButtons;
    public StackPane saveLoadControlsPane;
    public Text nameText;
    public Button deleteButton;
    private final TripDataController tripDataController = TripDataController.getSingleton();
    private FileChooser fileChooser = new FileChooser();

    @FXML
    private VBox contentPane;
    @FXML
    private Text noFlightsText;

    @FXML
    /**
     * Initialises the trip view with its associated trip
     */
    public void initialize() {
        tripDataController.subscribeToCurrentTrip(this);
        noFlightsText.setVisible(false);

        fileChooser.setTitle("Export Trip");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MattyG Trips", "*.mtyg"));

        refreshTripView();
    }

    @FXML
    private void exportTrip() {
        Trip currentTrip = tripDataController.getCurrentlyOpenTrip();
        if (currentTrip == null) {
            Alert alert = AlertHelper.generateAlertDialog(
                Alert.AlertType.INFORMATION,
                "File Export Unavailable",
                "Please select a trip before exporting",
                null,
                null
            );
            alert.getButtonTypes().add(ButtonType.OK);
            alert.showAndWait();
            return;
        }

        File chosenFile = fileChooser.showSaveDialog(RootController.stage);
        if (chosenFile != null) {
            String path = null;
            try {
                path = chosenFile.getCanonicalPath();
                if (FilenameUtils.getExtension(path).length() == 0) {
                    path += ".mtyg";
                }

                Export.exportTrip(currentTrip, path);
            } catch (IOException e) {
                AlertHelper.showGenericErrorAlert(e, true,
                    "Export Error",
                    "The trip could not be exported due to an IO error",
                    AlertHelper.sendReportToDevWithStacktraceString,
                    null
                );
            }
        }
    }


    /**
     * Shows error alert that a trip with the same name already exists
     */
    private void tripWithSameNameExistsAlert() {
        AlertHelper.showGenericErrorAlert(null, false,
                "Invalid name",
                "Failed to save your trip",
                "A trip with the name already exists - you must pick a unique name for the trip",
                null
        );
    }

    /**
     * Shows error alert when a data constraint exception that is probably for name but could be for something else occurs
     * @param e constraint error when saving the trip
     */
    private void tripConstraintExceptionProbablyName(DataConstraintsException e) {
        if (e.errors.size() == 1 && e.errors.containsKey(Trip.NAME)) {
            AlertHelper.showGenericErrorAlert(null, false,
                    "Invalid name",
                    e.errors.get(Trip.NAME),
                    null,
                    null
            );
        } else {
            // In case something other than name is somehow invalid
            AlertHelper.showErrorAlert(e);
        }
    }

    @FXML
    private void loadTrip() {
        List<String> choices;
        try {
            choices = tripDataController.getAllTripNames();
        } catch (SQLException throwables) {
            AlertHelper.showGenericErrorAlert(throwables, true,
                "Database error",
                "Failed to get saved trip names",
                "An error occurred while attempting to get saved trips names\n\n" +
                AlertHelper.sendReportToDevWithStacktraceString,
                null
            );
            return;
        }
        // If there are no trips in the DB, tell the user
        if (choices.size() == 0) {
            AlertHelper.showGenericAlert(null, false, Alert.AlertType.INFORMATION,
                "Error Loading Trip",
                "There are no trips in the database",
                "A trip must be created or imported before trips can be loaded into the program",
                null
            );
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<String>(choices.get(0), choices);
        dialog.setTitle("Select Trip");
        dialog.setHeaderText("Select trip to load");

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            try {
                Trip trip = tripDataController.getEntity(result.get());
                tripDataController.setCurrentlyOpenTrip(trip);
            } catch (SQLException throwables) {
                AlertHelper.showGenericErrorAlert(throwables, true,
                    "Database error",
                    "Failed to get the trip",
                    "An error occurred while attempting to get the selected trip from the database\n\n" +
                        AlertHelper.sendReportToDevWithStacktraceString,
                    null
                );
            }
        }
    }


    /**
     * Runs the sanity check and puts the messages in the flight subviews
     *
     * @throws SQLException  If ta fatal error occurs in the database
     */
    private void sanityCheck() throws SQLException {
        List<TripDataController.WarningError> messages = tripDataController.tripSanityCheck(tripDataController.getCurrentlyOpenTrip());

        for (int i = 0; i < messages.size(); i++) {
            FlightViewController controller = (FlightViewController) contentPane.getChildren().get(i);
            if (messages.get(i) != null) {  // If there is an error/warning message for this flight
                controller.setErrorText(messages.get(i).toString());
            } else {
                controller.setErrorText(null);
            }
        }
    }

    /**
     * Saves the trip that's displayed in the trip view
     */
    @FXML
    public void saveTrip() {
        Trip trip = tripDataController.getCurrentlyOpenTrip();
        if (trip == null) {
            return;
        }

        try {
            trip = tripDataController.save(trip);
        } catch (SQLException throwables) {
            AlertHelper.showErrorAlert(throwables);
        }
        refreshTripView();

    }

    public void saveTrip(Trip newTrip) {
        while (true) {
            TextInputDialog dialog = new TextInputDialog(newTrip.getName());
            dialog.setTitle("Save Trip");
            dialog.setHeaderText("Enter a name to save the trip as");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    newTrip.setName(result.get());
                } catch (DataConstraintsException e) {
                    tripConstraintExceptionProbablyName(e);
                    continue;
                }
            } else {
                break;
            }
            try {
                newTrip = tripDataController.save(newTrip);
                tripDataController.setCurrentlyOpenTrip(newTrip);
                break;
            } catch (SQLException throwables) {
                AlertHelper.showGenericErrorAlert(throwables, true,
                    "Database Error",
                    "Failed to save your trip",
                    "An error occurred when trying to save your trip. You may need to restart the program.\n\n" +
                        AlertHelper.sendReportToDevWithStacktraceString,
                    null
                );
                break;
            } catch (ConstraintsError constraintsError) {
                tripWithSameNameExistsAlert();
            }
        }
    }


    @FXML
    private void newTrip() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Trip");
        dialog.setHeaderText("Enter a name to save the trip as");

        while (true) {
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    Trip newTrip = new Trip(result.get(), null);
                    if (tripDataController.getAllTripNames().contains(result.get())) {
                        tripWithSameNameExistsAlert();
                        continue;
                    }
                    Trip trip = tripDataController.save(newTrip);
                    tripDataController.setCurrentlyOpenTrip(trip);
                } catch (DataConstraintsException e) {
                    tripConstraintExceptionProbablyName(e);
                    continue;
                } catch (SQLException e) {
                    AlertHelper.showErrorAlert(e);
                    continue;
                }
            }
            break;
        }
    }

    /**
     * Refreshes the associated trip view. Can be called when the current trip is modified. Is also a handler for the
     * refresh trip button in the view.
     */
    @FXML
    public void refreshTripView() {
        contentPane.getChildren().clear();
        Trip trip = tripDataController.getCurrentlyOpenTrip();
        if (trip != null) {
            nameText.setText(trip.getName());
            deleteButton.setDisable(false);
            exportButton.setDisable(false);
        } else {
            nameText.setText("No trip currently open");
            deleteButton.setDisable(true);
            exportButton.setDisable(true);
            return;
        }
        // Show a message if the trip is empty or doesn't exist
        if (trip.getFlights().size() == 0) {
            noFlightsText.setVisible(true);
            contentPane.getChildren().add(noFlightsText);
            return;
        }

        for (int i = 0; i < trip.getFlights().size(); i++) {
            // Remember to add 1 to i as flight numbers are 1-indexed
            contentPane.getChildren().add(new FlightViewController(trip.getFlights().get(i), this, i + 1));
        }

        try {
            sanityCheck();
        } catch (SQLException sqlException) {
            AlertHelper.showErrorAlert(sqlException);
        }
    }

    /**
     * Deletes a flight from the trip and view
     *
     * @param flightView view (hopefully) for the flight
     * @param flight     flight to remove
     * @throws SQLException if an SQL error occurs
     */
    protected void deleteFlight(FlightViewController flightView, TripFlight flight) throws SQLException {
        contentPane.getChildren().remove(flightView);
        tripDataController.getCurrentlyOpenTrip().getFlights().remove(flight);
        saveTrip();   // Save the trip so the deletion is saved
        sanityCheck();
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void currentTripChange(Trip trip) {
        refreshTripView();
    }

    public void deleteTrip() {
        if (tripDataController.getCurrentlyOpenTrip() == null)
            return;

        // Create and show alert that asks the user for confirmation
        Alert alert = AlertHelper.generateAlertDialog(
            Alert.AlertType.CONFIRMATION,
            "Delete Trip?",
            "Are you sure you want to delete the trip '" + tripDataController.getCurrentlyOpenTrip().getName() + "'?",
            null,
            null
        );

        ButtonType yesButton = new ButtonType("Yes");
        ButtonType noButton = new ButtonType("No");
        alert.getButtonTypes().setAll(yesButton, noButton);
        Optional<ButtonType> option = alert.showAndWait();

        if (option.isPresent() && option.get() == yesButton) {
            try {
                tripDataController.deleteFromDatabase(tripDataController.getCurrentlyOpenTrip().getId());
            } catch (SQLException e) {
                AlertHelper.showErrorAlert(e);
            }
        }
    }
}
