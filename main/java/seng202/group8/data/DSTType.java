package seng202.group8.data;

/**
 * Enum conforming to OpenFlights daylight saving types
 * Includes functions mapping the character code for the DST type to an enum value, and vice versa
 */
public enum DSTType {
    EUROPE, US_CANADA, SOUTH_AMERICA, AUSTRALIA, NEW_ZEALAND, NONE, UNKNOWN;

    /**
     * Generates DSTType enum value from a given character (one of 'E', 'A', 'S', "O', 'Z' and 'N')
     *
     * @param code; character denoting OpenFlights type.
     * @return DSTType enum value. If unknown, returns UNKNOWN
     */
    public static DSTType fromCode(char code) {
        switch (Character.toUpperCase(code)) {
            case 'E':
                return EUROPE;
            case 'A':
                return US_CANADA;
            case 'S':
                return SOUTH_AMERICA;
            case 'O':
                return AUSTRALIA;
            case 'Z':
                return NEW_ZEALAND;
            case 'N':
                return NONE;
            default:
                return UNKNOWN;
        }
    }

    /**
     * Generates char from DSTType in same format as OpenFlights
     *
     * @param type DSTType enum value denoting region
     * @return char character the DSTType maps to
     */
    public static char toCode(DSTType type) {
        switch (type) {
            case EUROPE:
                return 'E';
            case US_CANADA:
                return 'A';
            case SOUTH_AMERICA:
                return 'S';
            case AUSTRALIA:
                return 'O';
            case NEW_ZEALAND:
                return 'Z';
            case NONE:
                return 'N';
            default:
                return 'U';
        }
    }
}
