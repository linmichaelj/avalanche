from ConfigUtil import hierarchy_config

# TODO Sanitize alias so spaces are removed and starting with numbers error
class TopologyConfig(hierarchy_config.HierarchyConfig):
    def __init__(self, file_name):
        hierarchy_config.HierarchyConfig.__init__(self, file_name, 'node')

    def add_new_level(self, level_alias):
        self.config_map[level_alias] = {"node":[]}

    def add_node_to_level(self, level_alias, node_alias):
        if level_alias not in self.config_map:
            self.add_new_level(level_alias)
        self.config_map[level_alias]["node"].append({"alias": node_alias})


    def add_machine_to_level(self, level_alias, machine_alias):
        if level_alias not in self.config_map:
            return
        nodes = self.config_map[level_alias]["node"]
        for node in nodes:
            node["machine"] = machine_alias

    def add_machine_to_node(self, node_alias, machine_alias):
        for level in self.config_map:
            nodes = self.config_map[level]["node"]

            for node in nodes:
                if node_alias == node["alias"]:
                    node["machine"] = machine_alias


    def add_attribute_to_node(self, node_alias, attr_name, attr_value):
        for level in self.config_map:
            nodes = self.config_map[level]["node"]

            for node in nodes:
                if node_alias == node["alias"]:
                    node[attr_name] = str(attr_value)


    def is_valid(self):
        return True
