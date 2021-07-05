package seng202.group8.io;

import com.opencsv.CSVWriter;
import seng202.group8.data.Trip;
import seng202.group8.data.TripFlight;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports trip from database to a file in chosen location
 */
public class Export {

    /**
     * Exports trip to a MTYG file in chosen file location
     *
     * @param trip     Trip object to save
     * @param filePath the file path to save the file to
     * @throws IOException if given file path is invalid
     */
    public static void exportTrip(Trip trip, String filePath) throws IOException {
        File tripFile = new File(filePath);
        CSVWriter fileWriter = new CSVWriter(new FileWriter(tripFile, Import.FILE_ENCODING));
        List<String[]> data = new ArrayList<>();

        data.add(new String[]{trip.getName(), trip.getComment()});
        for (TripFlight flight : trip.getFlights()) {
            data.add(new String[]{flight.getAirlineCode(), flight.getSourceCode(), flight.getDestinationCode(), Integer.toString(flight.getTakeoffTime()), flight.getTakeoffDate().toString(), flight.getComment()});
        }

        fileWriter.writeAll(data);
        fileWriter.close();
    }
}
