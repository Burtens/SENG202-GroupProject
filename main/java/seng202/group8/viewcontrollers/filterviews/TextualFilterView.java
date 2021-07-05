package seng202.group8.viewcontrollers.filterviews;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import org.controlsfx.control.CheckListView;
import seng202.group8.AlertHelper;
import seng202.group8.data.filters.TextualFilter;
import seng202.group8.data.filters.TextualFilterPredicate;

import java.io.IOException;

/**
 * A custom JavaFX control that contains a checklist that can be filtered by a text search bar.
 * Acts as an interface for a textual filter object
 * Designed to the JavaFX custom control pattern:
 * see https://docs.oracle.com/javafx/2/fxml_get_started/custom_control.htm) for more details
 */
public class TextualFilterView extends FilterView {
    private TextualFilter textualFilter;
    private FilteredList<String> filteredOptionsList;

    @FXML
    TitledPane titledPane;
    @FXML
    TextField searchBar;
    @FXML
    CheckListView<String> checkList;

    /**
     * Initialise the pane title text and set the initial checklist options
     */
    @FXML
    public void initialize() {
        super.initialize();
        // For some reason initialize() gets called twice per TextualFilter, and checkList is always null the first time
        if (checkList == null) return;

        checkList.setItems(filteredOptionsList);
        setCheckListChangeListener();
        filterOptions();
    }

    /**
     * Filters the checklist to only show the options that contain the searchbar text as a substring
     */
    @FXML
    public void filterOptions() {
        filteredOptionsList.setPredicate(new TextualFilterPredicate(searchBar.getText()));
        // When text is empty, make the checked options appear at the top
        if (searchBar.getText().length() == 0) {
            textualFilter.getOptions().removeAll(textualFilter.getSelectedOptions());   // Remove checked options
            textualFilter.getOptions().addAll(0, textualFilter.getSelectedOptions());   // Then re-add them to the top of the list
        }
        // CheckList clears all checks when options are filtered, so need to re-check all items that has been checked
        for (String checkedOption : textualFilter.getSelectedOptions()) {
            checkList.getCheckModel().check(checkedOption);
        }
        forceSearchbarFocus();
    }


    /**
     * {@inheritDoc}
     * Resets the numeric values in the filter to the min or max bounds. For example, if the filter is in the
     * "Between" mode, the range will be set to the min and max values
     */
    @Override
    protected void clear() {
        checkList.getCheckModel().clearChecks();
        clearButton.setDisable(true);
    }

    /**
     * Adds a list change listener to the checklist that updates the checked options collection when an item is ticked/unticked.
     * The listener will add any newly checked items to checkedOptions and remove any newly unchecked items.
     * This methods must be called whenever the checkList's items are changed as the old listener will be removed.
     */
    private void setCheckListChangeListener() {
        checkList.getCheckModel().getCheckedItems().addListener(
                (ListChangeListener<String>) change -> {
                    // Record whether an item was checked or unchecked
                    change.next();
                    if (change.wasAdded()) {
                        textualFilter.getSelectedOptions().add(change.getAddedSubList().get(0)); // Add checked item to checkedOptions
                        clearButton.setDisable(false);
                    } else if (change.wasRemoved()) {
                        textualFilter.getSelectedOptions().remove(change.getRemoved().get(0));   // Remove unchecked item from checkedOptions
                        if (textualFilter.getSelectedOptions().size() == 0) {
                            clearButton.setDisable(true);  // Disable clear button if no options are selected anymore
                        }
                    }
                });
    }

    /**
     * This method is a hack to fix a bug where another node (probably a child of the checklist) would be given focus
     * when something was typed into the searchbar. It attempts to give the focused node a listener that gives focus
     * over to the searchbar whenever the node itself is focused.
     */
    private void forceSearchbarFocus() {
        if (getScene() != null) {
            // Find focused node
            Node focusedNode = getScene().focusOwnerProperty().get();

            if (focusedNode != searchBar && focusedNode != titledPane) {    // Ensure we can still select the title
                searchBar.requestFocus();   // Set focus to the search bar
                // JavaFX wants to select the entire text when focus is initially restored so move selection to end of the text
                searchBar.selectRange(searchBar.getText().length(), searchBar.getText().length());

                // Force the focused item to, when focused, give focus over to the searchbar
                focusedNode.focusedProperty().addListener((observable, oldValue, newValue) -> searchBar.requestFocus());
            }
        }
    }

    /**
     * Sets the text options that can be selected by the user
     *
     * @param options The text options that can be selected by the user
     */
    public void setOptions(ObservableList<String> options) {
        filteredOptionsList = new FilteredList<String>(options);
        if (checkList != null) {    // Guard against JavaFX initialisation shenanigans
            checkList.setItems(filteredOptionsList);
            setCheckListChangeListener();
            filterOptions();  // Ensure new options are filtered by the text already in the searchbox
        }
    }


    /**
     * Create the TextualFilterView
     *
     * @param textualFilter The textual filter object that this view will be an interface to
     */
    public TextualFilterView(TextualFilter textualFilter) {
        super(textualFilter);
        this.textualFilter = textualFilter;
        this.textualFilter.setFilterView(this);
        setOptions(textualFilter.getOptions());

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/seng202/group8/textualFilter.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            AlertHelper.showErrorAlertIOErrorLoadingFXML(exception, "The textual filters file could not be loaded");
        }
    }

}
