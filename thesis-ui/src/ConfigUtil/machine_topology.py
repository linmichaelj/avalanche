__author__ = 'michaellin'

from ConfigUtil import topology_config
import random

class MachineTopology:
    def __init__(self, topology_config_path, machine_config_path, edges, machine_alias):
        '''
        Input: path to topology_config xml object, edge map, list of machine alias
        '''

        self.topology = topology_config.TopologyConfig(topology_config_path)
        self.topology.load_from_file()

        self.machine_assignments = self.generate_machine_assignments(self.topology, machine_alias)
        self.machine_alias = machine_alias

        # update xml topology
        self.add_topology_machines(self.topology, self.machine_assignments)
        self.topology.file_name = machine_config_path
        self.topology.write_to_file()

        # generate the machine description {machine: {level: [{node}]}}
        self.machine_description = self.generate_machine_descriptions(self.topology, machine_alias)

        # generate links {machine source: {destination machine: {Source Node (on source machine): [destination Source (on destination machine)]}}
        self.link_description = self.generate_link_descriptions(self.machine_assignments, edges, machine_alias)

    def generate_machine_assignments(self, topology, machine_alias):
        machine_assignments = {}
        random.shuffle(machine_alias)
        i = 0

        for level_alias in topology.config_map:
            for node in topology.config_map[level_alias]['node']:
                machine_assignments[node['alias']] = machine_alias[i%len(machine_alias)]["alias"]
                i += 1

        return machine_assignments

    def add_topology_machines(self, topology, machine_assignments):
        for node in machine_assignments.keys():
            topology.add_machine_to_node(node, machine_assignments[node])

    def generate_machine_descriptions(self, topology, machine_alias):
        machine_description = {}

        for machine in machine_alias:
            machine_description[machine["alias"]] = {}

        for level_alias in topology.config_map:
            for node in topology.config_map[level_alias]['node']:
                if level_alias in machine_description[node["machine"]]:
                    machine_description[node["machine"]][level_alias].append(node)
                else:
                    machine_description[node["machine"]][level_alias] = [node]

        return machine_description

    def generate_link_descriptions(self, machine_assignments, edges, machine_alias):
        link_description = {}

        for machine in machine_alias:
            link_description[machine["alias"]] = {}

        for source_node in edges.keys():
            source_machine = machine_assignments[source_node]
            for dest_node in edges[source_node]:
                dest_machine = machine_assignments[dest_node]

                if dest_machine in link_description[source_machine]:
                    if source_node in link_description[source_machine][dest_machine]:
                        link_description[source_machine][dest_machine][source_node].append(dest_node)
                    else:
                        link_description[source_machine][dest_machine][source_node] = [dest_node]
                else:
                    link_description[source_machine][dest_machine] = {}
                    link_description[source_machine][dest_machine][source_node] = [dest_node]
        return link_description


    def save_params_for_avalanche_core(self, filename):
        f = open(filename, 'w')
        for machine in self.machine_alias:
            f.write(machine['ip_address'] + '\n');
        f.close()

