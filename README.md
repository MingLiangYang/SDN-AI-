# SDN-AI-
ODL+OVS+Tensorflow AI
## Tensorflow AI
Lstm-Tensorflow Classification model can classify samples with time series features.
### Data characteristics
* ‘Index’ timing characteristics
* ‘PktNum, PktNumRate, AveLength, IpEntropy, PortEntropy’ other features
* ‘Tag’ 0 is the normal type and 1 is the attack type
### Import data
Firstly, pre-set time step, layer units, input_size, output_size, weights, biases and so on, and divide the training set and test set after data normalization.
### Construct model
Pay attention to the transformation of the variable dimension. The input of the hidden layer is 2D, the input of the lstm cell is 3D, and the input of the output layer must be converted accordingly.
### Train model
Define batch_size to be 10 and perform two hundred times to get better results.
