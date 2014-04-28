import sys
sys.path.append('../../')
    
from ConfigUtil.logical_topology_generator import *
from ConfigUtil.config_file_generator import *
from ConfigUtil import topology_config

import json
import pprint

if __name__ == "__main__":
    # Get logical topology
    topology_config = topology_config.TopologyConfig('logical_topology_generator.xml')
    topology_config.load_from_file()
    pprint.pprint(topology_config.config_map)
    
    # Hard code global params and program specific prams
    global_config = {'CHECK_INPUT_TIMEOUT': 5, 'CHECK_SCHEDULER_TIMEOUT': 1200}
    program_config = {
        'wc-1d': {'FILE_TYPES': 'msg-1d'},
        'wc-2d': {'FILE_TYPES': 'msg-2d'}
    }
    program_assignment = {'Level0': 'wc-1d', 'Level1': 'wc-2d'}
    
    f = open('logical_topology_params.json', 'r')
    logical_topology_params = json.loads(f.read())
    f.close()

    gen = ConfigFileGenerator(global_config, program_config, topology_config, program_assignment, logical_topology_params)
    print gen.level_to_file_content('Level1')
    gen.create_config_files_per_level('auto_generated_config_files')
    
    