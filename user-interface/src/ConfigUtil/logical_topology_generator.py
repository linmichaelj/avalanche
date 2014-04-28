import sys
sys.path.append('../')

from ConfigUtil.topology_config import *
import re
import json

# Name of the form used for submitting request
_GRAN_FORM = 'l'
_LVL_GRAN = 'level_granularity'
_START = 'begin_day'
_END = 'end_day'


"""
Given the time granularities for different levels (like 1 unit, 2 units, 7 units, 30 units),
create a logical topology XML for the user to plot
"""
class LogicalTopologyGenerator:
    total = 0
    granularities = []
    level_map = {} # map from granularity to level
    levels = []
    edges = {}
    input_edges = {}
    config = None
    """
    each_level = [
	{'input': [(1,1), (1,2)],
	 'output': [(3,1)],
	 'level_granularity': 2,
	 'begin_day': 1,	
	 'end_day': 2}
    ]
    """

    def clear_cache(self):
        self.levels = []
        self.edges = {}
        self.config = None


    def set_time_span(self, total_span):
        """total_span is a positive integer"""
        self.total = total_span
        self.clear_cache()
        return


    def set_granularities(self, granularities):
        """
        granularities is a list of positive integers in increasing order.
        The first element has to 1. We call the first element level 0.
        """
        self.level_map = {}
        self.granularities = granularities
        for i in xrange(len(granularities)):
            self.level_map[granularities[i]] = i
        self.clear_cache()
        return


    def node_time(self, granularity, index):
        """
        Given a granularity (e.g. 3 days), and an index (e.g. 2) within that level,
        return a tuple for the node time (e.g. (4, 6) for 3 days and index = 2).
        """
        start = granularity * (index - 1) + 1
        end = min(self.total, granularity * index)
        return (start, end)


    def last_index(self, granularity):
        """
        Return the index of the last node within the level.
        This node's time range may be shorter than granularity.
        """
        return (self.total - 1) / granularity + 1


    def index(self, granularity, node_time):
        """
        Given a granularity (e.g. 3 days), and a proper node time(e.g. (4, 6)),
        return a index (e.g. 2) for that time range within that level.
        """
        return (node_time[0] - 2) / granularity + 2

    
    def find_nodes(self, granularity, time_range):
        """
        Given a granularity (e.g 2 days) and a time range (e.g. (2, 7)),
        return the smallest and largest indices for nodes whose node time falls within
        the time range (e.g. (2, 3) )
        """
        min_index = (time_range[0] - 2) / granularity + 2
        max_index = time_range[1] / granularity

            # The case where time range is shorter than granularity
        if (time_range[1] == self.total):
            max_index = self.last_index(granularity)

        if min_index > max_index:
            return False

        return (min_index, max_index)


    def list_nodes(self, granularity, min_index, max_index):
        """
        Given the minimum index and maximum index, return a list of tuples representing all nodes within that indices range.
        """
        nodes = []
        level = self.level_map[granularity]
        for i in xrange(min_index, max_index + 1):
            nodes.append((level, i))
        return nodes

    @staticmethod
    def level_alias(lvl):
        return "Level" + str(lvl)

    @staticmethod
    def level_alias_to_level(alias):
        return int(re.search('Level(.*)', alias).group(1))

    def node_alias(self, lvl, index):
        gran = self.granularities[lvl]
        [start, end] = self.node_time(gran, index)
        return self.node_alias_by_level(lvl, start, end)

    @staticmethod
    def node_alias_by_level(lvl, start, end):
        return "Day_" + str(start) + "_to_Day_" + str(end) + "_of_Level_" + str(lvl)

    def node_tuple(self, lvl, index):
        return (lvl, index)

    def node_alias_to_node_tuple(self, node_alias):
        """
        Helper function: given the node alias, return a tuple
        in the format of (lvl, idx) for that node.
        """
        print "node alias" + node_alias
        mo = re.search('Day_(\d+)_to_Day_(\d+)_of_Level_(\d+)', node_alias)
        lvl = int(mo.group(3))
        start = int(mo.group(1))
        end = int(mo.group(2))
        idx = self.index(self.granularities[lvl], (start, end))
        return self.node_tuple(lvl, idx)

    
    @staticmethod
    def source_nodes_aliases(node_alias):
        """
        Given a node alias, return a list of all the aliases for its sources on Level 0.
        """
        print "node alias" + node_alias
        mo = re.search('Day_(\d+)_to_Day_(\d+)_of_Level_(\d+)', node_alias)
        start = int(mo.group(1))
        end = int(mo.group(2))

        node_aliases = []
        for idx in range(start, end + 1):
            node_aliases.append(LogicalTopologyGenerator.node_alias_by_level(0, idx, idx))

        return node_aliases


    @staticmethod
    def downstream_nodes_aliases(topology_edges, node_alias):
        """
        Given a node alias, return a list of all alises for its downstream nodes
        """
        open_list = topology_edges[node_alias]
        downstream_list = []
        downstream_set = set()

        # Breadth first search
        while (len(open_list)):
            node = open_list.pop(0)
            downstream_list.append(node)
            downstream_set.add(node)

            for dest in topology_edges[node]:
                if dest not in downstream_set:
                    open_list.append(dest)

        return downstream_list


    def list_all_input_nodes_rec(self, lvl, time_range):
        """
        List all input nodes of a particular node (specified by its lvl and index)
        """
        if lvl == 0:
            [min_index, max_index] = self.find_nodes(self.granularities[lvl], time_range)
            return self.list_nodes(self.granularities[lvl], min_index, max_index)

        return_list = []
        indices_range = self.find_nodes(self.granularities[lvl], time_range)

        if not indices_range:
            return self.list_all_input_nodes_rec(lvl - 1, time_range)

        nodes_found = self.list_nodes(self.granularities[lvl], indices_range[0], indices_range[1])
        return_list += nodes_found

        # More time on the left
        leftmost_time = self.node_time(self.granularities[lvl], indices_range[0])[0]
        if (leftmost_time != time_range[0]):
            nodes_on_the_left = self.list_all_input_nodes_rec(lvl - 1, (time_range[0], leftmost_time - 1))
            return_list += nodes_on_the_left

        # More time on the right
        rightmost_time = self.node_time(self.granularities[lvl], indices_range[1])[1]
        if (rightmost_time != time_range[1]):
            nodes_on_the_right = self.list_all_input_nodes_rec(lvl - 1, (rightmost_time + 1, time_range[1]))
            return_list += nodes_on_the_right

        return return_list


    def list_all_input_nodes(self, granularity, index):
        """
        List all input nodes of a particular node (specified by its granularity and index)
        """
        lvl = self.level_map[granularity]
        time_range = self.node_time(granularity, index)
        return self.list_all_input_nodes_rec(lvl - 1, time_range)


    def create_level_configuration(self):
        # Level 0
        self.levels.append([])
        gran = self.granularities[0]
        for index in range(1, self.last_index(self.granularities[0]) + 1):
            (start, end) = self.node_time(gran, index)
            node = {'input': [], 'output': [], _LVL_GRAN: gran, _START: start, _END: end}
            self.levels[0].append(node)

        # Other levels
        for lvl in range(1,len(self.granularities)):
            self.levels.append([])
            gran = self.granularities[lvl]
            for index in range(1, self.last_index(self.granularities[lvl]) + 1):
                (start, end) = self.node_time(gran, index)
                node = {'input': [], 'output': [], _LVL_GRAN: gran, _START: start, _END: end}
                self.levels[lvl].append(node)
                input_nodes = self.list_all_input_nodes(self.granularities[lvl], index)

                for (inp_lvl, inp_idx) in input_nodes:
                    self.levels[inp_lvl][inp_idx - 1]['output'].append( (lvl, index) )
                    self.levels[lvl][index - 1]['input'].append( (inp_lvl, inp_idx) )
        return

	
    def get_all_levels(self):
        if not self.levels:
            self.create_level_configuration()
        return self.levels


    def get_input_list(self, lvl, idx):
        if not self.levels:
            self.create_level_configuration()
        return self.levels[lvl][idx - 1]['input']


    def get_output_list(self, lvl, idx):
        if not self.levels:
            self.create_level_configuration()
        return self.levels[lvl][idx - 1]['output']


    def create_all_edges(self):
        if not self.levels:
            self.create_level_configuration()
        for lvl in range(len(self.levels)):
            level = self.levels[lvl]
            for idx in range(1, len(level) + 1):
                node = level[idx - 1]
                self.edges[self.node_alias(lvl, idx)] = []
                for target in node['output']:
                    self.edges[self.node_alias(lvl, idx)].append(self.node_alias(target[0], target[1]))
        return


    def get_all_edges(self):
        if not self.edges:
            self.create_all_edges()
        return self.edges


    def create_config(self, file_name):
        if not self.levels:
            self.create_level_configuration()
        self.config = TopologyConfig(file_name)
        for lvl in range(len(self.levels)):
            level = self.levels[lvl]
            self.config.add_new_level(LogicalTopologyGenerator.level_alias(lvl))
            for idx in range(1, len(level) + 1):
                self.config.add_node_to_level(LogicalTopologyGenerator.level_alias(lvl), self.node_alias(lvl, idx))
                self.config.add_attribute_to_node(self.node_alias(lvl, idx), _LVL_GRAN, self.levels[lvl][idx-1][_LVL_GRAN])
                self.config.add_attribute_to_node(self.node_alias(lvl, idx), _START, self.levels[lvl][idx-1][_START])
                self.config.add_attribute_to_node(self.node_alias(lvl, idx), _END, self.levels[lvl][idx-1][_END])


    def get_config(self):
        return self.config


    def parse_form_input(self, request_args):
        """Given request.args object, parse the inputs return a LogicalTopologyGenerator object.
        The time span and granularities should be set.
        """
        self.set_time_span(int(request_args.get('total')))
        gran = []

        i = 0
        arg = request_args.get(_GRAN_FORM + str(i))

        while arg:
            gran.append(int(arg))
            i += 1
            arg = request_args.get(_GRAN_FORM + str(i))

        self.set_granularities(gran)
        self.get_all_levels()


    def save_params_to_file(self, filename):
        f = open(filename, 'w')
        params = {"total_span": self.total, \
                  "granularities": self.granularities}
        f.write(json.dumps(params))
        f.close()
    

    def get_input_edges(self):
        if self.input_edges:
            return self.input_edges

        for src in self.get_all_edges():
            if src not in self.input_edges:
                self.input_edges[src] = []

            for d in self.get_all_edges()[src]:
                print d, src
                if d in self.input_edges:
                    self.input_edges[d].append(src)
                else:
                    self.input_edges[d] = [src]

        return self.input_edges


    def save_params_for_avalanche_core(self, filename):
        f = open(filename, 'w')
        for dest in self.get_input_edges():
            f.write(dest)
            for src in self.get_input_edges()[dest]:
                f.write(" " + src)
            f.write("\n")

        f.close()



