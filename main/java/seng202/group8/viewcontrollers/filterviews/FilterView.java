package seng202.group8.viewcontrollers.filterviews;

import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import seng202.group8.AlertHelper;
import seng202.group8.data.filters.Filter;

import java.io.IOException;

/**
 * An abstract class that defines common functionally between filter views.
 * Acts as an interface for a single filter object
 * Built for JavaFX FXML extensions, see this link for more details:
 * http://www.devlabs.ninja/article/fxml-markup-inheritance-javafx/
 **/
@DefaultProperty("extension")
public abstract class FilterView extends TitledPane {
    private Filter filter;

    @FXML
    private TitledPane titledPane;
    @FXML
    private VBox extension;
    @FXML
    private Text titleText;
    @FXML
    protected Button clearButton;
    @FXML
    private BorderPane titleGraphic;

    /**
     * Needed for JavaFX Extension to work
     *
     * @return Children of the extension VBox
     */
    public ObservableList<Node> getExtension() {
        return extension.getChildren();
    }

    /**
     * Defines CSS pseudoclass for this custom filter, with the two true/false states representing an enabled and disabled
     * filter respectively. See https://guigarage.com/2016/02/javafx-and-css-pseudo-classes/ for more details.
     */
    private static PseudoClass FILTER_ENABLED_PSEUDO_CLASS = PseudoClass.getPseudoClass("filter-enabled");

    BooleanProperty filterEnabled = new BooleanPropertyBase(false) {
        public void invalidated() {
            pseudoClassStateChanged(FILTER_ENABLED_PSEUDO_CLASS, get());
        }

        @Override
        public Object getBean() {
            return FilterView.this;
        }

        @Override
        public String getName() {
            return "filter-enabled";
        }
    };

    /**
     * Event handler for the clear button is pressed. Should clear any selected or entered values in the filter.
     */
    @FXML
    abstract protected void clear();

    /**
     * Initialise the pane title text
     */
    @FXML
    public void initialize() {
        // Code below must be run in this function, and not the constructor, as checkList and titledPane are only guaranteed to be initialised in this function
        titleText.setText(filter.getFilterName());
        // Ensure title graphic fills most of the space in the title
        titleGraphic.prefWidthProperty().bind(titledPane.widthProperty().subtract(40));
        // Add CSS class to self
        this.getStyleClass().add("filter-view");
    }

    /**
     * Constructs the filter view
     *
     * @param filter The filter object that this view will be an interface to
     */
    public FilterView(Filter filter) {
        this.filter = filter;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/seng202/group8/filter.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            AlertHelper.showErrorAlertIOErrorLoadingFXML(exception, "The filter view could not be loaded");
        }
    }
}
