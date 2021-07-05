/**
 * Runs a function once after some requirements (e.g. element exists in DOM) have been met, polling the requirement function at regular intervals
 * @param {() => boolean} loadTest once the load requirements are met, this function should return true
 * @param {() => boolean)} onLoad function that is run once the load requirement is met
 * @param {number} interval how often the `loadTest` method is run in milliseconds. Default 100
 * @param {number} how many times the test can run before it gives up. If 0 or negative, will never give up
 * @param {() => void } afterStop method that is run after it gives up
 */
let waitFor = (loadTest, onLoad, interval = 100, stopAfter = 50, afterStop = () => {}) => {
    counter = 0;
    let timer = window.setInterval(() => {
        if (loadTest()) {
            window.clearInterval(timer);
            onLoad();
        }
        counter++;
        if (stopAfter > 0 && counter >= stopAfter) {
            window.clearInterval(timer);
            afterStop();
        }
    }, interval);
}


/**
 * Generates a random integer
 * @param {integer} min minimum integer that can be generated, inclusive
 * @param {integer} max maximum integer that can be generated, exclusive
 */
let randInt = (min, max) => Math.floor(Math.random() * (max - min)) + min;

/**
 * Encodes an SVG as a base64 encoded url
 * @param {string} svg SVG to encode
 */
let svgToUrl = (svg) => "data:image/svg+xml;charset=UTF-8," + encodeURIComponent(svg);

/**
 * Generates a string containing code for a SVG circle
 * @param {String} fillColor some valid rgb/rgba/hex/named color string for the fill color of the circle
 * @param {number} size diameter of the circle
 */
let circleSvg = (fillColor, size) => {
  return `
  <svg version="1.1" baseProfile="full" width="${size}" height="${size}" xmlns="http://www.w3.org/2000/svg">
      <circle cx="${size/2}" cy="${size/2}" r="${size/2}" fill="${fillColor}"/>"
  </svg>`;
}

const toRad = Math.PI / 180; // multiply val in degrees by this
const toDegrees = 180 / Math.PI; // multiply val in radians by this


/**
 * Calculates the distance between two points on Earth
 * @param {LatLng} point1 
 * @param {LatLng} point2 
 */
let distanceBetweenPoints = (point1, point2) => {
    // https://stackoverflow.com/a/18883819
    var R = 6371; // km
    var deltaLat = (point2.lat() - point1.lat()) * toRad;
    var deltaLng = (point2.lng() - point1.lng()) * toRad;

    var lat1 = point1.lat() * toRad;
    var lat2 = point2.lat() * toRad;

    var a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
      Math.sin(deltaLng/2) * Math.sin(deltaLng/2) * Math.cos(lat1) * Math.cos(lat2); 
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
    var d = R * c;
    return d;
}


class Rectangle {
    /**
     * 
     * @param {{x, y}}} center 
     * @param {*} width 
     * @param {*} height 
     */
    constructor(centerX, centerY, width, height) {
        this.left = centerX - width/2;
        this.right = centerX + width/2;
        this.top = centerY - height/2;
        this.bottom = centerY + height/2;
    }

    intersectionArea(rect) {
        // https://math.stackexchange.com/a/99576
        let x_overlap = Math.max(0, Math.min(this.right, rect.right) - Math.max(this.left, rect.left));
        let y_overlap = Math.max(0, Math.min(this.bottom, rect.bottom) - Math.max(this.top, rect.top));
        return x_overlap * y_overlap;
    }
}