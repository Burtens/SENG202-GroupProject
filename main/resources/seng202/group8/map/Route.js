class Route {
    constructor(sourceCode, destinationCode, airlineCode, duration, planeTypes) {
        this.sourceCode = sourceCode;
        this.destinationCode = destinationCode;
        this.airlineCode = airlineCode;
        this.duration = duration;
        this.planeTypes = planeTypes;

        this.sourceAirport = undefined;
        this.destinationAirport = undefined;
    }

    /**
     * Checks that the source and destination airports are not undefined (i.e. references to objects exist)
     */
    isValid() {
        // Valid if source and destination airports  (Airport object, not code) are defined
        return this.sourceAirport != undefined && this.destinationAirport != undefined;
    }

    /**
     * Get the source and destination airport objects from the `airports` array
     */
    getAirports(airportsDictionary) {
        this.sourceAirport = airportsDictionary[this.sourceCode];
        this.destinationAirport = airportsDictionary[this.destinationCode];
    }

    /**
     * Generates a polyline connecting the source and destination airports
     */
    generatePolyline() {
        return new google.maps.Polyline({
            path: [this.sourceAirport.latLng, this.destinationAirport.latLng],
            geodesic: true,
            strokeColor: "rgb(255, 155, 0)",
            strokeOpacity: 1,
            strokeWeight: 2
        });
    }

    /**
     * Get the approximate bounds for the route between the two airports
     */
    getLatLngBounds() {
        let bounds = new google.maps.LatLngBounds();
        // If the flight is big just having the two end points will cut off part of the route
        for(let i = 0; i <= 1; i+= 0.1) {
            bounds.extend(this.getIntermediatePoint(i));
        }
        return bounds;
    }

    /**
     * Calculates the distance between the two airports
     */
    distance() {
        return distanceBetweenPoints(this.sourceAirport.latLng, this.destinationAirport.latLng);
    }

    /**
     * Gets the center point between the two airports of the route
     */
    getCenterPoint() {
        return this.getIntermediatePoint(0.5);
    }

    getIntermediatePoint(fraction) {
        // https://www.movable-type.co.uk/scripts/latlong.html/
        const lat1 = this.sourceAirport.latLng.lat() * toRad;
        const lat2 = this.destinationAirport.latLng.lat() * toRad;
        const deltaLat = lat2 - lat1;
        
        const lng1 = this.sourceAirport.latLng.lng() * toRad;
        const lng2 = this.destinationAirport.latLng.lng() * toRad;
        const deltaLng = lng2 - lng1;

        const a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng/2) * Math.sin(deltaLng/2);
        const delta = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        const A = Math.sin((1-fraction)*delta) / Math.sin(delta);
        const B = Math.sin(fraction*delta) / Math.sin(delta);

        const x = A * Math.cos(lat1) * Math.cos(lng1) + B * Math.cos(lat2) * Math.cos(lng2);
        const y = A * Math.cos(lat1) * Math.sin(lng1) + B * Math.cos(lat2) * Math.sin(lng2);
        const z = A * Math.sin(lat1) + B * Math.sin(lat2);

        const lat3 = Math.atan2(z, Math.sqrt(x*x + y*y));
        const lng3 = Math.atan2(y, x);

        return new google.maps.LatLng({
            lat: lat3 * toDegrees,
            lng: lng3 * toDegrees
        });
    }


    static generateRandomRoute(airportsDictionary) {
        let keys = Object.keys(airportsDictionary);
        
        let origin = airportsDictionary[keys[randInt(0, keys.length)]];
        let destination = airportsDictionary[keys[randInt(0, keys.length)]];
        let planeTypes = ["777", "320"];
        let route = new Route(origin.code, destination.code, "RND", 0, planeTypes);

        route.sourceAirport = origin;
        route.destinationAirport = destination;
        route.duration = route.distance() * 60 / 500;
        route.price = 75 + route.distance() * 0.15;
        return route;
    }
}