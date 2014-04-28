from ConfigUtil.program_config import *

if __name__ == '__main__':
    print '___________Test program_config.py_____________'

    print '___________Test add_alias_____________________'
    test_class = ProgramConfig('test_program_config_output.xml')
    test_class.add_programs('db copy program alias', 'copy program alias', {'peak':'computation program alias 1', 'path':'computation program alias 2'})
    print test_class.get_xml()


    print '___________Test write_to_file and load_from_file_______________'
    test_class.write_to_file()
    new_class = ProgramConfig('test_program_config_output.xml')
    new_class.load_from_file()
    print new_class.get_xml()