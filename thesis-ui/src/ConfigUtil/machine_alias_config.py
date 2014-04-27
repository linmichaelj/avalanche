import sys
sys.path.append('../')

from ConfigUtil import alias_config

class MachineAliasConfig (alias_config.AliasConfig):
    def __init__(self, file_name):
        alias_config.AliasConfig.__init__(self, file_name, 'machine')

    def add_alias(self, alias, ip_address, ram_port, file_port, db_port):
        machine_config_map = {}
        machine_config_map['alias'] = alias
        machine_config_map['ip_address'] = ip_address
        machine_config_map['ram_port'] = ram_port
        machine_config_map['file_port'] = file_port
        machine_config_map['db_port'] = db_port
        self.alias_list.append(machine_config_map)

    def is_valid(self):
        for alias in self.alias_list:
            if len(alias.keys()) != 5:
                return False
            for key in ['alias', 'ip_address', 'ram_port', 'file_port', 'db_port']:
                if key not in alias:
                    return False

        return True
