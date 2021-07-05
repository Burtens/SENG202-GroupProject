package seng202.group8.viewcontrollers.dialogs;

import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;

import java.time.LocalDate;
import java.util.Optional;

/**
 * A JavaFX Dialog that prompts the user to enter a date.
 */
public class DatePickerDialog extends Dialog<LocalDate> {
    DatePicker datePicker;

    private DatePickerDialog() {
        setTitle("Enter flight date");
        setHeaderText("Choose a date for your flight");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());   // Set date so that user can't submit an empty date
        getDialogPane().setContent(datePicker);

        setResultConverter(dialogButton -> {
            if (dialogButton.equals(ButtonType.OK)) {
                return datePicker.getValue();
            }
            return null;
        });
    }

    /**
     * Pops up a date picker dialog and waits for the user to enter a date. Will then return the entered date.
     *
     * @return The date entered into the dialog if successful, or null if the dialog was cancelled
     */
    public static LocalDate showAndGetDate() {
        DatePickerDialog dialog = new DatePickerDialog();
        Optional<LocalDate> result = dialog.showAndWait();
        return result.orElse(null);
    }
}

