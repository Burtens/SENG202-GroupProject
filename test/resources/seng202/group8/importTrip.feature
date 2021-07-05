Feature: Import trip
  Scenario Outline: Trip is imported or it is not
    Given The trip file "<filename.mtyg>" exists in directory "target/test-classes/seng202/group8/Trip Example Files/"
    When User imports "target/test-classes/seng202/group8/Trip Example Files/<filename.mtyg>"
    Then I should be told the status of the import "<answer>"
    Examples:
      | filename.mtyg| answer|
      | missingcomma.mtyg| Trip is not imported |
      | invaliddata1.mtyg | Trip is not imported |
      | invaliddata2.mtyg | Trip is not imported |
      | goodtrip1.mtyg     | Trip is imported     |
      | goodtrip2.mtyg     | Trip is imported     |
      | goodtrip3.mtyg     | Trip is imported     |
