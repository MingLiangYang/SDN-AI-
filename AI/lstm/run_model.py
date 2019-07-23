import numpy as np
import tensorflow as tf
from tensorflow.contrib import rnn
import configparser
from minibatcher import MiniBatcher 
import matplotlib.pyplot as plt
from sklearn.metrics import roc_curve, auc  ###计算roc和auc

class RunModel(object):
	def run(self):
		config = configparser.ConfigParser()
		config.read('./config.ini')
		print('loading data...')
		X_tr = np.load(config.get('DataPath', 'X_tr_path'))
		y_tr = np.load(config.get('DataPath', 'y_tr_path'))
		X_val = np.load(config.get('DataPath', 'X_val_path'))
		y_val = np.load(config.get('DataPath', 'y_val_path'))

		# 训练参数
		learning_rate = float(config.get('Parameters', 'learning_rate'))
		batch_size = int(config.get('Parameters', 'batch_size'))
		display_step = int(config.get("Parameters", 'display_step'))
		EPOCH = int(config.get('Parameters', 'EPOCH'))

		minibatcher = MiniBatcher(batch_size, X_tr.shape[0])

		tf.reset_default_graph()
		# 获取神经网络的参数
		num_input = int(config.get('NetworkParameters', 'num_input'))
		timesteps = int(config.get('NetworkParameters', 'timesteps'))
		num_hidden = int(config.get('NetworkParameters', 'num_hidden'))
		num_classes = int(config.get('NetworkParameters', 'num_classes'))
		PEEHOLE = config.getboolean('NetworkParameters', 'peehole')
		bp_hidden = int(config.get('NetworkParameters', 'bp_hidden'))
		bp_hidden1 = int(config.get('NetworkParameters', 'bp_hidden1'))
		# tf 图的输入
		X = tf.placeholder("float", [None, timesteps, num_input])
		Y = tf.placeholder("float", [None, num_classes])

		weights = {
			'out': tf.Variable(tf.random_normal([num_hidden, num_classes]))
		}
		biases = {
			'out': tf.Variable(tf.random_normal([num_classes]))
		}

		def LSTM(x, weights, biases):
			x = tf.unstack(x, timesteps, 1)

			# 使用tensorflow定义LSTM细胞
			def lstm_cel():
				return rnn.LSTMCell(num_hidden, forget_bias=0.3, use_peepholes=PEEHOLE)

			stacked_lstm = rnn.MultiRNNCell([lstm_cel() for _ in range(2)])
			outputs, states = rnn.static_rnn(stacked_lstm, x, dtype=tf.float32)
			return tf.matmul(outputs[-1], weights['out']) + biases['out']

		# 定义RNN网络
		def RNN(x, weights, bias):
			x = tf.reshape(x, shape=[-1, timesteps, 1])
			# 先把输入转换为dynamic_rnn接受的形状：batch_size,sequence_length,frame_size这样子的
			rnn_cell = tf.nn.rnn_cell.BasicRNNCell(num_hidden)
			# 生成hidden_num个隐层的RNN网络,rnn_cell.output_size等于隐层个数，state_size也是等于隐层个数，但是对于LSTM单元来说这两个size又是不一样的。
			# 这是一个深度RNN网络,对于每一个长度为sequence_length的序列[x1,x2,x3,...,]的每一个xi,都会在深度方向跑一遍RNN,每一个都会被这hidden_num个隐层单元处理。
			output, states = tf.nn.dynamic_rnn(rnn_cell, x, dtype=tf.float32)
			# 此时output就是一个[batch_size,sequence_length,rnn_cell.output_size]形状的tensor
			print(output.shape)
			return tf.matmul(output[:, -1, :], weights['out']) + bias['out']

		# 我们取出最后每一个序列的最后一个分量的输出output[:,-1,:],它的形状为[batch_size,rnn_cell.output_size]也就是:[batch_size,hidden_num]所以它可以和weights相乘。这就是2.5中weights的形状初始化为[hidden_num,n_classes]的原因。然后再经softmax归一化。

		# 定义bp神经网络

		def bp(x, weights, bias):
			x = x[:, :, 0]
			W1 = tf.Variable(tf.random_normal([timesteps, bp_hidden], stddev=0.1))
			B1 = tf.Variable(tf.constant(0.1), [bp_hidden])
			W2 = tf.Variable(tf.random_normal([bp_hidden, num_classes], stddev=0.1))
			B2 = tf.Variable(tf.constant(0.1), [num_classes])
			hidden_opt = tf.matmul(x, W1) + B1  # 输入层到隐藏层正向传播
			hidden_opt = tf.nn.relu(hidden_opt)
			return tf.matmul(hidden_opt, W2) + B2  # 隐藏层到输出层正向传播

		logits = bp(X, weights, biases)
		prediction = tf.nn.softmax(logits)

		loss_op = tf.reduce_mean(tf.nn.softmax_cross_entropy_with_logits(
			logits=logits, labels=Y))
		optimizer = tf.train.AdamOptimizer(learning_rate=learning_rate)
		train_op = optimizer.minimize(loss_op)

		correct_pred = tf.equal(tf.argmax(prediction, 1), tf.argmax(Y, 1))

		argmax_prediction = tf.argmax(prediction, 1)
		argmax_y = tf.argmax(Y, 1)

		TP = tf.count_nonzero(argmax_prediction * argmax_y, dtype=tf.float32)
		TN = tf.count_nonzero((argmax_prediction - 1) * (argmax_y - 1), dtype=tf.float32)
		FP = tf.count_nonzero(argmax_prediction * (argmax_y - 1), dtype=tf.float32)
		FN = tf.count_nonzero((argmax_prediction - 1) * argmax_y, dtype=tf.float32)
		Total = TP + TN + FP + FN
		Precision = TP / (TP + FP)
		Recall = TP / (TP + FN)
		F1 = 2 * (Recall * Precision) / (Recall + Precision)
		accuracy = tf.reduce_mean(tf.cast(correct_pred, tf.float32))

		# 初始化变量
		init = tf.global_variables_initializer()

		plotX = []
		losses = []
		accs = []
		# 开始训练
		with tf.Session() as sess:

			# 运行init
			sess.run(init)
			y_pre = np.arange(0, 1)
			for epoch in range(EPOCH):
				print("*" * 10, 'EPOCH :', epoch, "*" * 10)
				step = 0
				for idxs in minibatcher.get_one_batch():
					batch_x, batch_y = X_tr[idxs], y_tr[idxs]
					# reshape数据
					batch_x = batch_x.reshape((-1, timesteps, num_input))
					# 运行train_op
					sess.run(train_op, feed_dict={X: batch_x, Y: batch_y})
					if step % display_step == 0 or step == 1:
						# 计算损失值和精准度
						loss, acc, f1, recall = sess.run([loss_op, accuracy, F1, Recall], feed_dict={X: batch_x,
																									 Y: batch_y})
						losses.append(loss)
						print("Step " + str(step) + ", Minibatch Loss= " + \
							  "{:.4f}".format(loss) + ", Training Accuracy= " + \
							  "{:.3f}".format(acc) + ", F1= " + \
							  "{:.3f}".format(f1) + ", Recall= " + \
							  "{:.3f}".format(recall)
							  )
					step += 1

				acc_te, recall_te, f1_te, y_test_pre, tp, fp, tn, fn = sess.run(
					[accuracy, Recall, F1, prediction, TP, FP, TN, FN], feed_dict={X: X_val, Y:
						y_val})
				accs.append(acc_te)
				myw = fp * 4 + fn
				if epoch == EPOCH - 1:
					y_pre = y_test_pre
				print("Testing Accuracy= " + \
					  "{:.3f}".format(acc_te) + ", Recall= " + \
					  "{:.3f}".format(recall_te) + ", F1= " + \
					  "{:.3f}".format(f1_te) + ", TP=" + \
					  "{0}".format(tp) + ", FP=" + \
					  "{0}".format(fp) + ", TN=" + \
					  "{0}".format(tn) + ", FN=" + \
					  "{0}".format(fn) + ", myWeight=" + \
					  "{0}".format(myw) + ",查准率P=" + \
					  "{:.3f}".format(tp / (tp + fp)) + ",查全率R=" + \
					  "{:.3f}".format(tp / (tp + fn))
					  )
			# 绘制ROC曲线
			y_test = np.zeros([y_pre.shape[0], 1])
			y_predict = np.zeros([y_pre.shape[0], 1])
			print("y_pre的shape：")
			print(y_pre.shape)
			for i in range(y_pre.shape[0]):
				if y_val[i, 1] == 1:
					y_test[i, 0] = 1
				else:
					y_test[i, 0] = 0
				y_predict[i, 0] = y_pre[i, 1]
			print(y_pre[1, :])
			fpr, tpr, threshold = roc_curve(y_test, y_predict)
			print("shape:")
			print(tpr.shape)
			roc_auc = auc(fpr, tpr)  ###计算auc的值
			plt.figure(1)
			plt.plot(fpr, tpr, color='darkorange', label='ROC curve ')  ###假正率为横坐标，真正率为纵坐标做曲线
			plt.xlabel('False Positive Rate')
			plt.ylabel('True Positive Rate')
			plt.title('Receiver operating characteristic example')
			plt.legend(loc="lower right")
			plt.show()
			# 绘制loss值变化
			plt.figure(2)
			plt.plot(np.arange(len(losses)), losses)
			plt.ylabel('loss')
			plt.show()
			# 正确率变化
			plt.figure(3)
			plt.plot(np.arange(len(accs)), accs)
			plt.ylabel('acc')
			plt.show()
			print("Optimization Finished!")
