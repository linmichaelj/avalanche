__author__ = 'michaellin'

import xml.etree.ElementTree as ET
from xml.dom import minidom


def hierarchy_to_xml(parent, hierarchy_map):

    for node in hierarchy_map.keys():
        if type(hierarchy_map[node]) is dict:
            hierarchy_to_xml(ET.SubElement(parent, node), hierarchy_map[node])
        elif type(hierarchy_map[node]) is list:
            for list_elem in hierarchy_map[node]:
                hierarchy_to_xml(ET.SubElement(parent, node), list_elem)
        else:
            ET.SubElement(parent, node).text = hierarchy_map[node]
    return parent


def xml_to_hierarchy(parent, repeated_tag):
    level_dict = {}
    for element in parent:
        if len(element) == 0:
            value = element.text.strip()
        else:
            value = xml_to_hierarchy(element, repeated_tag)

        # Deal with repeated keys
        if repeated_tag == element.tag:
            if element.tag in level_dict:
                level_dict[element.tag].append(value)
            else:
                level_dict[element.tag] = [value]
        else:
            level_dict[element.tag] = value

    return level_dict


def prop_to_xml(element, prop_map):
    for key in prop_map.keys():
        ET.SubElement(element, key).text = prop_map[key]
    return element


def prop_to_map(element):
    prop_map = {}
    for prop in element:
        prop_map[prop.tag] = prop.text.strip()
    return prop_map


def format_url(root):
    rough_string = ET.tostring(root)
    print rough_string
    return minidom.parseString(rough_string).toprettyxml(indent="  ")
