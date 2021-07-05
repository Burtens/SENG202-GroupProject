package seng202.group8.data.filters;


/**
 * Defines a range for a numerical filter
 **/
public class FilterRange<T> {
    /**
     * Minimum (inclusive) value in range. If null there is no lower bound
     */
    public T min;
    /**
     * Maximum (inclusive) value in range. If null there is no upper bound
     */
    public T max;

    /**
     * Constructor for filter range
     *
     * @param min Minimum value in range, inclusive. Can be null
     * @param max Maximum value in range, inclusive. Can be null
     */
    public FilterRange(T min, T max) {
        this.min = min;
        this.max = max;
    }
}
