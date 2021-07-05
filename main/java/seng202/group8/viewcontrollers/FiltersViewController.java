package seng202.group8.viewcontrollers;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import seng202.group8.datacontroller.FiltersController;
import seng202.group8.viewcontrollers.filterviews.NumericFilterView;
import seng202.group8.viewcontrollers.filterviews.TextualFilterView;

/**
 * A controller for the Filters View component. This is the components that contains all the JavaFX filter components.
 * Responsible for displaying the relevant filters for each type of data.
 */
public class FiltersViewController {
    @FXML
    private VBox filterPane;
    @FXML
    private ScrollPane scrollPane;

    private FiltersController filters = FiltersController.getSingleton();

    private TextualFilterView airlineNameFilterView = new TextualFilterView(filters.getAirlineNameFilter());
    private TextualFilterView airlineCodeFilterView = new TextualFilterView(filters.getAirlineCodeFilter());
    private TextualFilterView startFilterView = new TextualFilterView(filters.getStartFilter());
    private TextualFilterView destinationFilterView = new TextualFilterView(filters.getDestinationFilter());
    private TextualFilterView countryFilterView = new TextualFilterView(filters.getCountryFilter());
    private TextualFilterView airportCodeFilterView = new TextualFilterView(filters.getAirportCodeFilter());
    private TextualFilterView airportNameFilterView = new TextualFilterView(filters.getAirportNameFilter());
    private NumericFilterView priceFilterView = new NumericFilterView(filters.getPriceFilter());
    private NumericFilterView flightNumberFilterView = new NumericFilterView(filters.getRouteNumberFilter());
    private NumericFilterView durationFilterView = new NumericFilterView(filters.getDurationFilter());

    @FXML
    public void applyFilters() {
        filters.notifyAllObservers();
    }

    /**
     * Displays the filters relevant to filtering Airports on the filterPane
     */
    public void showAirportsFilters() {
        filterPane.getChildren().clear();
        filterPane.getChildren().add(countryFilterView);
        filterPane.getChildren().add(flightNumberFilterView);
        filterPane.getChildren().add(airportCodeFilterView);
        filterPane.getChildren().add(airportNameFilterView);
    }

    /**
     * Displays the filters relevant to filtering Airlines on the filterPane
     */
    public void showAirlineFilters() {
        filterPane.getChildren().clear();
        filterPane.getChildren().add(countryFilterView);
        filterPane.getChildren().add(airlineCodeFilterView);
        filterPane.getChildren().add(airlineNameFilterView);
    }

    /**
     * Displays the filters relevant to filtering Flights on the filterPane
     */
    public void showFlightFilters() {
        filterPane.getChildren().clear();
        filterPane.getChildren().add(airlineNameFilterView);
        filterPane.getChildren().add(startFilterView);
        filterPane.getChildren().add(destinationFilterView);
        filterPane.getChildren().add(priceFilterView);
        filterPane.getChildren().add(durationFilterView);
    }

    /**
     * Displays the filters relevant to filtering the map on the filterPane
     */
    public void showMapFilters() {
        filterPane.getChildren().clear();
    }
}
