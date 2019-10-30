./boot.sh
./configure --with-linux=/lib/modules/$(uname -r)/build
make -j4
make install
make modules_install
modprobe openvswitch
lsmod | grep openvswitch
mkdir -p /usr/local/etc/openvswitch
ovsdb-tool create /usr/local/etc/openvswitch/conf.db vswitchd/vswitch.ovsschema
mkdir -p /usr/local/var/run/openvswitch
ovsdb-server --remote=punix:/usr/local/var/run/openvswitch/db.sock \
    --remote=db:Open_vSwitch,Open_vSwitch,manager_options \
    --private-key=db:Open_vSwitch,SSL,private_key \
    --certificate=db:Open_vSwitch,SSL,certificate \
    --bootstrap-ca-cert=db:Open_vSwitch,SSL,ca_cert \
    --pidfile --detach
ovs-vsctl --no-wait init
ovs-vswitchd --pidfile --detach --log-file
ps -ea | grep ovs


ovs-vsctl add-br br0
ovs-vsctl add-port br0 veth1
ovs-vsctl set interface veth1 type=internal
ovs-vsctl add-port br0 veth0
ovs-vsctl set interface veth0 type=internal

ifconfig veth0 192.168.181.204 netmask 255.255.255.0
ifconfig veth1 192.168.181.205 netmask 255.255.255.0

ping -I 192.168.181.205 -c 10 192.168.181.204

sudo ovs-dpctl del-dp ovs-system


date +%s>>my
top -b -n 1 | grep ovs  >> my
ps -eL

gcc test.c -o test


cat /proc/interrupts |grep ens33

lspci | grep -i net




sudo killall ovsdb-server
sudo killall ovs-vswitchd
sudo apt-get remove openvswitch-common openvswitch-datapath-dkms openvswitch-controller openvswitch-pki openvswitch-switch
ovs-dpctl del-dp ovs-system
modprobe -r openvswitch 
rmmod openvswitch


sudo ip tuntap add dev tap1 mod tap
sudo ip link set tap1 up
sudo ip tuntap add dev tap2 mod tap
sudo ip link set tap2 up
sudo ip tuntap add dev tap3 mod tap
sudo ip link set tap3 up
sudo ip tuntap add dev tap4 mod tap
sudo ip link set tap4 up
sudo ip tuntap add dev tap5 mod tap
sudo ip link set tap5 up


/etc/init.d/rsyslog restart #重启日志系统
