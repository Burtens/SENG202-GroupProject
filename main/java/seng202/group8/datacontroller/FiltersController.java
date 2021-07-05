package seng202.group8.datacontroller;

import seng202.group8.AlertHelper;
import seng202.group8.data.Airline;
import seng202.group8.data.Airport;
import seng202.group8.data.filters.FilterChangeObserver;
import seng202.group8.data.filters.NumericFilter;
import seng202.group8.data.filters.TextualFilter;
import seng202.group8.io.Database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores Current Filters for view and a list of FilterChangeObservers
 * currently subscribed to this class.
 **/
public class FiltersController {
    private static FiltersController singleton;

    // List of objects observing the filters
    private ArrayList<FilterChangeObserver> observers = new ArrayList<FilterChangeObserver>();

    private TextualFilter airlineNameFilter;

    private TextualFilter airlineCodeFilter;
    private TextualFilter startFilter;
    private TextualFilter destinationFilter;
    private TextualFilter countryFilter;
    private TextualFilter airportCodeFilter;
    private TextualFilter airportNameFilter;
    private NumericFilter priceFilter;
    private NumericFilter flightNumberFilter;
    private NumericFilter durationFilter;


    private DataObserver<Airport> airportObserver = new DataObserver<Airport>() {

        @Override
        public void dataChangedEvent(Airport data) {
            try {
                List<Airport> airports = AirportDataController.getSingleton().getAllEntities();

                ArrayList<String> airportCodes = new ArrayList<String>();
                for (Airport airport : airports) {
                    airportCodes.add(airport.getCode());
                }

                ArrayList<String> airportNames = new ArrayList<String>();
                for (Airport airport : airports) {
                    airportNames.add(airport.getName());
                }

                startFilter.setOptions(airportCodes);
                destinationFilter.setOptions(airportCodes);
                airportCodeFilter.setOptions(airportCodes);

                airportNameFilter.setOptions(airportNames);
            } catch (SQLException sql) {
                AlertHelper.showErrorAlert(sql, "Database error occurred while updating airport filters");
            }
        }
    };

    private DataObserver<Airline> airlineObserver = new DataObserver<Airline>() {

        @Override
        public void dataChangedEvent(Airline data) {
            try {
                List<Airline> airlines = AirlineDataController.getSingleton().getAllEntities();

                ArrayList<String> airlineNames = new ArrayList<>();
                for (Airline airline : airlines) {
                    airlineNames.add(airline.getName());
                }

                ArrayList<String> airlineCodes = new ArrayList<>();
                for (Airline airline : airlines) {
                    airlineCodes.add(airline.getCode());
                }

                airlineNameFilter.setOptions(airlineNames);
                airlineCodeFilter.setOptions(airlineCodes);
            } catch (SQLException sql) {
                AlertHelper.showErrorAlert(sql, "Database error occurred while updating airline filters");
            }
        }
    };

    /**
     * Gets the singleton instance for the Filters
     *
     * @return the singleton instance
     */
    public static FiltersController getSingleton() {
        if (singleton == null) {
            singleton = new FiltersController();
        }

        return singleton;
    }

    /**
     * Constructor for filters controllers; attempts to initialize all filters with an empty list of options
     * and subscribe to relevant data types
     */
    private FiltersController() {
        ArrayList<String> items = new ArrayList<>();
        airlineNameFilter = new TextualFilter("By Airline Name", items);

        airlineCodeFilter = new TextualFilter("By Airline Code", items);

        startFilter = new TextualFilter("By Starting Airport", items);

        destinationFilter = new TextualFilter("By Destination Airport", items);

        airportCodeFilter = new TextualFilter("By Airport Code", items);

        airportNameFilter = new TextualFilter("By Airport Name", items);

        countryFilter = new TextualFilter("By Country", Database.getAllCountryNames());

        priceFilter = new NumericFilter("By Price", 0, 5000, 10);

        flightNumberFilter = new NumericFilter("By Number of Flights", 0, 5000, 1);

        durationFilter = new NumericFilter("By Duration", 0, 50000, 100);

        // If testing, filters don't work, so if add observer is at the top, subscription will occur and when
        // the observer methods are called, it will try and access uninitialized filters
        AirlineDataController.getSingleton().addObserver(DataController.OBSERVE_ALL, airlineObserver);
        AirportDataController.getSingleton().addObserver(DataController.OBSERVE_ALL, airportObserver);

        airlineObserver.dataChangedEvent(null);
        airportObserver.dataChangedEvent(null);
    }


    /**
     * Adds (Subscribes) a FilterChangeObserver to a list of currently
     * added observers.
     *
     * @param observer a FilterChangeObserver object
     * @see FilterChangeObserver
     **/
    public void addObserver(FilterChangeObserver observer) {
        observers.add(observer);
    }

    /**
     * Notifies all observers that a change in filters is applied.
     * Does this by calling filterChangedEvent in each observer and passing through a copy of itself
     * @see FilterChangeObserver
     **/
    public void notifyAllObservers() {
        for (FilterChangeObserver observer : observers) {
            observer.filterChangedEvent();
        }
    }

    /**
     * Gets the filter for filtering by airline name
     * @return The filter for filtering by airline name
     */
    public TextualFilter getAirlineNameFilter() {
        return airlineNameFilter;
    }

    /**
     * Gets the filter for filtering by airline code
     * @return The filter for filtering by airline code
     */
    public TextualFilter getAirlineCodeFilter() {
        return airlineCodeFilter;
    }

    /**
     * Gets the filter for filtering by starting airport code
     * @return The filter for filtering by starting airport code
     */
    public TextualFilter getStartFilter() {
        return startFilter;
    }

    /**
     * Gets the filter for filtering by destination airport code
     * @return The filter for filtering by destination airport code
     */
    public TextualFilter getDestinationFilter() {
        return destinationFilter;
    }

    /**
     * Gets the filter for filtering by country
     * @return The filter for filtering by country
     */
    public TextualFilter getCountryFilter() {
        return countryFilter;
    }

    /**
     * Gets the filter for filtering by airport code
     * @return The filter for filtering by airport code
     */
    public TextualFilter getAirportCodeFilter() {
        return airportCodeFilter;
    }

    /**
     * Gets the filter for filtering by airport name
     * @return The filter for filtering by airport name
     */
    public TextualFilter getAirportNameFilter() {
        return airportNameFilter;
    }

    /**
     * Gets the filter for filtering by flight price
     * @return The filter for filtering by flight price
     */
    public NumericFilter getPriceFilter() {
        return priceFilter;
    }

    /**
     * Gets the filter for filtering by the total number of routes starting from a airport
     * @return The filter for filtering by the total number of routes starting from a airport
     */
    public NumericFilter getRouteNumberFilter() {
        return flightNumberFilter;
    }

    /**
     * Gets the filter for filtering by the duration of a flight
     * @return The filter for filtering by the duration of a flight
     */
    public NumericFilter getDurationFilter() {
        return durationFilter;
    }
}
