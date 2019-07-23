import pandas as pd
import numpy as np
from time import time
from targetEncoder import TargetEncoder
from sklearn.preprocessing import StandardScaler
import configparser

class Datapro(object):
    def dataprocess(self):
        print('loading data...')
        config = configparser.ConfigParser()
        config.read('./config.ini')

        col_names = ["duration", "protocol_type", "service", "flag", "src_bytes",
                     "dst_bytes", "land", "wrong_fragment", "urgent", "hot", "num_failed_logins",
                     "logged_in", "num_compromised", "root_shell", "su_attempted", "num_root",
                     "num_file_creations", "num_shells", "num_access_files", "num_outbound_cmds",
                     "is_host_login", "is_guest_login", "count", "srv_count", "serror_rate",
                     "srv_serror_rate", "rerror_rate", "srv_rerror_rate", "same_srv_rate",
                     "diff_srv_rate", "srv_diff_host_rate", "dst_host_count", "dst_host_srv_count",
                     "dst_host_same_srv_rate", "dst_host_diff_srv_rate", "dst_host_same_src_port_rate",
                     "dst_host_srv_diff_host_rate", "dst_host_serror_rate", "dst_host_srv_serror_rate",
                     "dst_host_rerror_rate", "dst_host_srv_rerror_rate", "label"]
        DoS = ['back.', 'land.', 'neptune.', 'pod.', 'smurf.', 'teardrop.']
        R2L = ['ftp_write.', 'guess_passwd.', 'imap.', 'multihop.', 'phf.', 'spy.', 'warezclient.', 'warezmaster.']
        U2R = ['buffer-overflow.', 'loadmodule.', 'perl.', 'rootkit.']
        Probe = ['ipsweep.', 'nmap.', 'portsweep.', 'satan.']

        kdd_data = pd.read_csv(config.get('DataPath', 'traindatapath'), header=None, names=col_names)

        kdd_data.loc[kdd_data['label'] != 'normal.', 'label'] = 0
        kdd_data.loc[kdd_data['label'] == 'normal.', 'label'] = 1
        kk = kdd_data.loc[kdd_data['label'] == 1, :]
        kdd_data = kk.append(kdd_data)

        print('normal:', len(kdd_data[kdd_data['label'] == 1]), 'abnormal:', len(kdd_data[kdd_data['label'] != 1]))
        useful_columns = ['service', 'src_bytes', 'dst_host_diff_srv_rate', \
                          'dst_host_rerror_rate', 'dst_bytes', 'hot', 'num_failed_logins', 'dst_host_srv_count',
                          'label']

        kdd_data = kdd_data[useful_columns]

        print('shuffle train df...')
        np.random.seed(1)

        tr_ratio = float(config.get('Parameters', 'train_ratio'))
        tr_len = int(len(kdd_data) * (tr_ratio))
        print('training len:{},val len:{}'.format(tr_len, len(kdd_data) - tr_len))

        print('encoding categorical features...')
        print(kdd_data.groupby('service').mean().size)
        target_encoder = TargetEncoder(kdd_data[:tr_len], kdd_data[tr_len:], 10, 10, 0.01)
        service_encode = target_encoder.encode1col('service')
        kdd_data['service'] = service_encode
        features = ['service', 'src_bytes', 'dst_host_diff_srv_rate', 'dst_host_rerror_rate', 'dst_bytes', 'hot',
                    'num_failed_logins', 'dst_host_srv_count', 'label']

        data = np.array(kdd_data[features])
        np.random.shuffle(data)
        data_train = data[:-tr_len, :]
        data_test = data[tr_len:, :]
        X_tr = data_train[:, :-1]
        X_val = data_test[:, :-1]
        y_tr = data_train[:, -1:]
        y_val = data_test[:, -1:]

        def bzone(X, y):
            # 将label转化为one-hot编码
            y_one_hot = np.zeros([y.shape[0], 2])
            for i in range(y.shape[0]):
                if y[i, 0] == 1:
                    y_one_hot[i, 1] = 1
                else:
                    y_one_hot[i, 0] = 1

            scaler = StandardScaler()
            scaler.fit(X)
            X = scaler.transform(X)
            X = X.reshape([-1, X.shape[1], 1])
            return X, y_one_hot

        X_tr, y_tr = bzone(X_tr, y_tr)
        X_val, y_val = bzone(X_val, y_val)

        print('save data...')
        np.save(config.get('DataPath', 'X_tr_path'), X_tr)
        np.save(config.get('DataPath', 'y_tr_path'), y_tr)
        np.save(config.get('DataPath', 'X_val_path'), X_val)
        np.save(config.get('DataPath', 'y_val_path'), y_val)
