from ConfigUtil import config_util

__author__ = 'michaellin'

import xml.etree.ElementTree as ET


class AliasConfig:
    def __init__(self, file_name, type):
        self.alias_list = []
        self.file_name = file_name
        self.type = type

    def delete_alias(self, del_alias):
        """
        Completely remove a machine alias along with its properties.
        Return 0 if successful, return -1 otherwise.
        """
        for alias in self.alias_list:
            if 'alias' in alias and alias['alias'] == del_alias:
                self.alias_list.remove(alias)
                return 0
        return -1

    def change_alias(self, old_alias, new_alias):
        """
        Modify an existing alias without changing its properties.
        Return 0 if successful, return -1 otherwise.
        """
        for alias in self.alias_list:
            if 'alias' in alias and alias['alias'] == old_alias:
                alias['alias'] = new_alias
            return 0
        return -1

    def write_to_file(self):
        if not self.is_valid():
            print 'Validation Exception Occurred'
            return

        f = open(self.file_name, 'w')
        f.write(self.get_xml())
        f.close()

    def load_from_file(self):
        f = open(self.file_name, 'r')
        root = ET.fromstring(f.read())
        for alias in root:
            alias_config_map = config_util.prop_to_map(alias)
            self.alias_list.append(alias_config_map)

        if not self.is_valid():
            print 'Validation Exception Ocurred'
            self.alias_list = []
            return
        f.close()

    def get_xml(self):
        config_elem = ET.Element('config')
        for alias in self.alias_list:
            machine_elem = ET.SubElement(config_elem, self.type)
            config_util.prop_to_xml(machine_elem, alias)
        return config_util.format_url(config_elem)


    #Abstract Methods is_valid, add_config
