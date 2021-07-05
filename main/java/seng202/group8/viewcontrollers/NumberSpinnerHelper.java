package seng202.group8.viewcontrollers;

import javafx.scene.control.Spinner;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.function.UnaryOperator;

public class NumberSpinnerHelper {
    /**
     * A helper class with methods that will magically improve your number spinners. These will prevent
     * bad input from being entered into the spinners and also allow the spinners to display numbers to a proper format.
     * Why aren't these built into JavaFX???
     */

    private static UnaryOperator<TextFormatter.Change> getNumberTextFilter(NumberFormat format) {
        // Much of this code was inspired by the following StackOverflow question and answer(s):
        // https://stackoverflow.com/questions/25885005/insert-only-numbers-in-spinner-control
        UnaryOperator<TextFormatter.Change> filter = c -> {
            if (c.isContentChange()) {
                if (c.getControlNewText().equals(""))  return c;  // Allow empty string as zero
                ParsePosition parsePosition = new ParsePosition(0);
                // NumberFormat evaluates the beginning of the text
                format.parse(c.getControlNewText(), parsePosition);
                if (parsePosition.getIndex() == 0 ||
                        parsePosition.getIndex() < c.getControlNewText().length()
                        || c.getControlNewText().length() > 10) {    // Don't allow text to get too long, otherwise it might overflow an int
                    // reject parsing the complete text failed
                    return null;
                }
            }
            return c;
        };
        return filter;
    }

    /**
     * Adds an Integer text format to the given Spinner. This ensures that only valid Integers can be typed into the spinners.
     * @param spinner The spinner to add the format to.
     */
    public static void addIntegerFormat(Spinner<Integer> spinner) {
        StringConverter<Integer> converter = new StringConverter<>() {

            @Override
            public String toString(Integer integer) {
                if (integer == null) {
                    return "0";
                }
                return integer.toString();
            }

            @Override
            public Integer fromString(String string) {
                try {
                    if (string == null) {
                        return 0;
                    }
                    string = string.trim();
                    if (string.length() < 1) {
                        return 0;
                    }
                    return Integer.parseInt(string);
                } catch (NumberFormatException ex) {
                    return 0;
                }
            }
        };
        UnaryOperator<TextFormatter.Change> filter = getNumberTextFilter(NumberFormat.getIntegerInstance());
        spinner.getValueFactory().setConverter(converter);
        spinner.getEditor().setTextFormatter(new TextFormatter<>(
                converter, spinner.getValue(), filter));
    }

    /**
     * Adds the given text format to the given Spinner. This ensures that only valid Doubles can be typed into the spinners,
     * and that the doubles can be shown to the given number of decimal places given by the format.
     * @param spinner The spinner to add the format to.
     * @param format The valid decimal format to display the doubles to.
     */
    public static void addDoubleFormat(Spinner<Double> spinner, DecimalFormat format) {
        // Taken from https://stackoverflow.com/questions/56029250/how-can-i-set-decimal-places-in-javafx-spinner
        StringConverter<Double> converter = new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                if (object == null) {return "";}
                return format.format(object);}
            @Override
            public Double fromString(String string) {
                try {
                    if (string == null) {return null;}
                    string = string.trim();
                    if (string.length() < 1) {return null;}
                    return format.parse(string).doubleValue();
                } catch (ParseException ex) {
                    return null;
                }
            }
        };

        UnaryOperator<TextFormatter.Change> filter = getNumberTextFilter(NumberFormat.getNumberInstance());
        spinner.getEditor().setTextFormatter(new TextFormatter<>(
                converter, spinner.getValue(), filter));
        spinner.getValueFactory().setConverter(converter);
    }
}
