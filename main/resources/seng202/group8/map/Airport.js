/**
 * Generates an SVG image in the form of a base64 encoded URL to mark the location of an airport
 * @param {String} fillColor some valid rgb/rgba/hex/named color string for the fill color of the sign
 * @param {String} strokeColor some valid rgb/rgba/hex/named color string for the outer post color of the sign
 */
let customMarker = (fillColor, strokeColor) => {
    let size = 40;
    let strokeWidth = 2;
    let borderRadius = 2;
    let height = 20;
    return `
    <svg version="1.1" baseProfile="full" width="${size}" height="${size}" xmlns="http://www.w3.org/2000/svg">
        <rect x="${strokeWidth}" y="${strokeWidth}" width="${size - strokeWidth * 2}" height="${height - strokeWidth * 2}" rx="${borderRadius}" fill="${fillColor}" stroke="${strokeColor}" stroke-width="${strokeWidth}"></rect>
        <line x1="${size / 2}" x2="${size / 2}" y1="${height - strokeWidth}" y2="${size}" stroke="${strokeColor}" stroke-width="${strokeWidth}"></line>
    </svg>`;
}


class Airport {
    constructor(name, iata, icao, latitude, longitude) {
        this.name = name;
        this.iata = iata;
        this.icao = icao;
        this.code = (iata == undefined || iata == "null" || iata.length == 0)? this.icao: this.iata; // Code prefers the IATA if both are available

        this.latLng = new google.maps.LatLng({
            lat: latitude,
            lng: longitude
        });

        // Don't update marker since the object has to remain the same for insertion and deletion
        this.marker = new google.maps.Marker({
            position: this.latLng,
            label: {
                text: this.code,
                fontFamily: "monospace"
            },
            title: this.name
        });

        this.setIcon("rgb(0, 162, 211)");
    }

    /**
     * Sets the icon for the marker to a custom color
     * @param {String} strokeColor some valid rgb/rgba/hex/named color string for the fill color of the sign
     * @param {String} backgroundColor some valid rgb/rgba/hex/named color string for the background color of the sign. Defaults to white
     */
    setIcon(strokeColor, backgroundColor = "white") {
        this.marker.setIcon({
            labelOrigin: new google.maps.Point(
                this.code.length == 3? 19: 16, 10 // Center the text depending on if it is an IATA or ICAO code. Monospace font so should be consistent
            ),
            url: svgToUrl(customMarker(backgroundColor, strokeColor)),
        });
    }


    static generateRandomAirport() {
        function makeid(length) {
            // https://stackoverflow.com/a/1349426
            var result           = '';
            var characters       = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
            var charactersLength = characters.length;
            for ( var i = 0; i < length; i++ ) {
               result += characters.charAt(Math.floor(Math.random() * charactersLength));
            }
            return result;
        }

        return new Airport(makeid(10), makeid(3).toUpperCase(), makeid(4).toUpperCase(), randInt(-90, 90), randInt(-180, 180));
    }

    /**
     * Checks if two airports are identical in property values
     * @param {Airport} airport airport to compare to
     */
    equals(airport) {
        return  this.name == airport.name &&
                this.iata == airport.iata &&
                this.icao == airport.icao &&
                this.latitude == airport.latitude &&
                this.longitude == airport.longitude;
    }
}