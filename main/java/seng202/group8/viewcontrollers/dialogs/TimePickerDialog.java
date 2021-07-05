package seng202.group8.viewcontrollers.dialogs;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import seng202.group8.AlertHelper;
import seng202.group8.datacontroller.DataConstraintsException;

import java.io.IOException;
import java.util.Optional;

/**
 * A JavaFX Dialog that prompts the user to enter a 24 hour UTC time.
 */
public class TimePickerDialog extends Dialog<Integer> {

    @FXML
    private TextField hoursField;

    @FXML
    private TextField minsField;

    private Integer calculateTakeoffTime() throws DataConstraintsException {
        Integer hours = Integer.parseInt(hoursField.getText());
        Integer minutes = Integer.parseInt(minsField.getText());
        if (hours >= 24 || hours < 0 || minutes >= 60 || minutes < 0) {
            throw new DataConstraintsException("time", "You did not enter in a valid time");
        }
        return Integer.parseInt(hoursField.getText()) * 60 + Integer.parseInt(minsField.getText());
    }

    private TimePickerDialog() {
        setResultConverter(dialogButton -> {
            if (dialogButton.equals(ButtonType.OK)) {
                try {
                    return calculateTakeoffTime();
                } catch (NumberFormatException | DataConstraintsException e) {
                    AlertHelper.showGenericErrorAlert(null, false,
                        "Time Error",
                        "You have not entered a valid time",
                        "The time must be positive and be less than 24 hours",
                        null
                    );
                    return null;
                }
            }
            return null;
        });

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/seng202/group8/timePickerDialog.fxml"));
            loader.setController(this);
            DialogPane dialogPane = loader.load();
            this.setDialogPane(dialogPane);
        } catch (IOException exception) {
            AlertHelper.showErrorAlertIOErrorLoadingFXML(exception, "The time picker file could not be loaded");
        }
    }

    /**
     * Pops up a time picker dialog and waits for the user to enter a time. Will then return the entered date.
     *
     * @return The time in minutes entered into the dialog if successful, or null if the dialog was cancelled
     */
    public static Integer showAndGetTime() {
        TimePickerDialog timePicker = new TimePickerDialog();

        try {
            Optional<Integer> result = timePicker.showAndWait();
            return result.orElse(null); // Return the entered time if present, otherwise return null
        } catch (NumberFormatException e) { // Invalid times entered
            AlertHelper.showGenericErrorAlert(null, false,
                "Time Error",
                "You have not entered a valid time",
                null,
                null
            );
            return null;
        }

    }
}
