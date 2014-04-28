from ConfigUtil.machine_alias_config import *
from ConfigUtil.program_alias_config import *

if __name__ == '__main__':
    print '___________Test machine_config.py_____________'

    print '___________Test add_alias_____________________'
    test_class = MachineAliasConfig('machine_alias.xml')
    test_class.add_alias('fake name 1', '1.1.1.1', '100', '200', '300')
    test_class.add_alias('fake name 2', '2.2.2.2', '101', '201', '301')
    print test_class.get_xml()

    print '___________Test change_alias_____________________'
    test_class.change_alias('fake name 1','random name 1')
    print test_class.get_xml()

    print '___________Test delete_alias_____________________'
    test_class.delete_alias('fake name 2')
    print test_class.get_xml()

    print '___________Test write_to_file and load_from_file_______________'
    test_class.write_to_file()
    new_class = MachineAliasConfig('machine_alias.xml')
    new_class.load_from_file()
    print new_class.get_xml()

    print '___________Test completed!____________________'

    print '___________Test program_config.py_____________'

    print '___________Test add_alias_____________________'
    test_class = ProgramAliasConfig('test_program_config_output.xml')
    test_class.add_alias('Program 1', 'path1.txt', [])
    test_class.add_alias('Program 2', 'path2.txt', ['-v', 'path.txt'])
    print test_class.get_xml()

    print '___________Test write_to_file and load_from_file_______________'
    test_class.write_to_file()
    new_class = ProgramAliasConfig('test_program_config_output.xml')
    new_class.load_from_file()
    print new_class.get_xml()

    print '___________Test completed!____________________'

