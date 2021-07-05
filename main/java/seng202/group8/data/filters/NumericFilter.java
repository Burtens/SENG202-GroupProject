package seng202.group8.data.filters;

import seng202.group8.viewcontrollers.filterviews.NumericFilterView;

/**
 * An class that represents a numeric filter component. Contains upper and lower bounds that can be used to filter
 * numeric attributes of collections of objects.
 */
public class NumericFilter extends Filter {
    private final Integer min;
    private final Integer max;
    private final Integer stepBy;

    private FilterRange<Integer> range;
    private NumericFilterView filterView;

    /**
     * Create the NumericFilter
     *  @param filterName The name of the filter, to be displayed at the top of this control
     * @param min        The minimum possible value of the filter's lower bound
     * @param max        The maximum possible value of the filter's upper bound
     * @param stepBy     The amount by which the number spinners step per each arrow press
     */
    public NumericFilter(String filterName, Integer min, Integer max, Integer stepBy) {
        super(filterName);
        if (min >= max) {
            throw new IllegalArgumentException("Minimum cannot be higher than the maximum");
        }
        this.min = min;
        this.max = max;
        this.range = new FilterRange<>(min, max);
        this.stepBy = stepBy;
    }

    /**
     * Returns the minimum bound for this filter (ie the lowest possible low value allowed)
     *
     * @return The minimum bound for this filter
     */
    public Integer getMin() {
        return min;
    }

    /**
     * Returns the maximum bound for this filter (ie the highest possible high value allowed)
     *
     * @return The maximum bound for this filter
     */
    public Integer getMax() {
        return max;
    }

    /**
     * Returns the amount by which the filter's values should step by per increment.
     *
     * @return the amount by which the filter's values should step by per increment.
     */
    public Integer getStepBy() {
        return stepBy;
    }

    /**
     * Sets the (inclusive) range that this filter is set to filter by
     *
     * @param range the (inclusive) range that this filter is set to filter by
     */
    public void setRange(FilterRange<Integer> range) {
        this.range = range;
    }

    /**
     * Gets the (inclusive) range that this filter is set to filter by
     *
     * @return the (inclusive) range that this filter is set to filter by
     */
    public FilterRange<Integer> getBounds() {
        if (this.filterView != null) {  // Get the bounds from the filter view if one is attached
            setRange(filterView.getBounds());
        }
        return range;
    }

    /**
     * Sets the numeric filter view component that interfaces this object
     *
     * @param filterView the numeric filter view component that interfaces this object
     */
    public void setFilterView(NumericFilterView filterView) {
        this.filterView = filterView;
    }
}
