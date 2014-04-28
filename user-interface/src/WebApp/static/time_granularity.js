var add_level = function(){
	
	var levels = $('.topology-levels');	
	var num_levels = levels.children('div').length;

	var new_level = $('<div class="form-group">' +
          '<label for="level' + num_levels + '">Level ' + num_levels + ' Granularity</label>' +
          '<input type="text" class="form-control" name="l' + num_levels + '" id="level' + num_levels + '" placeholder="Must be greater than the previous level">' +
	  '</div>');
	
	levels.append(new_level);
}


