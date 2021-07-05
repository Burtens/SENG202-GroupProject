package seng202.group8.viewcontrollers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import seng202.group8.data.Airline;
import seng202.group8.data.Airport;
import seng202.group8.data.Route;
import seng202.group8.io.SortOrder;
import seng202.group8.viewcontrollers.detailcontrollers.DetailRootController;
import seng202.group8.viewcontrollers.tablecontrollers.AirlineTableController;
import seng202.group8.viewcontrollers.tablecontrollers.AirportTableController;
import seng202.group8.viewcontrollers.tablecontrollers.RouteTableController;
import seng202.group8.viewcontrollers.tablecontrollers.TableController;

import java.io.IOException;
import java.util.Arrays;

/**
 * Controller for the parent data view pane. The data view pane is the pane that contains all data views
 * (ie. airport, route, and airline tables, as well as the map view). Is responsible for handling data tab selection
 * event, and the controls for the tables (sorting, prev/next buttons and so on).
 */
public class DataViewController {

    private RootController rootController;
    private TableController<Airport> airportTableController;
    private TableController<Airline> airlineTableController;
    private TableController<Route> routeTableController;
    private static TableController<?> currentTableController;
    private FiltersViewController filters;
    private DetailRootController detailRootController;
    private MapViewController mapViewController;

    @FXML
    private TabPane dataTabPane;
    //Fix to allow me to interact with the TabPane when controller initialises.
    private static TabPane staticDataTabPane;

    @FXML
    private Tab routeTab;

    @FXML
    private Tab airlinesTab;

    @FXML
    private Tab airportsTab;

    @FXML
    private Tab mapTab;

    @FXML
    private Pane controlPane;
    //Fix to allow me to interact with the controlPane when controller initialises.
    private static Pane staticControlPane;

    @FXML
    private Button prevDataButton;

    @FXML
    private Button nextDataButton;

    @FXML
    private Slider rowsLoadedSlider;

    @FXML
    private Button createNewButton;

    @FXML
    public ChoiceBox<String> sortOrderBox;

    @FXML
    public ChoiceBox<String> sortColumnBox;

    private static Slider staticRowsLoadedSlider;

    private static Tab currentTab;

    /**
     * Constructs the DataViewController.
     * @param filters The filters view controller, will be called to show the specific filters when the tab is switched
     * @param detailRootController The details root controller, will be called to show the correct details pane when the tab is switched
     * @param rootController The root controller of the program, will be called to show/hide the filters depending if the map pane is open
     */
    public DataViewController(FiltersViewController filters, DetailRootController detailRootController, RootController rootController) {
        this.rootController = rootController;
        this.filters = filters;
        this.detailRootController = detailRootController;
    }

    /**
     * Initializes the tabController using FXML loaders.
     *
     * @throws IOException If the FXML could not be loaded
     * @see FXMLLoader
     **/
    @FXML
    private void initialize() throws IOException {

        //Setting up sorting column boxes
        sortOrderBox.setItems(FXCollections.observableList(Arrays.asList("None", "Ascending", "Descending")));
        sortOrderBox.setValue("None");
        sortOrderBox.valueProperty().addListener((observableValue, s, t1) -> setSort());
        sortColumnBox.valueProperty().addListener((observableValue, s, t1) -> setSort());


        //Setup TableViews and get controllers for each table
        FXMLLoader routeTableLoader = new FXMLLoader(getClass().getResource(
                "/seng202/group8/routeTable.fxml"));
        routeTableLoader.setControllerFactory(c -> {
            return new RouteTableController(this); // Pass in self as argument to TableController constructor
        });
        TableView<Route> routeTableView = routeTableLoader.load();
        routeTableController = routeTableLoader.getController();
        routeTableController.setDetailRootController(detailRootController);
        
        /* Current Table Controller should be initialized to the first Tab when program opens this is to avoid null
        pointer exceptions on initialize. At the current time this is the routes table*/
        currentTableController = routeTableController;

        FXMLLoader airlineTableLoader = new FXMLLoader(getClass().getResource(
                "/seng202/group8/airlineTable.fxml"));
        airlineTableLoader.setControllerFactory(c -> {
            return new AirlineTableController(this); // Pass in self as argument to TableController constructor
        });
        TableView<Airline> airlineTableView = airlineTableLoader.load();
        airlineTableController = airlineTableLoader.getController();
        airlineTableController.setDetailRootController(detailRootController);

        FXMLLoader airportTableLoader = new FXMLLoader(getClass().getResource(
                "/seng202/group8/airportTable.fxml"));
        airportTableLoader.setControllerFactory(c -> {
            return new AirportTableController(this);    // Pass in self as argument to TableController constructor
        });
        TableView<Airport> airportTableView = airportTableLoader.load();
        airportTableController = airportTableLoader.getController();
        airportTableController.setDetailRootController(detailRootController);

        FXMLLoader mapViewLoader = new FXMLLoader(getClass().getResource(
                "/seng202/group8/mapView.fxml"));
        Pane mapView = mapViewLoader.load();
        mapViewController = mapViewLoader.getController();


        //Load Views into Root pane (this)
        routeTab.setContent(routeTableView);
        airlinesTab.setContent(airlineTableView);
        airportsTab.setContent(airportTableView);
        mapTab.setContent(mapView);

        //Sets objects I want to interact with as static to allow me to interact with them in static method
        // setTableViewEnabled. This is to get around JavaFX's null pointer exception when initializing nodes.
        staticControlPane = controlPane;
        staticDataTabPane = dataTabPane;
        staticRowsLoadedSlider = rowsLoadedSlider;
        //Initializes the current Table controller
        currentTableController = routeTableController;
        tabSelected();
    }

    /**
     * Is called when a new tab is selected. Updates tableViews, currentTableController and current filters based on tab selected.
     * Error in how scene is built. This runs before scene is initialised so have to check for null pointers.
     */
    @FXML
    private void tabSelected() throws IOException {
        //Stops null pointer error when scene is initially built.
        if (currentTableController != null) {
            sortOrderBox.setValue("None");

            //Stops tabSelected from triggering twice by checking what the previous tab was
            if (currentTab != dataTabPane.getSelectionModel().getSelectedItem()) {
                currentTab = dataTabPane.getSelectionModel().getSelectedItem();

                controlPane.setVisible(true);

                rootController.showFilters();
                dataTabPane.setPrefWidth(600);
                switch (currentTab.getId()) {
                    case "routeTab":
                        filters.showFlightFilters();
                        detailRootController.setDetailsContentType("route");
                        currentTableController = routeTableController;
                        sortColumnBox.setItems(FXCollections.observableList(Arrays.asList("Airline", "Source", "Destination", "Duration", "Price")));
                        sortColumnBox.setValue("Airline");
                        createNewButton.setText("+ New Route");
                        loadData();
                        break;
                    case "airlinesTab":
                        filters.showAirlineFilters();
                        detailRootController.setDetailsContentType("airline");
                        currentTableController = airlineTableController;
                        sortColumnBox.setItems(FXCollections.observableList(Arrays.asList("Name", "Callsign", "Code", "Country")));
                        sortColumnBox.setValue("Name");
                        createNewButton.setText("+ New Airline");
                        loadData();
                        break;
                    case "airportsTab":
                        detailRootController.setDetailsContentType("airport");
                        filters.showAirportsFilters();
                        currentTableController = airportTableController;
                        sortColumnBox.setItems(FXCollections.observableList(Arrays.asList("Name", "City", "Country", "Code", "Altitude", "Timezone", "Routes")));
                        sortColumnBox.setValue("Name");
                        createNewButton.setText("+ New Airport");
                        loadData();
                        break;
                    case "mapTab":
                        rootController.hideFilters();
                        dataTabPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
                        mapViewController.refreshMap();
                        filters.showMapFilters();
                        controlPane.setVisible(false);
                        break;
                }

            }
        }
    }

    /**
     * Selects the tab in the tab pane that has the given Id. If there is no tab with the given id, no tab will be selected.
     * @param tabId The id of the tab to be selected
     */
    public void setTab(String tabId) {
        for (Tab tab : dataTabPane.getTabs()) {
            if (tab.getId().equals(tabId)) {
                dataTabPane.getSelectionModel().select(tab);
            }
        }
    }

    /**
     * Sets the state of the control (prev/next) buttons.
     * Value of 1 means prev button disabled, 2 means next button disabled, and 3 means both buttons enabled.
     * @param disableButtonVal value for disabling switch
     * @param controller Table Controller
     */
    public void setControlButtonsEnabled(int disableButtonVal, TableController controller) {
        if (controller == currentTableController) {
            switch (disableButtonVal) {
                case 1:
                    prevDataButton.setDisable(true);
                    nextDataButton.setDisable(false);
                    break;
                case 2:
                    prevDataButton.setDisable(false);
                    nextDataButton.setDisable(true);
                    break;
                case 3:
                    nextDataButton.setDisable(true);
                    prevDataButton.setDisable(true);
                    break;
                default:
                    nextDataButton.setDisable(false);
                    prevDataButton.setDisable(false);
                    break;

            }
        }
    }

    /**
     * Gets the number of rows currently set in the row slider
     *
     * @return the number of rows currently set in the row slider
     */
    public static double getRowsSliderValue() {
        return staticRowsLoadedSlider.getValue();
    }

    /**
     * Triggers when the 'Next' button is pressed.
     * Calls update on the current tableController allowing the user to see the next chosen amount of objects
     * from the database.
     */
    @FXML
    private void nextData() {
        currentTableController.update((int) staticRowsLoadedSlider.getValue(), false, 'N');
    }

    /**
     * Triggers when the 'Prev' button is pressed.
     * Calls update on the current tableController allowing the user to see the previous chosen amount of objects
     * from the database.
     */
    @FXML
    private void prevData() {
        currentTableController.update((int) staticRowsLoadedSlider.getValue(), false, 'P');
    }

    /**
     * Reloads or loads current Tableview with data.
     * Is also used by "sort" button to load sorted data when clicked.
     */
    @FXML
    public void loadData() {
        currentTableController.update((int) staticRowsLoadedSlider.getValue(), true, 'S');
    }


    /**
     * Disables and Enables the current TableView based on users actions on the gui.
     * In most cases this will be when the user wants to edit data. This stops the user from
     * selecting new data when editing data.
     */
    public static void setTableViewEnabled() {
        //Pane Disabled
        if (staticDataTabPane.isDisabled()) {
            staticControlPane.setDisable(false);
            staticDataTabPane.setDisable(false);
        }
        //Pane Enabled
        else {
            staticControlPane.setDisable(true);
            staticDataTabPane.setDisable(true);
        }

    }

    /**
     * When user selects sort order will call the current tableControllers Sort method which sets sort order and column
     * to sort by.
     */
    @FXML
    public void setSort() {
        sortColumnBox.setDisable(false);
        //Sets sort column
        currentTableController.setSortColumn(sortColumnBox.getValue());
        //Sets sort order based on selection
        switch (sortOrderBox.getValue()) {
            case "Ascending":
                currentTableController.setSortOrder(SortOrder.ASCENDING);
                break;
            case "Descending":
                currentTableController.setSortOrder(SortOrder.DESCENDING);
                break;
            case "None":
                currentTableController.setSortOrder(null);
                sortColumnBox.setDisable(true);
        }
    }

    /**
     * Triggered when the 'Create New' button is pressed. Will call the relevant details view controller to
     * start the process of creating a new entity for the selected table.
     */
    @FXML
    public void createNewButtonPressed() {
        detailRootController.startCreateNew();
    }
}
