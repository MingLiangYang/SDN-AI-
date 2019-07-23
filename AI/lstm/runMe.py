import os
from dataprocessing import Datapro
from run_model import RunModel
if __name__ == '__main__':
	print ('Data Processing...')
	datapro=Datapro()
	datapro.dataprocess()
	print ('run model...')
	runModel=RunModel()
	runModel.run()
	# os.system('python run_model.py')
	print ('Finished!')