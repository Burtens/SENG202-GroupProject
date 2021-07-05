# 2020 SENG202 Group 8 README for Phase 3

Authors: Eric Song, Rio Ogino, Mitchell Veale, Samuel Burtenshaw, Josh Egan, and Niels van Antwerpen

## Installation

- Java 11 or later is required and must be in `PATH`
- All necessary resources for the program are contained in `seng202_2020_team8_phase3.jar`

## Deployment

To import it as an IntelliJ project:

- Click File, New, Project From Existing Sources, then select the folder
- The Maven extension will need to be enabled
- In Project Settings:
    - In Project:
      - Ensure the Project SDK and language level are both set to 11
    - In Modules, ensure that:
      - The language level is set to 11
      - The source folder is set to `src\main\java`
      - The test source folder is set to `src\test\java`
      - The resource folder is set to `src\main\resources`
      - The test resource source folder is set to `src\test\resources`
      - The `target` folder is excluded

To build from source from the command line:
- [Maven](https://maven.apache.org/index.html) is used to manage dependencies and is required for the build process
- If the file `database.db` exists in the root directory of the project, ensure it can be opened by the program. Otherwise, tests will fail
- Run `mvn package` to test and build the project
- A JAR file, `/target/seng202-project-{VERSION}-SNAPSHOT.jar`, is generated. This file contains all necessary resources to run the program

## Running the program

### From the command line

- To run the program, run the command `java -jar seng202_2020_group8_phase3.jar`
- A path to a database can optionally be provided:
  - If no database exists at the given location, a new database is created at the given path
  - If the database is invalid, it will fallback to using the default database, `./database.db`
- If `./database.db` is invalid, the program will print an error message and exit

### On Linux Mint

On some Linux Mint installations, you may be able to right click on the JAR file and click `Open With` and select a JDK distribution above 11.

In this case, the database will be created at `~/database.db`.

### Switching databases

To open a new database, click the button, 'Open Database' and select an appropriate file.
If the given file has no contents, it will create a new database in that location.
If the given file is not a valid database, it will attempt to revert to the previous database.

To create a new database, click the button, 'Create Database' and give the database an appropriate name

### Creating new airport, airline or route entries

- Navigate to the appropriate tab
- Click the '+ New Airport', '+ New Airline' or '+ New Route' button on the bottom of the window
- Enter the given fields with appropriate
- Click the save button; if any fields are invalid an error message will appear. Otherwise, it will save it and show it in the table view for that data type

### Importing airports, airlines, routes, or a trip

- Click on the 'Import Data' button near the top of the window
- Select the data type you would like to import from the drop down menu and click 'OK'
- When importing airports, airlines or routes:
  - The default file type is `.csv` (Comma-separated values)
  - Once imported, a pop-up will appear and showing the number of rows that successfully imported, the number of rows that failed, and the time taken to import
  - Any data successfully imported will appear in the corresponding table view
- When importing trips:
  - The default file type is `.mtyg`
  - Once imported, you will be given the option to name the trip
    - If a trip with the name already exists, you will be asked to rename it
    - If you click cancel, the import will be cancelled

### Trips

To add a flight to a trip:

- Ensure a trip has been loaded into the program:
  - Select the 'Trip' tab on the left hand side of the window
     - To load an existing trip, click 'Load Trip' and select one of the trips from the dropdown
     - To create a new trip, click 'New' and type in the name of the trip. It cannot match the name of an existing trip
- Go to the `Route` table tab and switch from the 'Trip' to the 'Details'
- Click on a route and in the details view, select a take off time
  - The route must be valid; the source and destination airports and airline must be present in the database
  - Click 'Add flight to trip' and select a takeoff date. It the flight occurs during an existing flight, an error will occur

Once the flight has been added, it should appear in the 'Trip' tab. A comment can be added to the flight by clicking on 'Comment', writing the comment and then clicking 'Save.'

By clicking on the 'Map' tab, the trip can be viewed visually, with airports, and information about the flights appearing on an overlay.

To export a trip, click on the 'Export' button in the 'Trip' tab and give it an appropriate name.

NOTE: if the file name has no extension the `.mtyg` extension is added automatically.
If a file with the same name and `.mtyg` extension exists in the same directory, it will be overwritten.
