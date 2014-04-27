var PI = 3.1415926;
var RADIUS = 250;
var X_CENTER = 320;
var Y_CENTER = 320;
var greyEndpointOptions = {
        endpoint:["Dot", {radius:5}],
        paintStyle:{ width:25, height:21, fillStyle:'#666' },
        isSource:true,
        isTarget:true,
        anchor:[ "Perimeter", { shape:"Circle" } ],
        connector: ["Straight"],
        connectorStyle: { lineWidth:1, strokeStyle:'#666'},

};

var MACHINE_CONNECTIONS_ENDPOINT = "/machine_connections.json"
var MACHINE_DESCRIPTION_ENDPOINT = "/machine_descriptions.json"
// This will store what nodes run on each machine
var machine_info_map = new Object();


/* Given the total number of points on the circumference of a circle,
 * the index of a point, and the radius of the circle, return an array
 * for the x and y coordinates for that point.
 */
function calculate_coords(total, index, radius) {
	theta = index / total * 2 * PI;
	x = radius * Math.cos(theta);
	y = radius * Math.sin(theta);
	return [Math.round(x), Math.round(y)];
}


jsPlumb.ready(function(){
        var connector_map = new Object();

        $.getJSON(MACHINE_CONNECTIONS_ENDPOINT, function( machine_edges ) {		
		var total = Object.keys(machine_edges).length;
		var index = 0;		

		for (var src_m in machine_edges) {
			// Compute the circle's coordinates
			var coords = calculate_coords(total, index, RADIUS);
			var x_offset = X_CENTER + coords[0];
			var y_offset = Y_CENTER + coords[1];

			// Add the machine's div to #container			
			var dest_obj = machine_edges[src_m];
			var machine = $("<div></div>").attr('id', src_m);
                        machine.addClass("machine");

			machine.addClass("square").css({"top":x_offset + "px", "left": y_offset + "px"});
			var machine_name = $("<h2></h2>").text(src_m);
			machine.append(machine_name)
			$('#container').append(machine);

			// Increment the counter for the total number of machines
			index += 1;
		}

		// Plot the edges
		for (var src_m in machine_edges) {	
			var dest_obj = machine_edges[src_m];
			for (var dest_m in dest_obj) {
			        if (dest_m == src_m){
			                continue;
			        }

				var e0 = jsPlumb.addEndpoint(src_m, greyEndpointOptions);
				var e1 = jsPlumb.addEndpoint(dest_m, greyEndpointOptions);
				jsPlumb.connect({source:e0, target:e1, anchors:"Left", connector:["Straight"], overlays: [["Arrow", {location:1, width:25}]]});
			}

		}

        });



});



$(document).ready( function() {
	$( "#hover-menu" ).draggable();

	// Generate machine_info_map
	$.getJSON(MACHINE_DESCRIPTION_ENDPOINT, function( machine_desc ) {

		for (var machine in machine_desc) {
			var desc = machine_desc[machine];
			window.machine_info_map[machine] = new Object();

			for (var lvl in desc) {
				window.machine_info_map[machine][lvl] = new Array();
				
				for (var node in desc[lvl]) {
					window.machine_info_map[machine][lvl].push(desc[lvl][node]["alias"])

				}
			}
		
		}
	});
});



$(document).on("mouseleave", ".square", function(){
	$(this).css('background-color', '#dddddd');
	$("#hover-menu").empty();
});


$(document).on("mouseenter", ".square", function(){
	$(this).css('background-color', '#545454');
	var machine_alias = $(this).attr('id');

	var title = $("<h1></h1>").text(machine_alias);
	title.addClass('hover-menu-heading');
	$("#hover-menu").append(title);
    
	$.each(window.machine_info_map[machine_alias], function(lvl, nodes) {
        	
		var level = $("<h3></h3>").text(lvl + ' nodes:');		
		level.addClass('hover-menu-level');
		$("#hover-menu").append(level);
		var list_of_nodes = "";
		$.each(nodes, function(idx, node) {
			list_of_nodes += node + ", "
		});
		list_of_nodes = list_of_nodes.substring(0, list_of_nodes.length - 2);
		var nodes_dom = $("<p></p>").text(list_of_nodes);
		$("#hover-menu").append(nodes_dom);
    	});

});









