package seng202.group8.viewcontrollers.filterviews;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import seng202.group8.data.filters.FilterRange;
import seng202.group8.data.filters.NumericFilter;
import seng202.group8.viewcontrollers.NumberSpinnerHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom JavaFX component that provides an interface for a NumericFilter object. Provides two number spinners, as well
 * as a combobox to select the mode of the filter (Between, Less than, and More than). If the Between mode is selected,
 * then the two spinners are used to enter the lower and upper bounds respectively. Otherwise, there is a single spinner
 * for a single value.
 */
public class NumericFilterView extends FilterView {
    private Spinner<Integer> topSpinner;
    private Spinner<Integer> bottomSpinner;
    private NumericFilter numericFilter;

    @FXML
    HBox topHBox;
    @FXML
    HBox bottomHBox;
    @FXML
    ComboBox<String> combo;

    /**
     * {@inheritDoc}
     * Resets the numeric values in the filter to the defaults. The default values depend on the mode of the textual fitler.
     * For example, if the filter is the "Between" mode, then the range is set to the min and max values given in the
     * associated numeric filter object. If the filter is in the "More than" mode, then the value is set to the max value.
     * Will also disable the clear button until the filter values are changed by the user.
     */
    @Override
    protected void clear() {
        switch (combo.getSelectionModel().getSelectedItem()) {
            case "More Than":
                topSpinner.getValueFactory().setValue(numericFilter.getMin());
                break;
            case "Less Than":
                topSpinner.getValueFactory().setValue(numericFilter.getMax());
                break;
            default:    // Ie. "Between" case
                topSpinner.getValueFactory().setValue(numericFilter.getMin());
                bottomSpinner.getValueFactory().setValue(numericFilter.getMax());
        }
        clearButton.setDisable(true);
    }

    /**
     * Takes in a spinner for the numeric filter and initialises it. This includes setting a few JavaFX attributes
     * (eg width, font size), as well as initialising the spinner's text format.
     * @param spinner The spinner to be initialised
     */
    private void initialiseSpinner(Spinner<Integer> spinner) {
        spinner.setEditable(true);
        spinner.setPrefWidth(120);
        spinner.getEditor().setStyle("-fx-font-size: 14;");
        NumberSpinnerHelper.addIntegerFormat(spinner);

        spinner.valueProperty().addListener((obs, oldValue, newValue) ->
                clearButton.setDisable(false));
        spinner.getEditor().textProperty().addListener((obs, oldValue, newValue) ->
                clearButton.setDisable(false));
    }

    /**
     * Event handler for when the combobox is selected. Will switch the mode of the spinner depending on the mode selected.
     */
    @FXML
    public void comboSelected() {
        if (combo.getSelectionModel().getSelectedItem() == "Between") {
            bottomHBox.setVisible(true);
        } else {
            bottomHBox.setVisible(false);
        }
    }

    /**
     * Initialise the pane title text and set the bounds
     */
    @FXML
    public void initialize() {
        // Code below must be run in this function, and not the constructor, as checkList and titledPane are only guaranteed to be initialised in this function
        super.initialize();

        if (topHBox == null) return;

        // Initialise Spinners and add them to the borderPane.
        // Cannot be done in FXML as the min, max and stepBy parameters are dynamic
        topSpinner = new Spinner<>(numericFilter.getMin(), numericFilter.getMax(), numericFilter.getMin(), numericFilter.getStepBy());
        initialiseSpinner(topSpinner);
        topHBox.getChildren().add(topSpinner);

        bottomSpinner = new Spinner<>(numericFilter.getMin(), numericFilter.getMax(), numericFilter.getMax(), numericFilter.getStepBy());
        initialiseSpinner(bottomSpinner);
        bottomHBox.getChildren().add(bottomSpinner);

        List<String> comboOptions = new ArrayList<String>();
        comboOptions.add("Between");
        comboOptions.add("More Than");
        comboOptions.add("Less Than");
        combo.setItems(FXCollections.observableList(comboOptions));
        combo.getSelectionModel().selectFirst(); // Sets default selected value to "Between"
    }

    /**
     * Create the NumericFilterView
     *
     * @param numericFilter The numeric filter object that this view will be an interface to
     */
    public NumericFilterView(NumericFilter numericFilter) {
        super(numericFilter);
        this.numericFilter = numericFilter;
        this.numericFilter.setFilterView(this);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/seng202/group8/numericFilter.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Gets the upper and lower bound of the textual filter view as entered by the user
     *
     * @return The upper and lower bound, stored in the max and min FilterRange attributes respectively
     */
    public FilterRange<Integer> getBounds() {
        Integer upperBound;
        Integer lowerBound;
        switch (combo.getSelectionModel().getSelectedItem()) {
            case "More Than":
                lowerBound = topSpinner.getValue();
                upperBound = null;
                break;
            case "Less Than":
                lowerBound = null;
                upperBound = topSpinner.getValue();
                break;
            default:    // Ie. "Between" case
                upperBound = bottomSpinner.getValue();
                lowerBound = topSpinner.getValue();
        }

        return new FilterRange<>(lowerBound, upperBound);
    }
}
