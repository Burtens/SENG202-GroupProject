Feature: Import Airline
  Scenario Outline: Airline is imported or not
    Given The airline file "<filename.csv>" exists in directory "target/test-classes/seng202/group8/Airline Example Files/"
    When User imports airline file named "target/test-classes/seng202/group8/Airline Example Files/<filename.csv>"
    Then Status of the airline import "<answer>"
    Examples:
      | filename.csv | answer |
      | goodairline1.csv | Airline is imported |
      | goodairline2.csv | Airline is imported |
      | goodairline3.csv | Airline is imported |
      | badairline1.csv  | Airline is not imported |
      | badairline2.csv  | Airline is not imported |
      | badairline3.csv  | Airline is not imported |
