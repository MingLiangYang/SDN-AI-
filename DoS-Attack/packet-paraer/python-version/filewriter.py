class filewriter:
    def writeToPath(path, content):
        with open(path, 'a+') as f:
            f.write(content + '\n')