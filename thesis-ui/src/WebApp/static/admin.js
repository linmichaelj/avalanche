$(document).ready( function () {
        $("a[href='/admin'").parent().addClass("active");

    var url = document.location.toString();
    if (url.match('#')) {
        $('.nav-tabs a[href=#'+url.split('#')[1]+']').tab('show') ;
    }


});





$(document).ready( function () {
        $('#mytab a').click(function (e) {
          e.preventDefault();
          $(this).tab('show');
        });
});

var load_add_machine = function(){
        row_number = $('.input-row').length
        var row = $('<div class="input-row"></div>');
        row.append($('<div class="form-group alias-input"><input type="text" name="alias' + row_number + '" class="form-control" placeholder="Machine Alias"></div>'));
        row.append($('<div class="form-group alias-input"><input type="text" name="ip_address' + row_number + '" class="form-control" placeholder="IP Address"></div>'));
        row.append($('<div class="form-group alias-input"><input type="text" name="db_port' + row_number + '" class="form-control" placeholder="DB Port"></div>'));
        row.append($('<div class="form-group alias-input"><input type="text" name="ram_port' + row_number + '" class="form-control" placeholder="Ram Port"></div>'));
        row.append($('<div class="form-group alias-input"><input type="text" name="file_port'+ row_number + '" class="form-control" placeholder="File Port"></div>'));
        (row.fadeIn(400)).insertBefore('.machine-save-button');
}

$(document).ready( function () {
    $('.program').hide()
});

var load_add_program = function(){
    $('.program').hide()
    $('#add_program').show()
}

var load_add_program_pair = function(){
    console.log('hello')
    row_number = $('.prog-row').length
    var row = $('<div class="prog-row"></div>');
    row.append($('<div class="form-group pair-input"><input type="text" name="key' + row_number + '" class="form-control" placeholder="Config Key"></div>'));
    row.append($('<div class="form-group pair-input"><input type="text" name="value' + row_number + '" class="form-control" placeholder="Config Value"></div>'));
    (row.fadeIn(400)).insertBefore('#prog-buttons');
}

var load_add_config_pair = function(){
    console.log('hello2')
    row_number = $('.prog-row').length
    var row = $('<div class="prog-row"></div>');
    row.append($('<div class="form-group pair-input"><input type="text" name="key' + row_number + '" class="form-control" placeholder="Config Key"></div>'));
    row.append($('<div class="form-group pair-input"><input type="text" name="value' + row_number + '" class="form-control" placeholder="Config Value"></div>'));
    (row.fadeIn(400)).insertBefore('#config-buttons');
}

var delete_alias = function(alias){
    window.location.replace("/machine_alias_delete?alias=" + alias);
}

var delete_global = function(key){
    window.location.replace("/global_pair_delete?key=" + key);
}

var delete_program = function(key){
    console.log('here')
    window.location.replace("/program_delete?key=" + key);
}

var machine_submit = function(){
        $('.machine-form').submit();
}

var program_submit = function(){
    $('.prog-form').submit();
}

var config_submit = function(){
    $('.config-form').submit();
}

var display_program = function(key){
    console.log(key)
    $('#' + key).show()
}

function get(name){
    if(name=(new RegExp('[?&]'+encodeURIComponent(name)+'=([^&]*)')).exec(location.search))
        return decodeURIComponent(name[1]);
}
