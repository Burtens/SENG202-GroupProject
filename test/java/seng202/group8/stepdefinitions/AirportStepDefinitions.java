package seng202.group8.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.javatuples.Quartet;
import org.junit.Assert;
import seng202.group8.data.Airport;
import seng202.group8.datacontroller.AirportDataController;
import seng202.group8.io.Database;
import seng202.group8.io.Import;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;

public class AirportStepDefinitions {
    AirportDataController airportDC = AirportDataController.getSingleton();
    Airport selectedAirport;
    Quartet<Integer, Integer, Long, String> importedAirport;
    Exception thrownException;
    String actualAnswer;

    public void setup() throws IOException, SQLException, URISyntaxException {
        Database.establishConnection();
        Database.setDatabasePath(Paths.get(new File("target/test-classes/seng202/group8/").getCanonicalPath(), "cucumberDB").toUri());
    }

    public void tearDown() throws Exception {
        Database.databaseConnection.close();
        File db = new File("target/test-classes/seng202/group8/cucumberDB");
        assertTrue(db.delete());
        Database.setDatabasePath();
    }

    public void isImported() throws SQLException {

        if (airportDC.getAllEntities().size() > 0) {
            actualAnswer = "Airport is imported";
        } else {
            actualAnswer = "Airport is not imported";
        }
    }

    @Given("The airport file {string} exists in directory {string}")
    public void existsInDirectory(String string, String string2){
    }


    @When("User imports airport file named {string}")
    public void userImports(String filePath) throws SQLException, IOException, URISyntaxException {
        setup();
        try {
            importedAirport = Import.importData(filePath, "Airport");
        } catch (Exception e) {
            thrownException = e;
        }
        isImported();
    }

    @Then("Status of the airport import {string}")
    public void iShouldBeToldTheStatusOfTheImport(String answer) throws Exception {
        Assert.assertEquals(answer, actualAnswer);
        tearDown();
    }
}
