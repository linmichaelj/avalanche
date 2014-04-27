from ConfigUtil import config_util

__author__ = 'michaellin'

import xml.etree.ElementTree as ET


class HierarchyConfig:
    def __init__(self, file_name, repeated_tag):
        self.config_map = {}
        self.file_name = file_name
        self.repeated_tag = repeated_tag

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
        self.config_map = config_util.xml_to_hierarchy(root, self.repeated_tag)

        if not self.is_valid():
            print 'Validation Exception Ocurred'
            self.alias_list = {}
            return
        f.close()

    def get_xml(self):
        config_elem = ET.Element('config')
        config_util.hierarchy_to_xml(config_elem, self.config_map)
        return config_util.format_url(config_elem)


    #Abstract Methods is_valid, add_config
