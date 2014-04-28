import sys
sys.path.append('../../')

from ConfigUtil.logical_topology_generator import *
from ConfigUtil.machine_topology import *
from ConfigUtil.machine_alias_config import *

import pprint

if __name__ == '__main__':

    print "________________Test logical_topology_generator.py______________"

    c = LogicalTopologyGenerator()
    c.set_time_span(5)
    c.set_granularities([1, 2])

    print '_____________________test create_level_configuration()______________________'
    print 'All levels ' + str(c.get_all_levels())

    print '______________________test create_config()________________________'
    c.create_config('logical_topology_generator.xml')
    c.get_config().write_to_file()

    print '_____________________test create_all_edges()_______________________'
    print 'All edges ' + str(c.get_all_edges())

    print '_____________________test source_nodes_aliases()_______________________'
    print 'Source node aliases for L1s3e4 are' + str(LogicalTopologyGenerator.source_nodes_aliases('L1s3e4'))

    print '_____________________test downstream_nodes_aliases()_______________________'
    print 'Downstream node aliases for L0s1e1 are' + str(LogicalTopologyGenerator.downstream_nodes_aliases(c.get_all_edges(), 'L0s1e1'))

    print '_____________________test save_params_to_file()_______________________'
    c.save_params_to_file('logical_topology_params.json')

    print "________________Test machine_topology.py______________"

    test_class = MachineAliasConfig('machine_alias.xml')
    test_class.add_alias('fake name 1', '1.1.1.1', '100', '200', '300')
    test_class.add_alias('fake name 2', '2.2.2.2', '101', '201', '301')

    test_class.write_to_file()
    new_class = MachineAliasConfig('machine_alias.xml')

    d = MachineTopology('logical_topology_generator.xml', 'machine_topology_generator.xml', c.edges, test_class.alias_list)

    print d.machine_description
    pprint.pprint(d.link_description)



