package seng202.group8.viewcontrollers.tablecontrollers;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import seng202.group8.data.Airline;
import seng202.group8.datacontroller.AirlineDataController;
import seng202.group8.datacontroller.DataController;
import seng202.group8.viewcontrollers.DataViewController;

/**
 * Controller that controls the viewing and editing of airline data. This is the controller for airlineTable.fxml
 **/

public class AirlineTableController extends TableController<Airline> {

    @FXML
    TableView<Airline> airlineTable;

    @FXML
    private TableColumn<Airline, String> nameColumn;

    @FXML
    private TableColumn<Airline, String> callsignColumn;

    @FXML
    private TableColumn<Airline, String> countryColumn;

    @FXML
    private TableColumn<Airline, String> codeColumn;


    /**
     * Constructs the AirlineTableController. Will add itself as an observer to both the AirlineDataController and the global Filters
     *
     * @param dataViewController The DataViewController that owns this AirlineTableController
     */
    public AirlineTableController(DataViewController dataViewController) {
        super(dataViewController);
    }

    /**
     * Initializes table with observers to get Airline object data when it is added to table.
     **/
    @FXML
    private void initialize() {
        getAndObserveDataController();
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        callsignColumn.setCellValueFactory(new PropertyValueFactory<>("callsign"));
        countryColumn.setCellValueFactory(new PropertyValueFactory<>("country"));
        codeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCode()));
    }


    /**
     * {@inheritDoc}
     */
    public void getAndObserveDataController() {
        dataController = AirlineDataController.getSingleton();
        dataController.addObserver(DataController.OBSERVE_ALL, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(int numRows, boolean isLoading, char event) {
        super.update(numRows, isLoading, event);
        airlineTable.setItems(currItems);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void viewRow() {
        if (airlineTable.getSelectionModel().getSelectedItem() != null) {
            Airline selectedAirline = airlineTable.getSelectionModel().getSelectedItem();
            detailRootController.setDetailViewObject(selectedAirline);
        }

    }
}
