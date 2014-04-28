import sys
sys.path.append('../')

from flask import Flask, request, render_template, jsonify, redirect
from ConfigUtil.topology_config import *
from ConfigUtil.machine_topology import *
from ConfigUtil.config_file_generator import *
from ConfigUtil.machine_alias_config import *
from ConfigUtil.logical_topology_generator import *
from ConfigUtil.task_status_reader import *
import json
import os
from random import randrange

app = Flask(__name__)
_TOPOLOGY_XML = 'configs/test_logical_topology_output.xml'
_MACHINE_TOPOLOGY_XML = 'configs/machine_topology.xml'
_MACHINE_ALIAS_XML = 'configs/machine_alias.xml'
_LOGICAL_TOPOLOGY_PARAMS = 'configs/logical_topology_params.json'
_CONNECTION_JSON = 'configs/connections.json'
_MACHINE_CONNECTIONS_JSON = 'configs/machine_connections.json'
_MACHINE_DESCRIPTION_JSON = 'configs/machine_descriptions.json'
_CONFIG_GLOBALS_JSON = 'configs/config_globals.json'
_PROGRAM_JSON = 'configs/programs.json'
_LEVEL_JSON = 'configs/level.json'
_LEVEL_TO_CONFIG_MAP_JSON = 'configs/level_to_config_map.json'
_LARRY_CONFIG_REPO = '../../ava01'
_AVALANCHE_CORE_ROOT = '../../../'
_AVALANCHE_CORE_TOPOLOGY = _AVALANCHE_CORE_ROOT + 'UI-config/topology.conf'
_AVALANCHE_CORE_MACHINE = _AVALANCHE_CORE_ROOT + 'UI-config/machine.conf'
_TASK_STATUS_FILE = _AVALANCHE_CORE_ROOT + "UI-config/task_status_file"
_SEARCH_JAR_CMD = "java -jar avalanche-core2.jar "
_LATEST_QUERY =  _AVALANCHE_CORE_ROOT + "UI-config/LatestQuery.txt"

# Global variables
_RUNNING = False

@app.route('/')
def root():
  return render_template('time_granularity.html')

@app.route('/levelassignment')
def level_assignment():
    f = open(_PROGRAM_JSON, 'r')
    programs = json.loads(f.read())
    f.close()
    topology = TopologyConfig(_TOPOLOGY_XML)
    topology.load_from_file()
    levels = []
    for level in topology.config_map.keys():
        levels.append(level)

    return render_template('level_assignment.html', levels=levels, programs=programs)

@app.route('/submitlevelassignment')
def submit_level_assignment():
    topology = TopologyConfig(_TOPOLOGY_XML)
    topology.load_from_file()
    levels = []
    for level in topology.config_map.keys():
        levels.append(level)

    level_assignment = {}
    for level in topology.config_map.keys():
        level_assignment[level] = request.args[level]

    f = open(_LEVEL_JSON, 'w')
    f.write(json.dumps(level_assignment))

    return redirect('/calculate_machine')

@app.route('/connections.json')
def get_connections():
    f = open(_CONNECTION_JSON, 'r')
    connections = json.loads(f.read())
    f.close()
    return jsonify(connections)


@app.route('/machine_descriptions.json')
def get_machine_descriptions():
    f = open(_MACHINE_DESCRIPTION_JSON, 'r')
    machine_descriptions = json.loads(f.read())
    f.close()
    return jsonify(machine_descriptions)

@app.route('/machine_connections.json')
def get_machine_connections():
    f = open(_MACHINE_CONNECTIONS_JSON, 'r')
    machine_connections = json.loads(f.read())
    f.close()
    return jsonify(machine_connections)


@app.route('/topology.json')
def get_topology():
    topology = TopologyConfig(_TOPOLOGY_XML)
    topology.load_from_file()
    map = topology.config_map
    return jsonify(map)

@app.route('/time_granularity_submit')
def get_time_granularity():
    topo = LogicalTopologyGenerator()
    topo.parse_form_input(request.args)
    topo.create_config(_TOPOLOGY_XML)
    topo.get_config().write_to_file()

    f = open(_CONNECTION_JSON, 'w')
    f.write(json.dumps(topo.get_all_edges()))
    f.close()

    topo.save_params_to_file(_LOGICAL_TOPOLOGY_PARAMS)
    ConfigFileGenerator.clear_config_folder(_LARRY_CONFIG_REPO)

    #TODO: save topology for avalanche core
    topo.save_params_for_avalanche_core(_AVALANCHE_CORE_TOPOLOGY)

    return redirect('/graph')


@app.route('/forward_backward_path')
def get_downstream_node_aliases():
    f = open(_CONNECTION_JSON, 'r')
    edges = json.loads(f.read())
    f.close()
    node_alias = request.args.get('alias')
    downstream_nodes = LogicalTopologyGenerator.downstream_nodes_aliases(edges, node_alias)
    source_nodes = LogicalTopologyGenerator.source_nodes_aliases(node_alias)
    return jsonify(backward = source_nodes, forward = downstream_nodes)


@app.route('/admin')
def admin_root():
    machine_alias = MachineAliasConfig(_MACHINE_ALIAS_XML)
    machine_alias.load_from_file()
    f = open(_CONFIG_GLOBALS_JSON, 'r')
    global_pairs = json.loads(f.read())
    f.close()
    f = open(_PROGRAM_JSON, 'r')
    programs = json.loads(f.read())
    f.close()
    return render_template('admin.html', machines=machine_alias.alias_list, global_pairs = global_pairs, programs=programs, program_count = len(programs))

@app.route('/config_alias_submit')
def config_global_submit():
    f = open(_CONFIG_GLOBALS_JSON, 'r+')
    globals = json.loads(f.read())
    i = 0
    while 'key' + str(i) in request.args.keys():
        globals[request.args['key' + str(i)]] = request.args['value' + str(i)]
        i+=1
    f.seek(0)
    f.write(json.dumps(globals))
    f.truncate()
    f.close()
    return redirect('/admin#Configs')

@app.route('/machine_alias_submit')
def machine_alias_submit():
    machine_alias = MachineAliasConfig(_MACHINE_ALIAS_XML)
    machine_alias.load_from_file()
    i = 0
    while 'alias' + str(i) in request.args.keys():
        machine = [request.args['alias' + str(i)],
                   request.args['ip_address' + str(i)],
                   request.args['ram_port' + str(i)],
                   request.args['file_port' + str(i)],
                   request.args['db_port' + str(i)]]
        attr_test = True
        for attr in machine:
            if len(attr) == 0:
                attr_test = False
        if attr_test == False:
            i += 1
            continue

        machine_alias.add_alias(machine[0], machine[1], machine[2], machine[3], machine[4])
        i += 1
    machine_alias.write_to_file()
    return redirect('/admin')

@app.route('/program_alias_submit')
def program_alias_submit():
    f = open(_PROGRAM_JSON, 'r+')
    programs = json.loads(f.read())
    program = {}
    alias = request.args['Program Alias']
    program['path'] = request.args['Program Path']
    i = 0
    while 'key' + str(i) in request.args.keys():
        program[request.args['key' + str(i)]] = request.args['value' + str(i)]
        i+=1
    programs[alias] = program
    f.seek(0)
    f.write(json.dumps(programs))
    f.close()
    return redirect('/admin#program')


@app.route('/program_delete')
def program_delete():
    f = open(_PROGRAM_JSON, 'r+')
    globals = json.loads(f.read())
    del globals[request.args['key']]
    f.seek(0)
    f.write(json.dumps(globals))
    f.truncate()
    f.close()
    return redirect('/admin#program')



@app.route('/global_pair_delete')
def global_pair_delete():
    f = open(_CONFIG_GLOBALS_JSON, 'r+')
    globals = json.loads(f.read())
    del globals[request.args['key']]
    f.seek(0)
    f.write(json.dumps(globals))
    f.truncate()
    f.close()
    return redirect('/admin#Configs')

@app.route('/machine_alias_delete')
def machine_alias_delete():
    machine_alias = MachineAliasConfig(_MACHINE_ALIAS_XML)
    machine_alias.load_from_file()
    alias = request.args['alias']
    mark_for_delete = None
    for machine in machine_alias.alias_list:
        if machine['alias'] == alias:
            mark_for_delete = machine
    if mark_for_delete is not None:
        machine_alias.alias_list.remove(mark_for_delete)
    machine_alias.write_to_file()
    return redirect('admin')

@app.route('/calculate_machine')
def calculate_machine():
    f = open(_CONNECTION_JSON, 'r')
    connections = json.loads(f.read())
    f.close()

    machine = MachineAliasConfig(_MACHINE_ALIAS_XML)
    machine.load_from_file()

    d = MachineTopology(_TOPOLOGY_XML, _MACHINE_TOPOLOGY_XML, connections, machine.alias_list)
    f = open(_MACHINE_CONNECTIONS_JSON, 'w')
    f.write(json.dumps(d.link_description))
    f.close()

    f = open(_MACHINE_DESCRIPTION_JSON, 'w')
    f.write(json.dumps(d.machine_description))
    f.close()

    d.save_params_for_avalanche_core(_AVALANCHE_CORE_MACHINE)

    return redirect('/machine_graph')


@app.route('/graph')
def graph():
    return render_template('graph.html')


@app.route('/machine_graph')
def machine_graph():
    return render_template('machine_graph.html')
  
@app.route('/generate_config_files')
def generate_config_files():
    # Get logical topology
    topology_config = TopologyConfig(_TOPOLOGY_XML)
    topology_config.load_from_file()
    
    # Get the global params and program specific prams
    f = open(_CONFIG_GLOBALS_JSON, 'r')
    global_config = json.loads(f.read())
    f.close()
    
    f = open(_PROGRAM_JSON, 'r')
    program_config = json.loads(f.read())
    f.close()
    
    f = open(_LEVEL_JSON, 'r')
    program_assignment = json.loads(f.read())
    f.close()

    f = open(_LOGICAL_TOPOLOGY_PARAMS, 'r')
    logical_topology_params = json.loads(f.read())
    f.close()
    
    gen = ConfigFileGenerator(global_config, program_config, topology_config, \
                              program_assignment, logical_topology_params)
    gen.create_config_files_per_level(_LARRY_CONFIG_REPO)
    gen.save_level_to_config_map(_LEVEL_TO_CONFIG_MAP_JSON)

    return redirect('/progress')

@app.route('/progress')
def progress():
    TaskStatusReader(_TASK_STATUS_FILE).clear_status()
    return render_template('progress.html')

@app.route('/chart')
def get_chart_data():
    x = TaskStatusReader(_TASK_STATUS_FILE).read_status(request.args['alias'])
    return jsonify({'x1':x, 'x2':(100-x)})

@app.route('/start')
def start():
    print 'running'
    return jsonify({'status': 'success'})

@app.route('/getstatus')
def status():
    return jsonify({'running': _RUNNING})


@app.route('/search')
def search():
    saved_path = os.getcwd()
    os.chdir(_AVALANCHE_CORE_ROOT)
    print 'IN /SEARCH!!!'
    print "SAVED PATH: " + saved_path
    #os.system(_SEARCH_JAR_CMD + "L0s1e1")
    os.chdir(saved_path)

    f = open(_LATEST_QUERY, 'r')
    content = f.read()
    print content
    f.close()

    return content

if __name__ == '__main__':
  app.run(debug=True)


