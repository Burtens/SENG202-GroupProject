package seng202.group8;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import seng202.group8.datacontroller.DataConstraintsException;
import seng202.group8.io.ConstraintsError;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Helper to generate alerts. Main benefit is text wrapping. Also has wrappers in case of exceptions
 */
public class AlertHelper {
    public static final double TEXT_WIDTH = 500;

    public static boolean isTesting = false;
    public static Integer lastExitCode = null;
    public static int ERROR_CODE_IO_ERROR_LOADING_FXML = 6;

    /**
     * Blurb about sending details of what the user was doing along with following stack trace
     */
    public static String sendReportToDevWithStacktraceString = "If you believe this is an issue with the program and would like to send a report to the developer, email details of what you were doing along with the following stack trace:";

    /**
     * Error that is generating instead of calling `System.exit` when testing
     */
    public static class SystemExitTestingWrapper extends Error {
        public int exitCode = 0;

        /**
         * Constructor for the error
         * @param exitCode exit that should have been passed to `System.exit`
         */
        public SystemExitTestingWrapper(int exitCode) {
            this.exitCode = exitCode;
        }
    }

    /**
     * Shows error alert with stack trace for an SQL exception with a generic
     * header message about restarting the program
     * If JavaFX is not initialized, it prints out the stack trace to console
     *
     * @param exception SQLException to show the user
     */
    public static void showErrorAlert(SQLException exception) {
        showErrorAlert(exception, "You may need to restart the program");
    }

    /**
     * Shows error alert with stack trace for an SQL exception.
     * If JavaFX is not initialized, it prints out the stack trace to console
     *
     * @param exception SQLException to show the user
     * @param header header text about the error
     */
    public static void showErrorAlert(SQLException exception, String header) {
        showGenericErrorAlert(
                exception,
                true,
                "A database error occurred",
                header,
                "If the error persists, the database may be corrupted.\n" +
                        sendReportToDevWithStacktraceString,
                null
        );
    }

    /**
     * Shows error alert with no stack trace for a data constraints exception
     * @param exception data constraint that failed
     */
    public static void showErrorAlert(DataConstraintsException exception) {
        String message = "";
        for (String key: exception.errors.keySet()) {
            message += String.format("%s: %s\n", key, exception.errors.get(key));
        }

        showGenericErrorAlert(
                exception,
                false,
                String.format("Invalid value%s entered", exception.errors.size() == 1? "": "s"),
                exception.errors.size() == 1? "1 field has an invalid value": String.format("%s fields haves invalid values", exception.errors.size()),
                message,
                null
        );
    }

    /**
     * Shows error alert with no stack trace for a constraints error
     * @param error constraints error, likely uniqueness violation
     */
    public static void showErrorAlert(ConstraintsError error) {
        showGenericErrorAlert(
            error,
            false,
            "Invalid values",
            "One or more fields have invalid values",
            error.getMessage(),
            null
        );
    }


    /**
     * Shows error alert with stack trace, EXITING THE PROGRAM when an IO exception occurs after
     * attempting to load an FXML file
     * @param exception IOException caused by FXMLLoader.load
     * @param header header text. May be null
     */
    public static void showErrorAlertIOErrorLoadingFXML(IOException exception, String header) {
        showGenericErrorAlert(
            exception,
            true,
            "Fatal IO Error Occurred",
            header,
            "The program will now exit. Please contact the developers with a description of what you were doing and the following stack trace",
            ERROR_CODE_IO_ERROR_LOADING_FXML
        );
    }

    /**
     * Exits with the given exit code if not testing
     * @param exitCode exit code. May be null
     */
    protected static void exitWrapper(Integer exitCode) {
        lastExitCode = exitCode;
        if (exitCode != null) {
            if (isTesting) {
                // Exiting breaks tests, so throw an error instead
                throw new SystemExitTestingWrapper(exitCode);
            } else {
                System.exit(exitCode);
            }
        }
    }


    /**
     * Shows a generic warning alert for an optional exception or error. If JavaFX is not initialized, it prints out the stack trace to console.
     *
     * @param exception exception or error to show to the user. If null, stack trace will not be generated
     * @param showStackTrace if true, shows stack trace in a text area in the alert dialog. If exception is null, this setting will be overridden
     * @param title title of the alert. If null, a generic title is generated
     * @param header header for the alert. If null, header will not be shown
     * @param text text for the alert. If null, it will not be shown
     * @param exitCode if not null, the program will exit the code with the given exit code
     */
    public static void showGenericWarningAlert(Throwable exception, boolean showStackTrace, String title, String header, String text, Integer exitCode) {
        showGenericAlert(exception, showStackTrace, Alert.AlertType.WARNING, title, header, text, exitCode);
    }

    /**
     * Shows a generic error alert for an optional exception or error. If JavaFX is not initialized, it prints out the stack trace to console.
     *
     * @param exception exception or error to show to the user. If null, stack trace will not be generated
     * @param showStackTrace if true, shows stack trace in a text area in the alert dialog. If exception is null, this setting will be overridden
     * @param title title of the alert. If null, a generic title is generated
     * @param header header for the alert. If null, header will not be shown
     * @param text text for the alert. If null, it will not be shown
     * @param exitCode if not null, the program will exit the code with the given exit code
     */
    public static void showGenericErrorAlert(Throwable exception, boolean showStackTrace, String title, String header, String text, Integer exitCode) {
        showGenericAlert(exception, showStackTrace, Alert.AlertType.ERROR, title, header, text, exitCode);
    }
    /**
     * Shows a generic alert for an optional exception or error. If JavaFX is not initialized, it prints out the stack trace to console.
     *  @param exception exception or error to show to the user. If null, stack trace will not be generated
     * @param showStackTrace if true, shows stack trace in a text area in the alert dialog. If exception is null, this setting will be overridden
     * @param alertType type of alert
     * @param title title of the alert. If null, a generic title is generated
     * @param header header for the alert. If null, header will not be shown
     * @param text text for the alert. If null, it will not be shown
     * @param exitCode if not null, the program will exit the code with the given exit code
     * @return optional button type that was clicked
     */
    public static Optional<ButtonType> showGenericAlert(Throwable exception, boolean showStackTrace, Alert.AlertType alertType, String title, String header, String text, Integer exitCode) {
        if (title == null) {
            if (exception == null) title = "An error occurred";
            else title = String.format("A '%s' error occurred", exception.getClass().getName());
        }

        try {
            Alert alert = new Alert(alertType);
        } catch(ExceptionInInitializerError | NoClassDefFoundError e) {
            // JavaFX not initialized
            if (isTesting) {
                // If testing, don't bother printing out lots of stuff
                exitWrapper(exitCode);
                return null;
            }

            exitWrapper(exitCode);
            return null;
        }

        String textAreaText = null;
        if (showStackTrace && exception != null) {
            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));
            textAreaText = stringWriter.toString();
        }

        Alert alert = generateAlertDialog(alertType, title, header, text, textAreaText);

        Optional<ButtonType> result = alert.showAndWait();

        exitWrapper(exitCode);

        return result;
    }

    /**
     * Generates and returns an alert with the given content. Text will be wrapped!
     * @param alertType type of alert
     * @param title title of the alert
     * @param header header for the alert. May be null
     * @param text Text for the alert. May be null
     * @param textAreaText Text for the text area below the text. May be null
     * @return alert of the specified type and with the specified content
     */
    public static Alert generateAlertDialog(Alert.AlertType alertType, String title, String header, String text, String textAreaText) {
        Alert alert = new Alert(alertType);

        alert.setTitle(title);
        alert.setHeaderText(header);

        Label label = null;
        TextArea textArea = null;
        VBox vbox = new VBox();

        if (text != null) {
            label = new Label(text);
            label.setWrapText(true);

            label.setMaxWidth(TEXT_WIDTH);
            vbox.getChildren().add(label);
        }

        if (textAreaText != null) {
            textArea = new TextArea();
            textArea.setWrapText(true);
            textArea.setText(textAreaText);

            vbox.getChildren().add(textArea);
        }

        alert.getDialogPane().setContent(vbox);
        return alert;
    }
}