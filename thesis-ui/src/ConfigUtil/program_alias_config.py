from ConfigUtil import alias_config

__author__ = 'michaellin'


class ProgramAliasConfig (alias_config.AliasConfig):
    def __init__(self, file_name):
        alias_config.AliasConfig.__init__(self, file_name, 'program')

    def add_alias(self, alias, path, args):
        program_config_map = {}
        program_config_map['alias'] = alias
        program_config_map['path'] = path
        if args is not None:
            for i in range(0, len(args)):
                program_config_map["arg" + str(i)] = args[i]
        self.alias_list.append(program_config_map)

    def is_valid(self):
        for alias in self.alias_list:
            if ('alias' not in alias) or ('path' not in alias):
                print 'here'
                return False

        #TODO Validate existance of program
        return True

