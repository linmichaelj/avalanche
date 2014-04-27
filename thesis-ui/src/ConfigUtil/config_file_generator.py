"""
Class that generates Larry's config files.
See Test/test_config_file_generator.py for example usage.
"""

import sys
sys.path.append('../')

from ConfigUtil.logical_topology_generator import *
import os

_CONFIG_EXTENSION = '.conf'
_CONFIG_MERGER_NAME = 'dec11-merge-ww-{}-vangogh.msdg1'
_CONFIG_BUILDER_NAME = 'dec11-build-ww-vangogh.msdg1'
_CONFIG_INDEX = 'config_index.conf'


class ConfigFileGenerator():
    def __init__(self, global_config, program_config, topology_config, program_assignment, logical_topology_params):
        """
        global_config: a dictionary {param: default_value}
        program_config: a nested dictionary {program_name: {param: value}}
        topology_config: a instance of the TopologyConfig class which represents the logical topology
        program_assignment: a dictionary {level_name: program_name}
        logical_topology_params: e.g{'total_span': 4, 'granularities': [1, 2, 4]}
        """
        self.global_config = global_config
        self.program_config = program_config
        self.topology_config = topology_config
        self.program_assignment = program_assignment
        self.total = logical_topology_params['total_span']
        self.granularities = logical_topology_params['granularities']
        
    def dict_to_file_content(self, config):
        """
        Convert a single dictionary {param: value} to Larry's config file format.
        Return a string which can be written to file.
        """
        file_content = ""

        for key in config:
            value = config[key]
            if not isinstance(value, list):
                file_content += str(key) + ' ' + str(value) + '\n'
            else:
                file_content += str(key)
                for v in value:
                    file_content += ' ' + str(v)
                file_content += '\n'
                    
        return file_content
    
    
    def level_to_file_content(self, lvl):
        """
        Given a particular level's name, return the file content for the global param
        and program param on that level.
        """
        
        return self.dict_to_file_content(self.global_config) + \
               self.dict_to_file_content(self.program_config[self.program_assignment[lvl]])
    

    def node_to_file_content(self, node_dict):
        """
        Given the dict of a particular node,
        return the file content for the global param, program param and node param.
        """
        return self.dict_to_file_content({
            'STARTING_DATE': node_dict['begin_day'],
            'ENDING_DATE': node_dict['end_day']
        })
         
    
    def node_to_file_name(self, node_dict):
        """
        Given the dict of a particular node,
        return the file name used for its config file.
        """
        return node_dict['alias'] + _CONFIG_EXTENSION
         
    
    def level_to_file_name(self, level):
        """
        Given the name of a particular level,
        return the file name used for its config file.
        """
        lvl_idx = LogicalTopologyGenerator.level_alias_to_level(level)
        if (lvl_idx == 0):
            return _CONFIG_BUILDER_NAME + _CONFIG_EXTENSION
        
        gran = self.granularities[lvl_idx - 1]
        return _CONFIG_MERGER_NAME.format(gran) + _CONFIG_EXTENSION
    
    
    def create_config_files_per_node(self, repo): 
        index_file = open(repo + '/' + _CONFIG_INDEX, 'w')
        for lvl in self.topology_config.config_map:
            for node in self.topology_config.config_map[lvl]['node']:
                file_name = self.node_to_file_name(node)
                global_content = self.dict_to_file_content(self.global_config)
                level_content = self.level_to_file_content(lvl)
                node_content = self.node_to_file_content(node)
                
                f = open(repo + '/' + file_name, 'w')
                f.write(global_content + level_content + node_content)
                f.close()
                
                index_file.write(file_name + '\n')
                
        index_file.close()
        
    def create_config_files_per_level(self, repo): 
        index_file = open(repo + '/' + _CONFIG_INDEX, 'w')
        for lvl in self.topology_config.config_map:
            file_name = self.level_to_file_name(lvl)
            global_content = self.dict_to_file_content(self.global_config)
            level_content = self.level_to_file_content(lvl)
            
            f = open(repo + '/' + file_name, 'w')
            f.write(global_content + level_content)
            f.close()
            
            index_file.write(file_name + '\n')
                
        index_file.close()
        
        
    def save_level_to_config_map(self, path): 
        """Save a json dictionary which maps from level name to config names"""
        level_to_config_map = {}
        
        for lvl in self.topology_config.config_map:
            file_name = self.level_to_file_name(lvl)
            level_to_config_map[lvl] = file_name
            
        f = open(path, 'w')
        f.write(json.dumps(level_to_config_map))
        f.close()
        
        
    @staticmethod
    def clear_config_folder(repo):
        try:
            index_file = open(repo + '/' + _CONFIG_INDEX, 'r')
        except:
            # No index file. Don't need to delete
            return
        
        config_filename = index_file.readline().strip()
        while (config_filename):
            os.remove(repo + '/' + config_filename)
            config_filename = index_file.readline().strip()

        index_file.close()
        os.remove(repo + '/' + _CONFIG_INDEX)
        return

                
