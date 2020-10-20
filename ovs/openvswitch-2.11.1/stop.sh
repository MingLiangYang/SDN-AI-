#!/bin/bash
set -x


set +e
rm -rf /var/log/gary.log
rm -rf /var/log/kern.log
killall ovsdb-server
killall ovs-vswitchd
apt-get remove openvswitch-common openvswitch-datapath-dkms openvswitch-controller openvswitch-pki openvswitch-switch
ovs-dpctl del-dp ovs-system
modprobe -r openvswitch 
set -e