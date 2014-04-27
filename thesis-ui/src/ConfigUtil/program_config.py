from ConfigUtil import hierarchy_config

__author__ = 'michaellin'


class ProgramConfig (hierarchy_config.HierarchyConfig):

    def add_programs(self, db_program, copy_program, computation_programs):
        self.config_map["DB_Program"] = db_program
        self.config_map["Copy_Program"] = copy_program
        self.config_map["Computation_Program"] = computation_programs
        if computation_programs is not None:
            for level_alias in computation_programs.keys():
                self.config_map[level_alias] = computation_programs[level_alias]

    def is_valid(self):
        for key in ["DB_Program", "Copy_Program", "Computation_Program"]:
            if key not in self.config_map:
                return False
        return True





