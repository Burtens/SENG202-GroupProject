package seng202.group8.data;

import java.time.*;

/**
 * Defines some helper functions for working with DateTime objects
 */
public class DateTimeHelpers {
    /**
     * UTC Zone for when using ZonedDateTime
     */
    public static ZoneId utcZone = ZoneId.of("Z");

    /**
     * Generates a UTC time from a local date and time
     *
     * @param date          local time
     * @param timeInMinutes time in minutes from midnight
     * @return zoned DateTime in UTC timezone
     */
    public static ZonedDateTime generateUTCDateTime(LocalDate date, int timeInMinutes) {
        LocalDateTime localDateTime = LocalDateTime.of(date, timeInMinutesToLocalTime(timeInMinutes));
        return ZonedDateTime.of(localDateTime, utcZone);
    }

    /**
     * Generates a UTC time
     *
     * @param year       the year to represent, from MIN_YEAR to MAX_YEAR
     * @param month      the month-of-year to represent, from 1 (January) to 12 (December)
     * @param dayOfMonth the day-of-month to represent, from 1 to 31
     * @param hour       the hour-of-day to represent, from 0 to 23
     * @param minute     the minute-of-hour to represent, from 0 to 59
     * @return UTC zoned date time
     */
    public static ZonedDateTime generateUTCDateTime(int year, int month, int dayOfMonth, int hour, int minute) {
        return ZonedDateTime.of(year, month, dayOfMonth, hour, minute, 0, 0, utcZone);
    }

    /**
     * Converts the time in minutes from midnight to a local time
     *
     * @param timeInMinutes time in minutes from midnight
     * @return local time object
     */
    public static LocalTime timeInMinutesToLocalTime(int timeInMinutes) {
        return LocalTime.of(timeInMinutes / 60, timeInMinutes % 60, 0, 0);
    }
}
