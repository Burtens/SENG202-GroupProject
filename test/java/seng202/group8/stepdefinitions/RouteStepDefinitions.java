package seng202.group8.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.javatuples.Quartet;
import org.junit.Assert;
import seng202.group8.data.Route;
import seng202.group8.datacontroller.RouteDataController;
import seng202.group8.io.Database;
import seng202.group8.io.Import;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;

public class RouteStepDefinitions {
    RouteDataController routeDC = RouteDataController.getSingleton();
    Route selectedRoute;
    Quartet<Integer, Integer, Long, String> importedRoute;
    Exception thrownException;
    String actualAnswer;

    public void setup() throws IOException, SQLException, URISyntaxException {
        actualAnswer = "Route is imported";
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

        if (routeDC.getEntity(1) != null) {
            selectedRoute = routeDC.getEntity(1);
            if (routeDC.getEntity(selectedRoute.getSourceAirportCode(), selectedRoute.getDestinationAirportCode(),
                    selectedRoute.getAirlineCode()) != null) {
                actualAnswer = "Route is imported";
            }
        } else {
            actualAnswer = "Route is not imported";
        }
    }

    @Given("The route file {string} exists in directory {string}")
    public void existsInDirectory(String string, String string2){
    }


    @When("User imports route file named {string}")
    public void userImports(String filePath) throws SQLException, IOException, URISyntaxException {
        setup();
        try {
            importedRoute = Import.importData(filePath, "Route");
        } catch (Exception e) {
            thrownException = e;
        }
        isImported();
    }

    @Then("Status of the route import {string}")
    public void iShouldBeToldTheStatusOfTheImport(String answer) throws Exception {
        Assert.assertEquals(answer, actualAnswer);
        tearDown();
    }
}
