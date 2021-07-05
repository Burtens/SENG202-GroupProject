package seng202.group8.data.filters;

/**
 * An abstract class that defines common functionally between filters.
 **/
public abstract class Filter {

    protected String filterName;

    /**
     * Constructs the filter
     *  @param filterName The name of the filter
     *
     */
    public Filter(String filterName) {
        this.filterName = filterName;
    }

    /**
     * Returns the name of the filter, eg. 'Filter by _____'
     *
     * @return The name of the filter
     */
    public String getFilterName() {
        return filterName;
    }
}
