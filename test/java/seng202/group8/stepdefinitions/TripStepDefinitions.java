package seng202.group8.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import seng202.group8.data.Trip;
import seng202.group8.datacontroller.TripDataController;
import seng202.group8.io.Import;

public class TripStepDefinitions {
    TripDataController tripDC = TripDataController.getSingleton();
    Trip selectedTrip;
    Trip importedTrip;
    Exception thrownException;
    String actualAnswer;

    public void isImported() {
        if (importedTrip == null) {
            if (selectedTrip == tripDC.getCurrentlyOpenTrip()) {
                actualAnswer = "Trip is not imported";
            }
        } else {
            actualAnswer = "Trip is imported";
        }
    }

    @Given("The trip file {string} exists in directory {string}")
    public void filePathExistsInDirectory(String filePath, String dirPath){

    }

    @When("User imports {string}")
    public void userImportsFile(String filePath) {
        selectedTrip = tripDC.getCurrentlyOpenTrip();
        try {
            importedTrip = Import.importTrip(filePath);
        } catch (Exception e) {
            thrownException = e;
        }
        isImported();
    }

    @Then("I should be told the status of the import {string}")
    public void i_should_be_told(String answer) {
        Assert.assertEquals(answer, actualAnswer);
    }
}
