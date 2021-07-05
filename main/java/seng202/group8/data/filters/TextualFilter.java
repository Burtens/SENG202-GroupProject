package seng202.group8.data.filters;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import seng202.group8.viewcontrollers.filterviews.TextualFilterView;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * An class that represents a textual filter component. Contains a list of all options that can possibly be selected, as well as a
 * set of options that are currently selected. Can be used to filter textual attributes of a collection of objects, where
 * only attributes that are equal to one of the selected options are kept.
 */
public class TextualFilter extends Filter {
    private ObservableList<String> options;
    private HashSet<String> checkedOptions = new HashSet<>();
    private TextualFilterView textualFilterView;

    /**
     * Gets the text options that are currently selected (ticked) by the user
     *
     * @return Set of the text options that are currently selected (ticked) by the user
     */
    public Set<String> getSelectedOptions() {
        return checkedOptions;
    }

    /**
     * Selects all options given contained within the parameter collection options.
     *
     * @param options The text options selected by the user
     */
    public void setSelectedOptions(Collection<String> options) {
        this.checkedOptions = new HashSet<>(options);
    }

    /**
     * Selects a given option by adding it to the selected options list
     *
     * @param option The text options to be selected
     */
    public void selectOption(String option) {
        if (options.contains(option)) {
            this.checkedOptions.add(option);
        }
    }

    /**
     * Remove a given option by removing it from the selected options list
     *
     * @param option The text options to be removed
     */
    public void removeOption(String option) {
        this.checkedOptions.remove(option);
    }

    /**
     * Sets the text options that can be selected by the user
     *
     * @param options The text options that can be selected by the user
     */
    public void setOptions(Collection<String> options) {
        this.options = FXCollections.observableArrayList(options);
        checkedOptions.retainAll(options);  // Ensure that selected options which are no longer valid are removed
        if (textualFilterView != null) {
            textualFilterView.setOptions(this.options);
        }
    }

    /**
     * Gets the text options that can be selected by the user
     *
     * @return options The text options that can be selected by the user
     */
    public ObservableList<String> getOptions() {
        return options;
    }

    /**
     * Sets the textual filter view component that interfaces this object
     *
     * @param filterView the textual filter view component that interfaces this object
     */
    public void setFilterView(TextualFilterView filterView) {
        this.textualFilterView = filterView;
    }

    /**
     * Create the TextualFilter
     *  @param filterName The name of the filter, to be displayed at the top of this control
     * @param options    The options to be visible by default in the checklist
     */
    public TextualFilter(String filterName, Collection<String> options) {
        super(filterName);
        setOptions(options);
    }

}
