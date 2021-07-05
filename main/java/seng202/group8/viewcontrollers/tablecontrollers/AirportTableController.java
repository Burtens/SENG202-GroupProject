package seng202.group8.viewcontrollers.tablecontrollers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import seng202.group8.data.Airport;
import seng202.group8.datacontroller.AirportDataController;
import seng202.group8.datacontroller.DataController;
import seng202.group8.viewcontrollers.DataViewController;

import java.sql.SQLException;

/**
 * Controller that controls the viewing and editing of airport data. This is the controller for airportTable.fxml
 **/
public class AirportTableController extends TableController<Airport> {

    @FXML
    private TableView<Airport> airportTable;

    @FXML
    private TableColumn<Airport, String> nameColumn;

    @FXML
    private TableColumn<Airport, String> cityColumn;

    @FXML
    private TableColumn<Airport, String> countryColumn;

    @FXML
    private TableColumn<Airport, String> codeColumn;

    @FXML
    private TableColumn<Airport, Integer> altitudeColumn;

    @FXML
    private TableColumn<Airport, String> timezoneColumn;

    @FXML
    private TableColumn<Airport, Integer> totalRoutesColumn;

    /**
     * Constructs the AirportTableController. Will add itself as an observer to both the AirportDataController and the global Filters
     *
     * @param dataViewController The DataViewController that owns this AirportTableController
     */
    public AirportTableController(DataViewController dataViewController) {
        super(dataViewController);
    }


    /**
     * Initializes table with observers to get Airport object data when it is added to table.
     **/
    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        countryColumn.setCellValueFactory(new PropertyValueFactory<>("country"));
        codeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCode()));
        altitudeColumn.setCellValueFactory(new PropertyValueFactory<>("altitude"));
        timezoneColumn.setCellValueFactory(cellData -> {
                    if (cellData.getValue().getTimezone() >= 0) {
                        return new SimpleStringProperty("+" + cellData.getValue().getTimezone());
                    }
                    else {
                        return new SimpleStringProperty(String.valueOf(cellData.getValue().getTimezone()));
                    }
        });

        //Should get total routes for this airport
        totalRoutesColumn.setCellValueFactory(airport -> {
            try {
                //Converts int into a ObservableValue<Integer>
                return new SimpleIntegerProperty(AirportDataController.getSingleton().getTotalRoutes(airport.getValue().getCode())).asObject();
            } catch (SQLException | NullPointerException throwables) {
                //If there is an issue just set value to -1
                return new SimpleIntegerProperty(-1).asObject();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void getAndObserveDataController() {
        dataController = AirportDataController.getSingleton();
        dataController.addObserver(DataController.OBSERVE_ALL, this);
    }

    /**
     * {@inheritDoc}
     */
    //When tab is selected table is updated with the first 50 airports stored in the database.
    public void update(int numRows, boolean isLoading, char event) {
        super.update(numRows, isLoading, event);
        airportTable.setItems(currItems);
    }

    /**
     * {@inheritDoc}
     */
    @FXML
    public void viewRow() {
        if (airportTable.getSelectionModel().getSelectedItem() != null) {
            Airport selectedAirport = airportTable.getSelectionModel().getSelectedItem();
            detailRootController.setDetailViewObject(selectedAirport);
        }

    }


}
