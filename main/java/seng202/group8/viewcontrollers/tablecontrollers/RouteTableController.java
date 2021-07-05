package seng202.group8.viewcontrollers.tablecontrollers;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import seng202.group8.data.Route;
import seng202.group8.datacontroller.DataController;
import seng202.group8.datacontroller.RouteDataController;
import seng202.group8.viewcontrollers.DataViewController;


/**
 * Controller that controls the viewing and editing of Route data. This is the controller for routeTable.fxml
 **/

public class RouteTableController extends TableController<Route> {

    @FXML
    private TableView<Route> routeTable;

    @FXML
    private TableColumn<Route, String> airlineColumn;

    @FXML
    private TableColumn<Route, String> sourceColumn;

    @FXML
    private TableColumn<Route, String> destinationColumn;

    @FXML
    private TableColumn<Route, String> durationColumn;

    @FXML
    private TableColumn<Route, String> priceColumn;

    /**
     * Constructs the RouteTableController. Will add itself as an observer to both the RouteDataController and the global Filters
     *
     * @param dataViewController The DataViewController that owns this TableController
     */
    public RouteTableController(DataViewController dataViewController) {
        super(dataViewController);
    }


    /**
     * Initializes table with observers to get Route object data when it is added to table.
     **/
    @FXML
    private void initialize() {

        airlineColumn.setCellValueFactory(new PropertyValueFactory<>("airlineCode"));
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("sourceAirportCode"));
        destinationColumn.setCellValueFactory(new PropertyValueFactory<>("destinationAirportCode"));
        durationColumn.setCellValueFactory(cellData -> {
                int minutes = cellData.getValue().getFlightDuration();
                return new SimpleStringProperty(String.format("%dh %dm", minutes / 60, minutes % 60)); //Sets takes duration in minutes and formats it.
        });
        priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty("$" + cellData.getValue().getPrice()));
    }

    /**
     * {@inheritDoc}
     */
    public void getAndObserveDataController() {
        dataController = RouteDataController.getSingleton();
        dataController.addObserver(DataController.OBSERVE_ALL, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(int numRows, boolean isLoading, char event) {
        super.update(numRows, isLoading, event);
        routeTable.setItems(currItems);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void viewRow() {
        if (routeTable.getSelectionModel().getSelectedItem() != null) {
            Route selectedRoute = routeTable.getSelectionModel().getSelectedItem();
            detailRootController.setDetailViewObject(selectedRoute);
        }
    }
}

