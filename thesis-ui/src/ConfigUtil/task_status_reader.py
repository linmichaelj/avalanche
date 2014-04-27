
class TaskStatusReader():
    status_dict = {}

    def __init__(self, filename):
        self.filename = filename

    def read_status(self, task):
        if task in self.status_dict and self.status_dict[task] == 100:
            return 100

        f = open(self.filename)
        line = f.readline()
        while (line):
            if task == line.split()[0]:
                f.close()
                self.status_dict[task] = int(line.split()[1])
                return self.status_dict[task]
            line = f.readline()

        f.close()
        return 0

    def clear_status(self):
        self.status_dict = {}
        f = open(self.filename, 'w')
        f.write("")
        f.close()
