package seng202.group8.data.filters;

import java.util.function.Predicate;

/**
 * A Predicate to be used for a TextualFilter, matching all objects whose string representation contains a specified substring.
 * Only the test() function and constructor has been properly implemented.
 * Note that the functions that return a predicate (and, negate, or) simply return null, so don't use these functions.
 */
public class TextualFilterPredicate implements Predicate<Object> {
    String filter;

    /**
     * Constructs the TextualFilterPredicate
     *
     * @param substring The substring that all accepted strings must contain
     */
    public TextualFilterPredicate(String substring) {
        filter = substring;
    }

    /**
     * {@inheritDoc}
     *
     * @return True if the test string contains this Predicate's substring, false otherwise
     */
    @Override
    public boolean test(Object testString) {
        return testString.toString().toLowerCase().contains(filter.toLowerCase());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate<Object> and(Predicate<? super Object> other) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate<Object> negate() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate<Object> or(Predicate<? super Object> other) {
        return null;
    }
}
