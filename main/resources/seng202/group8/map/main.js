let map;
let airportsDictionary = {}; // Key is the IATA or ICAO code
let airportsCluster;

let defaultBounds = new google.maps.LatLngBounds({lat: -40, lng: -180}, {lat: 40, lng: 180});


let routes = [
    // new Route("GKA", "MAG", "AIR")
];

const drawAllRoutes = false;
const loadRandomDataOnLoad = false;

let routesPolylines = [];

let trip;


/**
 * Sets the airports and updates the cluster
 * Only modifies airports that have changed for performance
 * @param {Airports[]} newAirports new list of airports. Existing airports are removed (if an exact match is not found)
 */
let setAirports = (newAirports) => {
    let unusedKeys = new Set(Object.keys(airportsDictionary));
    let markersToAdd = new Set(); // Use sets to remove duplicates as both IATA and ICAO are being used as keys
    let markersToRemove = new Set();
    
    newAirports.forEach(airport => {
        if (unusedKeys.has(airport.iata)) {
            unusedKeys.delete(airport.iata);
            let iataAirport = airportsDictionary[airport.iata];
            if (!iataAirport.equals(airport)) {
                // Replace operation
                markersToRemove.add(iataAirport.marker);
                markersToAdd.add(airport.marker);
                
                airportsDictionary[airport.iata] = airport;
            }
        } else {
            // Insert operation
            markersToAdd.add(airport.marker);
            airportsDictionary[airport.iata] = airport;
        }

        if (unusedKeys.has(airport.icao)) {
            unusedKeys.delete(airport.icao);
            let icaoAirport = airportsDictionary[airport.icao];
            if (!icaoAirport.equals(airport)) {
                // Replace operation
                markersToAdd.add(icaoAirport.marker);
                markersToAdd.add(airport.marker);
                airportsDictionary[airport.icao] = airport;
            }
        } else {
            // Insert operation
            markersToAdd.add(airport.marker);
            airportsDictionary[airport.icao] = airport;
        }
    });

    // Delete unused keys
    unusedKeys.forEach(key => {
        markersToRemove.add(airportsDictionary[key].marker);
        delete airportsDictionary[key];
    });
    
    // console.log(`Removing ${markersToRemove.size} airportsToAdd, adding ${markersToAdd.size}`);
    airportsCluster.removeMarkers(Array.from(markersToRemove));
    airportsCluster.addMarkers(Array.from(markersToAdd));
    
}


/**
 * Sets the routes array
 * @param {Route} newRoutes new array of routes objects
 */
let setRoutes = newRoutes => {
    routes = newRoutes;
    // routes.forEach(route => route.getAirports(airportsDictionary));
    routes.forEach(route => {
        route.getAirports(airportsDictionary);

        if (drawAllRoutes && route.isValid()) {
            route.generatePolyline().setMap(map);
        }
    })
}

/*
Airports that are part of routes that are part of the trip should not be clustered
Routes that are part of the trip should have a popup thing
*/
let setTrip = newTrip => {
    // Trip airports are not part of the cluster; need to remove them separately
    let markersToRemove = new Set();
    if (trip != undefined) {
        trip.flights.forEach(flight => {
            if (flight.isValid() && flight.route.isValid()) {
                flight.route.sourceAirport.marker.setMap(null);
                flight.route.destinationAirport.marker.setMap(null);
            }

            if (flight.popup != undefined) {
                flight.popup.setMap(null);
            }
        });
    }


    trip = newTrip;

    routesPolylines.forEach(polyline => polyline.setMap(null)); // Hide existing routes
    routesPolylines = [];


    let airportsToAdd = new Set(); // Airports that need to be added to the map (i.e. their markers)

    for(let i = trip.flights.length - 1; i >= 0; i--) {
        // Need to do this backwards as the draw method dies if any aren't on the map when the popup
        // for the first flight is drawn
        let flight = trip.flights[i];
        flight.getRoute(routes);
        if (flight.isValid()) {
            flight.route.getAirports(airportsDictionary);
            if (flight.route.isValid()) {
                // Route and airports both have to exist so two checks
                let polyline = flight.route.generatePolyline();
                polyline.setMap(map);
                routesPolylines.push(polyline);

                let popup = flight.generatePopup();
                popup.setMap(map);

                airportsToAdd.add(flight.route.sourceAirport);
                airportsToAdd.add(flight.route.destinationAirport);
            }
        }
    }

    airportsCluster.removeMarkers(Array.from(airportsToAdd).map(airport => airport.marker));
    airportsToAdd.forEach(airport => {
        airport.marker.setMap(map);
        airport.setIcon("rgb(255, 155, 0)"); // Different color for these markers
    });

    let bounds = new google.maps.LatLngBounds();
    let validFlightExists = false;
    trip.flights.forEach(flight => {
        if (flight.isValid() && flight.route.isValid()) {
            validFlightExists = true;
            bounds.extend(flight.route.sourceAirport.latLng);
            bounds.extend(flight.route.destinationAirport.latLng);
        }
    });

    if (!validFlightExists) {
        bounds = defaultBounds;
    }
    map.fitBounds(bounds);

}


let setData = (newAirports, newRoutes, newTrip) => {

    // Hack; sometimes not all airports disappear
    airportsCluster.clearMarkers();
    airportsDictionary = {};

    setAirports(newAirports);
    setRoutes(newRoutes);
    setTrip(newTrip);

    airportsCluster.clearMarkers();
}




let init = () => {
    map = new google.maps.Map(document.getElementById("map"), {
        // center: { lat: -43.521455, lng: 172.583922 }, // Defaults to Eng Core
        zoom: 3,
        mapId: "c0cc7ca281926b3a", // Gets rid of POI, but doesn't work for some reason. Maybe need billing info
        styles: [
            {
                "featureType": "poi",
                "stylers": [
                    {
                        "visibility": "off"
                    }
                ]
            },
            {
                "featureType": "poi.park",
                "stylers": [
                    {
                        "visibility": "on"
                    }
                ]
            }
        ],
        minZoom: 2,
        zoomControl: true,
        mapTypeControl: false,
        scaleControl: true,
        streetViewControl: false,
        rotateControl: false,
        fullscreenControl: false
    });

    map.fitBounds(defaultBounds);

    airportsCluster = new MarkerClusterer(
        map,
        [], // Initialize with no airportsToAdd
        {
            // MarkerCluster allows for three different icons depending on the number of airportsToAdd in the cluster. Currently using the same for all three
            styles: [
                {
                    textColor: "white",
                    url: svgToUrl(circleSvg("rgba(0, 162, 211, 0.7)", 20)),
                    height: 20,
                    width: 20
                },
                {
                    textColor: "white",
                    // url: svgToUrl(circleSvg("rgba(255, 155, 0, 0.7)", 20)),
                    url: svgToUrl(circleSvg("rgba(0, 162, 211, 0.7)", 20)),
                    height: 20,
                    width: 20
                },
                {
                    textColor: "white",
                    // url: svgToUrl(circleSvg("rgba(255, 105, 105, 0.7)", 20)),
                    url: svgToUrl(circleSvg("rgba(0, 162, 211, 0.7)", 20)),
                    height: 20,
                    width: 20
                }
            ]
        }
    );

    // Get rid of this with a proper API key and billing setup
    hideNag();

    waitFor(() => typeof java != "undefined", () => java.onLoad());
    // When the Java communication object gets instantiated, call the onLoad method

    if (loadRandomDataOnLoad) {
        loadRandomData();
    }
    
    // setData([new Airport('Sochi International Airport', 'AER', 'URSS', 43.449902, 39.956600), new Airport('Mount Hagen Kagamuga Airport', 'HGU', 'AYMH', -5.826790, 144.296005), new Airport('Kazan International Airport', 'KZN', 'UWKD', 55.606201, 49.278702), new Airport('Goroka Airport', 'GKA', 'AYGA', -6.081690, 145.391998)],[new Route('AER', 'KZN', '2B', 0, ['CR2']), new Route('GKA', 'HGU', 'CG', 20, ['DH8', 'DHT'])],new Trip('ASDASDASD', 'A descriptive comment', [new Flight('null', 'ABC', 'DEF', 'AB', new Date(1599702000000)), new Flight('null', 'DEF', 'FGH', 'DC', new Date(1599702000000)), new Flight('null', 'AER', 'KZN', '2B', new Date(1599702000000)), new Flight('null', 'GKA', 'HGU', 'CG', new Date(1599702000000)), new Flight('null', 'ABC', 'DEF', 'CG', new Date(1599702000000))]));

}



let loadRandomData = () => {
    let tempAirports = [
        new Airport('Goroka Airport', 'GKA', 'AYGA', -6.081690, 145.391998),
        new Airport('Madang Airport', 'MAG', 'AYMD', -5.207080, 145.789001),
        new Airport('Mount Hagen Kagamuga Airport', 'HGU', 'AYMH', -5.826790, 144.296005),
        new Airport('Nadzab Airport', 'LAE', 'AYNZ', -6.569803, 146.725977),
        new Airport('Port Moresby Jacksons International Airport', 'POM', 'AYPY', -9.443380, 147.220001),
        new Airport('Wewak International Airport', 'WWK', 'AYWK', -3.583830, 143.669006)
    ];
    
    for (let i = 0; i < 10000; i++) {
        tempAirports.push(Airport.generateRandomAirport());
    }
    setAirports(tempAirports);
    
    let tempRoutes = [];
    //     new Route("GKA", "MAG", "AIR", 40, ["777", "320"]),
    //     new Route("MAG", "GKA", "AIR", 40, ["777", "320"]),
    //     new Route("GKA", "LAE", "AIR", 40, ["777", "320"]),
    //     new Route("LAE", "MAG", "AIR", 40, ["777", "320"]),
    // ]
    for(let i = 0; i < 1000; i++) {
        tempRoutes.push(Route.generateRandomRoute(airportsDictionary));
    }

    setRoutes(tempRoutes);

    // setTrip(new Trip("Trip Name", "Trip Comment", [
    //     new Flight("Flight 1 comment", "GKA", "MAG", "AIR", new Date(Date.now())),
    //     new Flight("Flight 2 comment", "MAG", "GKA", "AIR", new Date(Date.now() + 1000 * 60 * 100)),
    //     new Flight("Flight 3 comment", "GKA", "LAE", "AIR", new Date(Date.now() + 1000 * 60 * 400)),
    //     new Flight("Flight 4 comment", "LAE", "MAG", "AIR", new Date(Date.now() + 1000 * 60 * 500)),
    // ]));
    // setTrip(Trip.generateRandomTrip(routes, airportsDictionary));
}

/**
  * Hides the 'This page can't load Google Maps correctly.' popup
  * Billing needs to be enabled but I can't be bothered doing that
  */
let hideNag = () => {
    let count = 0;
    let timer = window.setInterval(() => {
        if (count++ > 30) { // After 3 seconds bail
            window.clearInterval(timer);
        }
        let button = document.querySelector(".dismissButton");
        if (button != null) {
            button.click();
            window.clearInterval(timer);
        }
    }, 100);
}


init();

// For testing only. Used this to view all airports and routes on the map
let setData2 = (airports, routes) => {
    let markers = [];
    airportsDictionary = {};
    airports.forEach(airport => {
        markers.push(airport.marker);
        if (airport.icao.length != 0) airportsDictionary[airport.icao] = airport;
        if (airport.iata.length != 0) airportsDictionary[airport.iata] = airport;
    });

    airportsCluster.addMarkers(Array.from(markers));
    routes.forEach(route => {
        route.getAirports(airportsDictionary);
        if (route.isValid()) {
            route.generatePolyline().setMap(map);
        }
    });
}
