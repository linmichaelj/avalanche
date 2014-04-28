var exampleGreyEndpointOptions = {
        endpoint:["Dot", {radius:5}],
        paintStyle:{ width:25, height:21, fillStyle:'#666' },
        isSource:true,
        isTarget:true,
        anchor:[ "Perimeter", { shape:"Circle" } ],
        connector: ["Straight"],
        connectorStyle: { lineWidth:1, strokeStyle:'#666'}
};

var FORWARD_BACKWARD_PATH = '/forward_backward_path';

var node_info_map = new Object();

$(document).on("mouseenter", ".circle", function(){
    $(this).css('background-color', '#545454');
    var node_alias = $(this).attr('id');

    var title = $("<h3></h3>").text(node_alias)
    $("#hover-menu").append(title)
    $.each(node_info_map[node_alias], function(key, value) {
        var attribute = $("<p></p>").text(key + ": " + value);
        $("#hover-menu").append(attribute)
    });

    var url = FORWARD_BACKWARD_PATH + "?alias=" + node_alias
    $.getJSON(url, function( data ) {
        for (i in data['backward']){

            $("#" + data['backward'][i]).css('background-color', 'red');
        }

        for (i in data['forward']){
            $("#" + data['forward'][i]).css('background-color', 'green');
        }
    });


});

$(document).on("mouseleave", ".circle", function(){
    var node_alias = $(this).attr('id');
    $(this).css('background-color', '#dddddd');
    $("#hover-menu").empty();
    var url = FORWARD_BACKWARD_PATH + "?alias=" + node_alias

    $.getJSON(url, function( data ) {
        for (i in data['backward']){
            $("#" + data['backward'][i]).css('background-color', '#dddddd');
        }

        for (i in data['forward']){
            $("#" + data['forward'][i]).css('background-color', '#dddddd');
        }
    });

});

$(document).ready( function() {
    $( "#menu" ).draggable();
});


jsPlumb.ready(function(){
        var connector_map = new Object();
        jsPlumb.Defaults.Container = $("body");

        $.getJSON("/topology.json", function( data ) {
                x_offset = 0;
                y_offset = 0;
                max_height = 50;
                total_height = $(window).height();
                level_count = 0;
                $.each( data, function ( key, val ) {
                        var level = $("<div></div>").attr('id', key);
                        var level_content = $("<h1></h1>").text(key);

                        level.append(level_content);
                        level.addClass("level").css({"height": total_height + "px", "width":100 + "px", "left": x_offset + "px"});

                        $('#container').append(level);

                        var num_nodes = val["node"].length;
                        margin = 10;
                        var height = (total_height-40 - margin * num_nodes)/num_nodes;
                        console.log(level_count);
                        if (height > 80){
                            height = 80 + 10 * level_count;
                        }

                        var y_incr_offset = (total_height-40 - margin*num_nodes)/(num_nodes+1) + 40;
                        y_offset = y_incr_offset;
                        console.log(y_incr_offset);
                        for (var i = 0; i < num_nodes; i++){
                                node_alias = val["node"][i]["alias"];
                                var node = $("<div></div>").attr('id', node_alias);
                                node.addClass("circle").css({"top":y_offset-height/2 + "px", "height": height + "px", "width": height + "px"});
                                $("#" + key).append(node);
                                y_offset += y_incr_offset - 40 + margin;
                                window.node_info_map[node_alias] = val["node"][i];
                        }
                        y_offset = 0;
                        x_offset += 200;
                        level_count ++;
                });

                fetch_connections(connector_map)
        });


});



function fetch_connections(connector_map){
        $.getJSON("/connections.json", function( data ) {
                $.each( data, function ( key, val ) {
                        for (var i = 0; i < val.length; i ++){
                                dest = val[i];
                                var e0 =  jsPlumb.addEndpoint(key, exampleGreyEndpointOptions);
                                var e1 =  jsPlumb.addEndpoint(dest, exampleGreyEndpointOptions);
                                jsPlumb.connect({source:e0, target:e1, anchors:"Left", connector:["Straight"], overlays: [["Arrow", {location:1, width:25}]]});
                        }
                });
        });
}
