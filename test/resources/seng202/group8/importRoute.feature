Feature: Import Route
  Scenario Outline: Route is imported or not
    Given The route file "<filename.csv>" exists in directory "target/test-classes/seng202/group8/Route ExampleFiles/"
    When User imports route file named "target/test-classes/seng202/group8/Route Example Files/<filename.csv>"
    Then Status of the route import "<answer>"
    Examples:
      | filename.csv | answer |
      | goodroute1.csv | Route is imported |
      | goodroute2.csv | Route is imported |
      | goodroute3.csv | Route is imported |
      | invalidroute1.csv | Route is not imported |
      | invalidroute2.csv | Route is not imported |
      | invalidroute3.csv | Route is not imported |