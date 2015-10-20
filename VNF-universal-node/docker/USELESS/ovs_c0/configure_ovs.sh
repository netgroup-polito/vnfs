#!/bin/bash

start(){
	echo "ovs-vsctl show"
	ovs-vsctl show
	ovs-vsctl add-br br-swt

	ovs-vsctl set bridge br-swt datapath_type=netdev

	for i in `ls /sys/class/net`
	do
		if [ $i != lo -a $i != 'ovs-system' ]
		then
		    ovs-vsctl add-port br-swt $i
		fi
	done

	cp /sbin/dhclient /usr/sbin/dhclient && /usr/sbin/dhclient br-swt -v

}

case $1 in
    start)
        start
    ;;
    stop)
    ;;
    *)
        echo "Usage: $0 {start}"
        exit 2
    ;;
esac

