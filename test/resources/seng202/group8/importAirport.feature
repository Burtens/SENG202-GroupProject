Feature: Import Airport
  Scenario Outline: Airport is imported or not
    Given The airport file "<filename.csv>" exists in directory "target/test-classes/seng202/group8/Airport ExampleFiles/"
    When User imports airport file named "target/test-classes/seng202/group8/Airport Example Files/<filename.csv>"
    Then Status of the airport import "<answer>"
    Examples:
      | filename.csv | answer |
      | goodairport1.csv | Airport is imported |
      | goodairport2.csv | Airport is imported |
      | goodairport3.csv | Airport is imported |
      | invalidairport1.csv | Airport is not imported |
      | invalidairport2.csv | Airport is not imported |
      | invalidairport3.csv | Airport is not imported |