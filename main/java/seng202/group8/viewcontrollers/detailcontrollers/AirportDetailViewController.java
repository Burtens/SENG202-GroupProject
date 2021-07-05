package seng202.group8.viewcontrollers.detailcontrollers;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import seng202.group8.data.Airport;
import seng202.group8.datacontroller.AirportDataController;
import seng202.group8.datacontroller.DataConstraintsException;
import seng202.group8.datacontroller.RouteDataController;
import seng202.group8.io.Database;
import seng202.group8.viewcontrollers.NumberSpinnerHelper;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.function.UnaryOperator;


/**
 * A concrete subclass of DetailController that controls an airport details pane
 */
public class AirportDetailViewController extends DetailViewController<Airport> {

    @FXML
    private Label codeLabel;

    @FXML
    private Label clockLabel;

    @FXML
    private Label nameLabel;

    @FXML
    private Label cityLabel;

    @FXML
    private Label countryLabel;

    @FXML
    private Label latLabel;

    @FXML
    private Label longLabel;

    @FXML
    private Label altitudeLabel;

    @FXML
    private TextField airportNameEditField;

    @FXML
    private TextField airportIATAEditField;

    @FXML
    private TextField airportICAOEditField;

    @FXML
    private ChoiceBox<String> countryEditSelector;

    @FXML
    private TextField cityEditField;

    @FXML
    private Spinner<Double> longitudeEditSpinner;

    @FXML
    private Spinner<Double> latitudeEditSpinner;

    @FXML
    private Spinner<Integer> altitudeEditSpinner;

    @FXML
    private ChoiceBox<String> timezoneEditSelector;


    private Timeline clock = new Timeline();

    DecimalFormat latLongDecimalFormat = new DecimalFormat("0.00000");     // 5 decimal place format for latitude / longitude

    @FXML
    private void initialize() {
        dataController = AirportDataController.getSingleton();  // Very important - must get the data controller before we do anything!
        //Sets the values of the timezone selector.
        timezoneEditSelector.setItems(FXCollections.observableArrayList(
                "+14.0", "+13.0", "+12.0", "+11.0", "+10.0", "+9.0", "+8.0", "+7.0", "+6.0", "+5.0", "+4.0", "+3.0", "+2.0",
                "+1.0", "+0.0", "-1.0", "-2.0", "-3.0", "-4.0", "-5.0", "-6.0", "-7.0", "-8.0", "-9.0", "-10.0", "-11.0"));
        countryEditSelector.setItems(FXCollections.observableArrayList(Database.getAllCountryNames()));

        /*
        Fixes formats for spinners so that they only allow double values to be typed in.
        Much of this code was inspired by the following StackOverflow question and answer(s):
        https://stackoverflow.com/questions/25885005/insert-only-numbers-in-spinner-control
         */
        // get a localized format for parsing
        NumberFormat format = NumberFormat.getNumberInstance();
        UnaryOperator<TextFormatter.Change> filter = c -> {
            if (c.isContentChange()) {
                ParsePosition parsePosition = new ParsePosition(0);
                // NumberFormat evaluates the beginning of the text
                format.parse(c.getControlNewText(), parsePosition);
                if (parsePosition.getIndex() == 0 ||
                        parsePosition.getIndex() < c.getControlNewText().length()) {
                    // reject parsing the complete text failed
                    return null;
                }
            }
            return c;
        };

        // Sets up initial values for spinners and increment rates for spinners
        longitudeEditSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-180.0, 180.0, 0.00, 0.00001));
        NumberSpinnerHelper.addDoubleFormat(longitudeEditSpinner, latLongDecimalFormat);

        latitudeEditSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-90.0, 90.0, 0.00, 0.00001));
        NumberSpinnerHelper.addDoubleFormat(latitudeEditSpinner, latLongDecimalFormat);

        altitudeEditSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-30000, 30000, 0, 1));
        NumberSpinnerHelper.addIntegerFormat(altitudeEditSpinner);

    }

    /**
     * {@inheritDoc}
     */
    public void load() {
        clock.stop();
        //Formats timezone so that it can be used in clock on detail screen
        int timezoneHours = (int) selectedItem.getTimezone();
        int timezoneMins = (int) (60 * Math.abs(selectedItem.getTimezone() - timezoneHours));
        String minsString = String.format("%02d", timezoneMins);
        String hoursString = String.format("%02d", Math.abs(timezoneHours));
        ZoneId utcTimezoneString;
        if (timezoneHours >=0) {
            utcTimezoneString = ZoneId.ofOffset("UTC", ZoneOffset.of("+" + hoursString + ":" + minsString));
        } else {
            utcTimezoneString = ZoneId.ofOffset("UTC", ZoneOffset.of("-" + hoursString + ":" + minsString));
        }

        //Animated Clock, created with help from https://stackoverflow.com/questions/42383857/javafx-live-time-and-date/42384436
        clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            OffsetDateTime currentTime = OffsetDateTime.now(utcTimezoneString);
            clockLabel.setText(String.format("%02d", currentTime.getHour()) + ":" + String.format("%02d", currentTime.getMinute()) + ":" + String.format("%02d", currentTime.getSecond()));
            }),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();


        if (selectedItem.getTimezone() >= 0){
            timezoneEditSelector.setValue("+"+selectedItem.getTimezone());
        }
        else {
            timezoneEditSelector.setValue(String.valueOf(selectedItem.getTimezone()));
        }

        Airport item = selectedItem;
        mainPane.setVisible(true);
        viewBox.setVisible(true);

        codeLabel.setText(item.getCode());

        //Dynamically Resize Name
        //First get width of text if it was at normal font
        Text tempText = new Text(item.getName());
        tempText.setFont(new Font(nameLabel.getFont().getName(), 24));
        double textWidth = tempText.getLayoutBounds().getWidth() + 5;   // Add a little bit of margin

        double scaleFactor = 1;
        // If name is too long, resize it
        if (textWidth > mainPane.getWidth()) {
            scaleFactor = mainPane.getWidth() / textWidth;
        }
        nameLabel.setFont(new Font(nameLabel.getFont().getName(), 24 * scaleFactor));

        nameLabel.setText(item.getName());
        cityLabel.setText(item.getCity());
        countryLabel.setText((item.getCountry()));
        latLabel.setText("Lat: " + latLongDecimalFormat.format(item.getLatitude()));
        longLabel.setText("Long: " + latLongDecimalFormat.format(item.getLongitude()));
        altitudeLabel.setText("Altitude: " + item.getAltitude() + " ft");
    }

    /**
     * {@inheritDoc}
     */
    public void clearAllEditFields() {
        airportNameEditField.setText("");
        airportIATAEditField.setText("");
        airportICAOEditField.setText("");
        cityEditField.setText("");
        countryEditSelector.getSelectionModel().clearSelection();
        longitudeEditSpinner.getValueFactory().setValue(0.0);
        latitudeEditSpinner.getValueFactory().setValue(0.0);
        altitudeEditSpinner.getValueFactory().setValue(0);
        timezoneEditSelector.setValue("+0.0");
    }

    /**
     * {@inheritDoc}
     */
    public void populateAllEditFields() {
        Airport item = selectedItem;
        airportNameEditField.setText(item.getName());
        airportIATAEditField.setText(item.getIata());
        airportICAOEditField.setText(item.getIcao());
        cityEditField.setText(item.getCity());
        countryEditSelector.setValue(item.getCountry());
        longitudeEditSpinner.getValueFactory().setValue(item.getLongitude());
        latitudeEditSpinner.getValueFactory().setValue(item.getLatitude());
        altitudeEditSpinner.getValueFactory().setValue(item.getAltitude());
    }

    /**
     * {@inheritDoc}
     */
    public DataConstraintsException updateSelectedItem() {
        Airport item = selectedItem;
        DataConstraintsException e = null;

        e = DataConstraintsException.attempt(e, () -> item.setName(airportNameEditField.getText()));
        e = DataConstraintsException.attempt(e, () -> item.setIata(airportIATAEditField.getText()));
        e = DataConstraintsException.attempt(e, () -> item.setIcao(airportICAOEditField.getText()));
        e = DataConstraintsException.attempt(e, () -> item.setCity(cityEditField.getText()));
        e = DataConstraintsException.attempt(e, () -> item.setCountry(countryEditSelector.getValue()));
        e = DataConstraintsException.attempt(e, () -> item.setLatitude(latitudeEditSpinner.getValue()));
        e = DataConstraintsException.attempt(e, () -> item.setLongitude(longitudeEditSpinner.getValue()));
        e = DataConstraintsException.attempt(e, () -> item.setAltitude(altitudeEditSpinner.getValue()));
        e = DataConstraintsException.attempt(e, () -> item.setTimezone(Double.parseDouble(timezoneEditSelector.getValue())));
        return e;
    }

    /**
     * {@inheritDoc}
     */
    public DataConstraintsException createItem() {
        DataConstraintsException e = null;

        e = DataConstraintsException.attempt(e, () -> {
            selectedItem = new Airport(airportNameEditField.getText(),
                    cityEditField.getText(), countryEditSelector.getValue(), airportIATAEditField.getText(), airportICAOEditField.getText(),
                    latitudeEditSpinner.getValue(), longitudeEditSpinner.getValue(), altitudeEditSpinner.getValue(),
                    Double.parseDouble(timezoneEditSelector.getValue()), 'y');
        });
        return e;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean saveEdit() throws SQLException {
        boolean val = super.saveEdit();
        if (val) {
            // successfully saved
            RouteDataController.getSingleton().autoGenerateValuesForAllRoutesWithPriceZero();
            // This line may slow things down a lot in the worst case scenario when the user manually made a lot of routes
            // where the source or destination airport is the new code for this airport
            // However, this is unlikely and this line is needed to get the route filters working correctly
        }
        return val;
    }
}
