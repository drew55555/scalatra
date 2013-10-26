var map;
var rectangle;
var infoWindow;
function initialize() {
  var mapOptions = {
    zoom: 4,
    center: new google.maps.LatLng(39.8282, -98.5795),
    disableDefaultUI : true,
    mapTypeId: google.maps.MapTypeId.ROADMAP
  };
  map = new google.maps.Map(document.getElementById('map-canvas'),
      mapOptions);

  var bounds = new google.maps.LatLngBounds(
      new google.maps.LatLng(35.0,-105.0),
      new google.maps.LatLng(44.0, -91.0)
   );

   rectangle = new google.maps.Rectangle({
       bounds: bounds,
       editable: true,
       draggable: true,
       geodesic: false
     });
   rectangle.setMap(map);

  google.maps.event.addListener(rectangle, 'bounds_changed', showNewRect);

  infoWindow = new google.maps.InfoWindow();


}

/** @this {google.maps.Rectangle} */
function showNewRect(event) {
  var ne = rectangle.getBounds().getNorthEast();
  var sw = rectangle.getBounds().getSouthWest();

  var contentString = '<b>Rectangle moved.</b><br>' +
      'New north-east corner: ' + ne.lat() + ', ' + ne.lng() + '<br>' +
      'New south-west corner: ' + sw.lat() + ', ' + sw.lng();

  // Set the info window's content and position.
  infoWindow.setContent(contentString);
  infoWindow.setPosition(ne);

  infoWindow.open(map);
}


google.maps.event.addDomListener(window, 'load', initialize);
