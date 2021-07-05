class Popup extends google.maps.OverlayView {
    // https://developers.google.com/maps/documentation/javascript/examples/overlay-popup#maps_overlay_popup-javascript
    constructor(flight) {
        super();
        this.flight = flight;
        this.position = this.flight.route.getCenterPoint();
        this.pixelRectangle = new Rectangle(-1000, -1000, 1, 1); // Rectangle of space the div takes up
        this.currentOffset = 0.5;
        
        let div = document.createElement("div");
        div.innerHTML = this.flight.generatePopupHTMLAsString();

        div.classList.add("popup-bubble");

        // This zero-height div is positioned at the bottom of the bubble.
        const bubbleAnchor = document.createElement("div");
        bubbleAnchor.classList.add("popup-bubble-anchor");
        bubbleAnchor.appendChild(div);
        // This zero-height div is positioned at the bottom of the tip.
        this.containerDiv = document.createElement("div");
        this.containerDiv.classList.add("popup-container");
        this.containerDiv.appendChild(bubbleAnchor);
        // Optionally stop clicks, etc., from bubbling up to the map.
        Popup.preventMapHitsAndGesturesFrom(this.containerDiv);
    }
    /** Called when the popup is added to the map. */
    onAdd() {
        this.getPanes().floatPane.appendChild(this.containerDiv);

        this.containerDiv.addEventListener("click", () => {
            this.getMap().fitBounds(this.flight.route.getLatLngBounds());
        });
    }
    /** Called when the popup is removed from the map. */
    onRemove() {
        if (this.containerDiv.parentElement) {
            this.containerDiv.parentElement.removeChild(this.containerDiv);
        }
    }
    /** Called each frame when the popup needs to draw itself. */
    draw() {
        // Algorithm used to position items requires stable ordering: flight 1 drawn before flight 2 etc.
        let is_first = false;

        for(let flight of this.flight.trip.flights) {
            if (flight.isValid() && flight.popup != undefined) {
                // Can't just check it is the first flight as that flight could be invalid and so the popup will not be drawn
                is_first = flight == this.flight;
                break;
            }
        }

        if (is_first) {
            this.flight.trip.flights.forEach(flight=> {
                if (flight.isValid() && flight.popup != undefined) {
                    flight.popup.draw2();
                }
            });
        }

        // this.draw2();


    }
    draw2() {
        let divHeight = this.containerDiv.offsetHeight || 80;
        let divWidth = this.containerDiv.offsetWidth || 100;

        let offset = this.currentOffset;
        let lowerBoundsReached = false;
        let higherBoundsReached = false;
        let add = false;
        let delta = 0;
        let deltaDelta = 1/Math.max(10, this.flight.route.distance() / 40); // One point per 40 km
        // Searches growing outwards from current position
        // If its visible at the current point, don't move it. If not, try and find the position that completely shows all four corners of it that it nearest to the current point
        // Above is no longer true: if visible at current point and no overlap, don't move it
        // Items are now drawn such that flight 1 drawn before flight 2 etc., and any flights that are placed
        // will not be moved. Thus, the last flight is much more constrained than the first
        // This is not optimal but works
        let candidates = []; // {index, position, overlap} (prefer lower indexes)
        while(!lowerBoundsReached || !higherBoundsReached) {
            add = !add;
            offset += delta * (add?1:-1);
            delta += deltaDelta; // This needs to be below offset+= so that the first time it runs, offset runs as the original value
            if (offset <= 0) {
                lowerBoundsReached = true;
                continue;
            }
            if (offset >= 1) {
                higherBoundsReached = true;
                continue;
            }
            // d=0, dd=.1, x=.5: .5, .4, .6, .3, .7, .2, .8 ...

            // Get the point some fraction of the way along the path
            // Convert that to pixel coordinates, and find the earth coordinates
            // for the four corners (approximately) of the div, and use the first
            // point that has all four corners appearing
            let point = this.flight.route.getIntermediatePoint(offset);
            if (!map.getBounds().contains(point)) continue; // If center not inside the map, don't bother
             
            
            let projection = this.getProjection().fromLatLngToDivPixel(point);
            let corners = 0;
            for(let x of [-divWidth/2, divWidth/2]) {
                for(let y of [-divHeight/2, divHeight/2]) {
                    let corner = this.getProjection().fromDivPixelToLatLng({
                        x: projection.x + x,
                        y: projection.y + y
                    });
                    if (map.getBounds().contains(corner)) corners++;
                }
            }
            if (corners == 4) {
                let pixelRectangle = new Rectangle(projection.x, projection.y, divWidth, divHeight);
                
                let overlap = 0;
            
                for(let i = 0; i < this.flight.count - 1; i++) {
                    // console.log(i, this.flight.count)
                    // Get flights whose positions have been calculated: assume calculated in order
                    let flight = this.flight.trip.flights[i];
                    if (flight.popup != undefined) {
                        overlap += pixelRectangle.intersectionArea(flight.popup.pixelRectangle);
                    }
                }

                candidates.push({
                    index: candidates.length,
                    position: point,
                    offset: offset,
                    pixelRectangle: pixelRectangle,
                    overlap: overlap
                });
            }
        }

        // console.log(candidates.map(x=>x.offset));
        if (candidates.length) {
            candidates.sort((x, y) => {
                if (x == 0 && y == 0) {
                    return x.index - y.index; // lower index wins if there is no overlap
                    // Lower index means closer to initial position
                }
                return x.overlap - y.overlap;
            });
            // if (this.flight.count == 1) {
            //     // debugger;
            //     console.log(candidates.map(c=>`(${(c.pixelRectangle.left+c.pixelRectangle.right)/2}, ${(c.pixelRectangle.top + c.pixelRectangle.bottom) /2 })`));
            //     console.log(candidates.map(x=>x.overlap));
            // }

            // If there is a lot of overlap, give up
            if (candidates[0].overlap < 0.8 * divWidth * divHeight) {
                this.position = candidates[0].position;
                this.pixelRectangle = candidates[0].pixelRectangle;
                this.currentOffset = candidates[0].offset;
            }

        }

        const divPosition = this.getProjection().fromLatLngToDivPixel(
            this.position // minDistancePoint
        );
        // Hide the popup when it is far out of view.
        const display =
            Math.abs(divPosition.x) < 4000 && Math.abs(divPosition.y) < 4000
                ? "block"
                : "none";

        if (display === "block") {
            this.containerDiv.style.left = divPosition.x + "px";
            this.containerDiv.style.top = divPosition.y + "px";
        }

        if (this.containerDiv.style.display !== display) {
            this.containerDiv.style.display = display;
        }
    }
}