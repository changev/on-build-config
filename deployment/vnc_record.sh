#!/bin/bash
# This script is learn from ```generate-sol-log.sh.in```

#This script runs in virtualbox or any other machines on which vnodes IP can be found through arp.
#because InfraSIM/vBMC expose the qemu console via VNC port 5901. so we can record VNC via $vBMCIP:5901

#bmc user/password list, "u1:p1 u2:p2 ..."
bmc_account_list="$1"

#Generate sol log for all nodes.
#Use cmd 'arp' to get vnodes' IP and cause don't know nodes' number and when the node get IP by DHCP,
#so retry several time ensure all nodes have got IP.
handled_ip_list=""
vnode_num=0
timeout=0
maxto=40

save_path=$2
if [ "$save_path" == "" ]; then
     echo "[Error] vnc_record.sh :  Wrong usage. please specific target save path for script."
     echo "[Usage] vnc_record.sh \$bmc_account_list \$file_output_folder \$file_prefix "
     exit 1
fi

fname_prefix=$3

pip install virtualenv --upgrade
virtualenv --clear `pwd`/vnc
source `pwd`/vnc/bin/activate
#Install vnc2flv is not installed
pip install vnc2flv
FLVREC=flvrec.py

#Using cutomized flvrec.py, which can do multiple retry when vNode power cycled (adding '-R' option)

pushd $save_path
echo "Download Customized flvrec.py.. "
curl --insecure https://raw.githubusercontent.com/panpan0000/vnc2flv/master/tools/flvrec.py > ./flvrec.py
#wget --no-check-certificate https://raw.githubusercontent.com/panpan0000/vnc2flv/master/tools/flvrec.py # FIXME: the wget always fail without any output when run under ansible for OVA
chmod +x flvrec.py
popd




while [ ${timeout} -ne ${maxto} ]; do
    ip_list=`arp | awk '{print $1}' | xargs`
    echo "IP LIST in ARP( iteration=$timeout): $ip_list"
    for ip in $ip_list; do
        if [[ "${ip%%.*}" == "172" ]] && [[ "$handled_ip_list" != *"$ip"* ]]; then
            ping -c 1 -w 5 $ip 2>&1 > /dev/null
            if [ $? == 0 ]; then
                echo "Find a pingable IP" $ip
                for bmc in $bmc_account_list; do
                    echo "ipmi cmd: ipmitool -I lanplus -H $ip -U XXXXX -P XXXXX -R 1 -N 3 chassis power status"
                    ipmitool -I lanplus -H $ip -U ${bmc%:*} -P ${bmc#*:} -R 1 -N 3 chassis power status |grep on
                    if [ $? == 0 ]; then
                        echo "Find a vNode IP": $ip
                        ######### Record operations ############
                        #
                        # per vBMC OVA design, the BMC IP(ipmi_sim) is the same as Qemu IP
                        #
                        #########################################
                        pushd $save_path
                        FLVREC=./flvrec.py
                        fname=${fname_prefix}_${ip}.flv
                        echo "Recording VNC for vNode IP ": ${ip} ",file save to ${save_path}/${fname}"
                        $FLVREC  -R 10 -o $fname  ${ip}    5901 &
                        popd

                        echo "The number of vnode $ip is $vnode_num"
                        handled_ip_list="$handled_ip_list $ip"
                        vnode_num=$(( ${vnode_num} + 1 ))
                    fi
                done
            fi
        fi
    done
    #after more than 5*40 seconds all nodes are believed have got the IP by DHCP
    sleep 5
    timeout=$(( ${timeout} + 1 ))
done
if [ $vnode_num -eq 0 ]; then
    echo "[Error][VNC-Record] No any node IP found when times up. no flv record available."
fi

deactivate
