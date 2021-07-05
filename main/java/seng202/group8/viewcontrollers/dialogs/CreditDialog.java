package seng202.group8.viewcontrollers.dialogs;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.io.IOException;

public class CreditDialog extends Dialog<Void> {
    private static CreditDialog dialog = new CreditDialog();

    private CreditDialog () {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/seng202/group8/creditsDialog.fxml"));
            loader.setController(this);
            DialogPane dialogPane = loader.load();
            this.setDialogPane(dialogPane);
            setTitle("Credits");

            // Adds a hidden close button to the dialog. This is needed in order to enable native window's X (to close) button.
            // Taken from https://stackoverflow.com/questions/32048348/javafx-scene-control-dialogr-wont-close-on-pressing-x
            getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            Node closeButton = getDialogPane().lookupButton(ButtonType.CLOSE);
            closeButton.managedProperty().bind(closeButton.visibleProperty());
            closeButton.setVisible(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showCredits() {
        dialog.showAndWait();
    }
}
