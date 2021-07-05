package seng202.group8;

import seng202.group8.io.Database;
import seng202.group8.viewcontrollers.RootController;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Where the program starts
 */
public class Main {
    public static boolean javaFXInitialized = false;

    public static void main(String[] args) {
        try {
            boolean dbSet = false;
            if (args.length == 1) {
                if (Arrays.stream(new String[]{"help", "--help"}).anyMatch(args[0].toLowerCase()::contains)) {
                    // https://stackoverflow.com/a/8832866
                    System.out.println(
                            "java -jar ./seng202_2020_team8_phase3.jar [database]\nUsage Notes:\n" +
                            "  The path to a database can optionally be specified; otherwise, the default database, `./database.db` will be used.\n" +
                            "  If the specified file does not exist, the file will be created.\n" +
                            "  If the specified file is not a valid database, the program will use the default database.\n" +
                            "  If the default database is invalid, the program will exit\n"
                    );
                    return;
                }

                File databaseFile = new File(args[0]);
                Database.setDatabasePath(databaseFile.toURI());
                dbSet = true;
            }

            if (!dbSet) Database.setDatabasePath(); // set to default
        } catch (IOException | URISyntaxException e) {
            AlertHelper.showGenericErrorAlert(e, true,
                "Could not load database",
                "Check if the given path to the database is valid",
                AlertHelper.sendReportToDevWithStacktraceString,
                Database.ERROR_CODE_FATAL_ERROR_ON_DATABASE_LOAD
            );
        } catch(SQLException e) {
            Database.exitProgramOnSetDatabasePathException(e);
        }

        Database.establishConnection();

        RootController.main(args);
    }
}
