package seng202.group8.viewcontrollers;

import com.sun.javafx.webkit.WebConsoleListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import seng202.group8.AlertHelper;
import seng202.group8.data.Airport;
import seng202.group8.data.Route;
import seng202.group8.data.Trip;
import seng202.group8.data.TripFlight;
import seng202.group8.datacontroller.AirlineDataController;
import seng202.group8.datacontroller.AirportDataController;
import seng202.group8.datacontroller.RouteDataController;
import seng202.group8.datacontroller.TripDataController;

import java.sql.SQLException;
import java.util.*;

/**
 * Controller for the map view. Is essentially a JS interface for a FX WebView, using the Google Maps API.
 * https://stackoverflow.com/a/7525278
 */
public class MapViewController implements TripDataController.CurrentTripObserver {
    private RouteDataController routeDC;
    private AirportDataController airportDC;
    private AirlineDataController airlineDC;
    private TripDataController tripDC;
    @FXML
    private WebView webView;
    @FXML
    private Button reloadButton;

    private WebEngine engine;
    private boolean pageLoaded = false;

    private Timer pageLoadFailsTimer = null;

    private String previousJS = ""; // previous JS sent; slightly reduces load on map

    @FXML
    public void initialize() {
        this.engine = webView.getEngine();
        routeDC = RouteDataController.getSingleton();
        airportDC = AirportDataController.getSingleton();
        airlineDC = AirlineDataController.getSingleton();
        tripDC = TripDataController.getSingleton();
        tripDC.subscribeToCurrentTrip(this);

        //engine.load("https://www.youtube.com/watch?v=dQw4w9WgXcQ");


        WebConsoleListener.setDefaultListener((_webView, message, lineNumber, sourceId) -> {
            String[] path = sourceId.split("/");
            String filename = path[path.length - 1];
        });

        loadPage();
    }

    /**
     * Loads the page and starts the load fail timer
     */
    public void loadPage() {
        engine.load(getClass().getResource("/seng202/group8/map/map.html").toExternalForm());
        reloadButton.setVisible(true);

//        https://docs.oracle.com/javase/8/javafx/embedded-browser-tutorial/js-javafx.htm
        engine.getLoadWorker().stateProperty().addListener(
                (ObservableValue<? extends State> ov, State oldState, State newState) -> {
                    if (newState == State.SUCCEEDED) {
                        // Some kind of magic
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("java", new JSInterface());
                        // Creates a new variable called 'java' in the global scope that is an instance of the 'JSInterface' class. JS call call its methods
                    }
        });

        if (pageLoadFailsTimer != null) {
            pageLoadFailsTimer.cancel();
            pageLoadFailsTimer = null;
        }

        pageLoadFailsTimer = new Timer();
        pageLoadFailsTimer.schedule(
            new TimerTask() {
                @Override
                public void run() {
                    reloadButton.setVisible(true);
                }
            },
            5000
        );

    }

    @FXML
    private void reloadButtonPressed() {
        loadPage();
    }

    /**
     * Some kind of magic that allows JS to call Java methods
     */
    public class JSInterface {
        public void onLoad() {
            pageLoaded = true;
            reloadButton.setVisible(false);
            if (pageLoadFailsTimer != null) {
                pageLoadFailsTimer.cancel();
                pageLoadFailsTimer = null;
            }
            // If trip id set before the page loads, it will error out, so need this to set trip when it loads
            sendTripToJS(tripDC.getCurrentlyOpenTrip());
        }
    }


    /**
     * Create as JS representation of airport
     *
     * @param airport airport object to convert
     * @return JS object for the airport as a string
     */
    protected String airportToJs(Airport airport) {
        return String.format("new Airport(%s, %s, %s, %f, %f)",
                escapeJSString(airport.getName()),
                escapeJSString(airport.getIata()),
                escapeJSString(airport.getIcao()),
                airport.getLatitude(),
                airport.getLongitude()
        );
    }

    /**
     * Create as JS representation of the route
     *
     * @param route route object to convert
     * @return JS object for the route as a string
     */
    protected String routeToJs(Route route) {
        return String.format("new Route(%s, %s, %s, %d, %s)",
                escapeJSString(route.getSourceAirportCode()),
                escapeJSString(route.getDestinationAirportCode()),
                escapeJSString(route.getAirlineCode()),
                route.getFlightDuration(),
                toArray(route.getPlaneTypes(), val -> escapeJSString(val))
        );
    }

    /**
     * Create as JS representation of the flight
     *
     * @param flight flight object to convert
     * @return JS object for the flight as a string
     */
    protected String flightToJs(TripFlight flight) {
        return String.format("new Flight(%s, %s, %s, %s, new Date(%d))",
                escapeJSString(null), // comment not used by JS
                escapeJSString(flight.getSourceCode()),
                escapeJSString(flight.getDestinationCode()),
                escapeJSString(flight.getAirlineCode()),
                flight.getUTCTakeoffDateTime().toEpochSecond() * 1000
        );
    }

    /**
     * Create as JS representation of the trip
     *
     * @param trip trip object to convert
     * @return JS object for the trip as a string
     */
    protected String tripToJs(Trip trip) {
        return String.format("new Trip(%s, %s, %s)",
                escapeJSString(null), // name not used by JS
                escapeJSString(null), // comment not used by JS
                toArray(trip.getFlights(), flight -> flightToJs(flight))
        );
    }

    /**
     * Escapes string for JS use
     *
     * @param str string, or null
     * @return escaped string
     */
    protected String escapeJSString(String str) {
        if (str == null) {
            return "\"\"";
        }
        return "\"" + str
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
        // Prevent quotes in name causing JS injection, and replace
    }

    /**
     * Refreshes the map view
     * **/
    public void refreshMap() {
        sendTripToJS(tripDC.getCurrentlyOpenTrip());
    }

    /**
     * Interface which converts an object to a JS string
     *
     * @param <T> type of object to convert
     */
    public interface ToJSString<T> {
        /**
         * Method which converts the given object to the string
         *
         * @param val object to convert
         * @return JS string representation of the object
         */
        String toJS(T val);
    }

    /**
     * Converts the given array of elements into a JS array
     *
     * @param elements  elements to convert
     * @param converter method which converts an element into its JS representation
     * @param <T>       type of the element
     * @return array of elements as a JS array
     */
    protected <T> String toArray(T[] elements, ToJSString<T> converter) {
        ArrayList<T> list = new ArrayList<>(elements.length);
        for (int i = 0; i < elements.length; i++) {
            list.add(elements[i]);
        }
        return toArray(list, converter);
    }


    /**
     * Converts the given list of elements into a JS array
     *
     * @param elements  elements to convert
     * @param converter method which converts an element into its JS representation
     * @param <T>       type of the element
     * @return array of elements as a JS array
     */
    protected <T> String toArray(List<T> elements, ToJSString<T> converter) {
        int numElements = elements.size();

        StringBuilder output = new StringBuilder();
        output.append("[");
        for (int i = 0; i < numElements; i++) {
            output.append(converter.toJS(elements.get(i)));
            if (i + 1 != numElements) {
                output.append(", ");
            }
        }
        output.append("]");
        return output.toString();
    }

    /**
     * Converts the given list of strings into a JS array
     *
     * @param elements  elements to convert
     * @return array of elements as a JS array
     */
    protected String toArray(List<String> elements) {
        int numElements = elements.size();

        StringBuilder output = new StringBuilder();
        output.append("[");
        for (int i = 0; i < numElements; i++) {
            output.append(elements.get(i));
            if (i + 1 != numElements) {
                output.append(", ");
            }
        }
        output.append("]");
        return output.toString();
    }


    /**
     * Attempts to show the trip in the map view, sending the data to the JS running on the page
     *
     * @param trip trip to show in the map view. May be null
     */
    protected void sendTripToJS(Trip trip) {
        if (!pageLoaded) {
            // setTrip will run with this ID when the page loads
            return;
        }

        try {
            if (trip == null) {
                String js = "setData([], [], new Trip('', '', []))";

                if (!js.equals(previousJS)) {
                    previousJS = js;
                    engine.executeScript(js);
                }
                return;
            }

            HashSet<Airport> airports = new HashSet<>();
            HashSet<Route> routes = new HashSet<>();

            for (TripFlight flight : trip.getFlights()) {
                Route route = routeDC.getEntity(flight.getSourceCode(), flight.getDestinationCode(), flight.getAirlineCode());
                if (route != null) {
                    routes.add(route);
                    Airport source = airportDC.getEntity(route.getSourceAirportCode());
                    if (source != null) airports.add(source);

                    Airport destination = airportDC.getEntity(route.getDestinationAirportCode());
                    if (destination != null) airports.add(destination);
                }
            }

            ArrayList<String> airportsSorted = new ArrayList<>(airports.size());
            for (Airport airport: airports) {
                airportsSorted.add(airportToJs(airport));
            }
            // HashSet does not offer stable ordering, so need to sort so string comparison of JS stays the same across function calls given the same arguments
            Collections.sort(airportsSorted);

            ArrayList<String> routesSorted = new ArrayList<>(routes.size());
            for (Route route: routes) {
                routesSorted.add(routeToJs(route));
            }
            Collections.sort(routesSorted);

            String js = "setData(" +
                    toArray(airportsSorted) + "," +
                    toArray(routesSorted) + "," +
                    tripToJs(trip) +
                    ");";

            if (!js.equals(previousJS)) {
                previousJS = js;
                engine.executeScript(js);
            }
        } catch (SQLException throwables) {
            AlertHelper.showErrorAlert(throwables, "Database error occurred while updating map view");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void currentTripChange(Trip trip) {
        sendTripToJS(trip);
    }
}
