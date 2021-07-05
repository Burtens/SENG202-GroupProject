package seng202.group8.viewcontrollers;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.javatuples.Quartet;
import seng202.group8.AlertHelper;
import seng202.group8.Main;
import seng202.group8.data.Trip;
import seng202.group8.io.Database;
import seng202.group8.io.Import;
import seng202.group8.viewcontrollers.detailcontrollers.DetailRootController;
import seng202.group8.viewcontrollers.dialogs.CreditDialog;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Optional;

/**
 * The controller for the root app window, which contains all of the application.
 */
public class RootController extends Application {
    @FXML
    private Pane leftPane;

    @FXML
    private Pane tableViewAnchor;

    @FXML
    private TabPane detailsAndTripTabPane;

    @FXML
    private Pane detailsPane;

    @FXML
    private Pane tripPane;

    @FXML
    private Tab detailsTab;

    @FXML
    private Tab tripTab;

    private Pane filtersView;
    private FiltersViewController filterController;
    private TripViewController tripViewController;
    private DetailRootController detailRootController;
    private DataViewController dataViewController;

    public static Stage stage;

    private FileChooser fileChooser = new FileChooser();

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/seng202/group8/root.fxml"));
        primaryStage.setTitle("M.A.T.T.I.A.S");
        primaryStage.setScene(new Scene(root, 1250, 768));
//        primaryStage.getScene().getStylesheets().add(getClass().getResource("/seng202/group8/stylesheets/customFont.css").toExternalForm());
        primaryStage.getIcons().add(new Image("/seng202/group8/Photos/OtherLogo.png"));
        stage = primaryStage;
        primaryStage.show();
        Main.javaFXInitialized = true;
    }


    @FXML
    public void initialize() throws IOException {
        FXMLLoader filtersViewLoader = new FXMLLoader(getClass().getResource("/seng202/group8/filtersPane.fxml"));
        filtersView = filtersViewLoader.load();
        filterController = filtersViewLoader.getController();

        FXMLLoader detailViewLoader = new FXMLLoader(getClass().getResource("/seng202/group8/detailsPane.fxml"));
        detailViewLoader.setControllerFactory(c -> new DetailRootController(this)); // Pass in this root controller as argument to DetailRootController constructor
        SplitPane detailView = detailViewLoader.load();
        detailRootController = detailViewLoader.getController();

        FXMLLoader tripViewLoader = new FXMLLoader(getClass().getResource("/seng202/group8/tripView.fxml"));
        SplitPane tripView = tripViewLoader.load();
        tripViewController = tripViewLoader.getController();

        FXMLLoader dataViewLoader = new FXMLLoader(getClass().getResource("/seng202/group8/dataViewPane.fxml"));
        dataViewLoader.setControllerFactory(c -> new DataViewController(filterController, detailRootController, this)); // Pass in filterController as argument to DataViewController constructor;

        SplitPane tableView = dataViewLoader.load();
        dataViewController = dataViewLoader.getController();

        showFilters();
        tableViewAnchor.getChildren().add(tableView);
        detailsPane.getChildren().add(detailView);
        tripPane.getChildren().add(tripView);
    }

    @FXML
    private void showCredits() {
        CreditDialog.showCredits();
    }

    /*
     * These methods are used when use selects a file to import from menu bar
     * runs importData method with a specific string representing a dataType
     * */
    @FXML
    private void importAirline() {
        importData("Airline");
        dataViewController.setTab("airlinesTab");
    }

    @FXML
    private void importAirport() {
        importData("Airport");
        dataViewController.setTab("airportsTab");
    }

    @FXML
    private void importRoute() {
        importData("Route");
        dataViewController.setTab("routeTab");
    }

    @FXML
    private void importTrip() {
        importData("Trip");
        showTripTab();  // Make Details/Trip view automatically switch to trip tab on successful import
    }

    /**
     * Called when the user selects a dataType to import
     * Allows user to select a file that contains the specific data type to import
     *
     * @param dataType String describing data type to import
     */
    private void importData(String dataType) {
        fileChooser.getExtensionFilters().clear();

        fileChooser.setTitle(String.format("Import %s file", dataType));

        if (dataType.equals("Trip"))
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MattyG Trips", "*.mtyg"));
        else
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Any", "*"));
        File chosenFile = fileChooser.showOpenDialog(RootController.stage);

        if (chosenFile != null) {
            Trip trip = null;
            if (dataType.equals("Trip")) {
                try {
                    trip = Import.importTrip(chosenFile.toString());
                    if (trip != null) {
                        tripViewController.saveTrip(trip);
                    } else {
                        AlertHelper.showGenericErrorAlert(null, false,
                                "Import Failed",
                                "Trip Import Failed",
                                "An error occurred when attempting to import the trip.\nFile is likely corrupted or not a valid .mtyg file",
                                null
                        );
                    }
                } catch (IOException e) {
                    AlertHelper.showGenericErrorAlert(e, true,
                            "Import Failed",
                            "Trip Import Failed",
                            "An IO error occurred while attempting to import the trip. \n\n" +
                                    AlertHelper.sendReportToDevWithStacktraceString,
                            null
                    );
                }
            } else {
                // ----------- Loading bar -------------

                Dialog loadingBarDialog = new Dialog();
                loadingBarDialog.setTitle("Importing " + dataType + "s");
                GridPane gridPane = new GridPane();
                gridPane.setPadding(new Insets(20, 20, 20, 20));
                DoubleProperty progress = new SimpleDoubleProperty();
                Label progressLabel = new Label("Importing " + dataType + "s...");
                ProgressBar progressBar = new ProgressBar(0);
                progressBar.progressProperty().bind(progress);
                ProgressIndicator progressIndicator = new ProgressIndicator(0);
                progressIndicator.progressProperty().bind(progress);
                gridPane.add(progressLabel, 0, 0);
                gridPane.add(progressBar, 0, 1);
                gridPane.add(progressIndicator, 1, 1);
                loadingBarDialog.getDialogPane().setContent(gridPane);

                loadingBarDialog.show();

                // -------------------------------------
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        Quartet<Integer, Integer, Long, String> importInfo = Import.importData(chosenFile.toString(), dataType, progress);
                        int numRows = importInfo.getValue0();
                        int numFailed = importInfo.getValue1();
                        long durationMilliseconds = importInfo.getValue2();
                        String errorMessage = importInfo.getValue3();
                        Platform.runLater(() -> {
                            Alert alert = AlertHelper.generateAlertDialog(Alert.AlertType.INFORMATION,
                                    String.format("%s Import", dataType),
                                    String.format("Imported %d of out %d rows (%d failure%s) in %d ms",
                                            numRows - numFailed,
                                            numRows,
                                            numFailed,
                                            numFailed == 1 ? "" : "s",
                                            durationMilliseconds
                                    ),
                                    null,
                                    errorMessage
                            );
                            // JavaFX, in its infinite wisdom, makes it literally impossible to close a dialog unless it has a button in it.
                            loadingBarDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                            loadingBarDialog.close();

                            alert.showAndWait();
                        });
                        return null;
                    }
                };
                Thread thread = new Thread(task);
                thread.start();
            }
        }
        fileChooser.getExtensionFilters().clear();
    }

    @FXML
    public void createDatabase() {
        changeDatabase(true);
    }

    @FXML
    public void openDatabase() {
        changeDatabase(false);
    }

    /**
     * Changes the database and reloads the views
     *
     * @param create if true, shows save dialog to create new DB file; else, shows open dialog to open existing DB file
     */
    public void changeDatabase(boolean create) {
        fileChooser.setTitle(create ? "Create Database" : "Open Database");
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DB", "*.db"));
        if (!create) fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Any", "*")
        );
        //fileChooser.setInitialDirectory(new File(FilenameUtils.getPath(Database.getDatabasePath().getPath())));

        File chosenFile = create ? fileChooser.showSaveDialog(RootController.stage) : fileChooser.showOpenDialog(RootController.stage);
        if (chosenFile != null) {
            try {
                String path = chosenFile.getCanonicalPath();
                URI uri = chosenFile.toURI();
                boolean fileExists = false;
                if (create) {// && FilenameUtils.getExtension(path).length() == 0) {
                    // No extension; add .db. But if opening DB, don't touch it
//                    uri = Path.of(path + ".db").toUri();
                    fileExists = new File(uri).exists();
                    if (fileExists) {
                        Alert alert = AlertHelper.generateAlertDialog(Alert.AlertType.WARNING,
                            "Database Warning",
                            "Data loss may occur",
                            String.format("If you continue, the file '%s' will be overridden and replaced with a new database", Paths.get(uri).getFileName().toString()),
                            null
                        );
                        alert.getButtonTypes().add(ButtonType.CANCEL);
                        Optional<ButtonType> clicked = alert.showAndWait();
                        if (!clicked.isPresent() || clicked.get() != ButtonType.OK) {
                            // If okay not clicked cancel
                            return;
                        }
                    }
                }

                if (create && fileExists) {
                    // Delete existing file so it is overridden instead of opening it
                    if (!new File(uri).delete()) {
                        // File not deleted
                        AlertHelper.showGenericErrorAlert(null, false,
                                "Database Error",
                                "Could overwrite the selected file",
                                "Could not delete the existing file. Database not created\n\n" +
                                "Try renaming or deleting the file manually before trying again",
                                null
                        );
                        return;
                    }
                }

                Database.setDatabasePath(uri);
                detailRootController.clearDetailViewObject();   // Clear the detail view so it won't display an old object that's not in the new database
            } catch (IOException | SQLException e) {
                AlertHelper.showGenericErrorAlert(e, true,
                        "Database Error",
                        "Could not open the selected database",
                        "The database may be corrupted or may not be a database generated by this program\n\n"
                            + AlertHelper.sendReportToDevWithStacktraceString,
                        null
                );
            }
        }
    }

    /**
     * Hides the filters pane. This will create free space in the filters portion of the root SplitPane
     * that can be taken up by another node.
     */
    public void hideFilters() {
        leftPane.getChildren().remove(filtersView);
    }

    /**
     * Shows the filters pane. This will take up all free space in the filters portion of the root SplitPane.
     */
    public void showFilters() {
        if (!leftPane.getChildren().contains(filtersView)) {
            leftPane.getChildren().add(filtersView);
        }
    }

    /**
     * Make the combined details/trip tab view to switch to the details tab. This ensures that the details view can be
     * seen even if the user is currently in the trip tab.
     */
    public void showDetailsTab() {
        detailsAndTripTabPane.getSelectionModel().select(detailsTab);
    }

    /**
     * Make the combined details/trip tab view to switch to the trip tab. This ensures that the trip view can be
     * seen even if the user is currently in the details tab.
     */
    public void showTripTab() {
        detailsAndTripTabPane.getSelectionModel().select(tripTab);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
