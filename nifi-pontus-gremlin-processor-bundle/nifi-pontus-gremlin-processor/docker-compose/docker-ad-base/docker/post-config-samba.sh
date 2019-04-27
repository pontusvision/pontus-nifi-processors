#!/bin/bash -x

export SHOST=`hostname -s`
export HOSTNAME=`hostname -f`
export IPADDR=`hostname -i`
export IPADDR_REV_LOOKUP=RR=$(IFS=. eval 'set -- ${IPADDR}'; echo "$4.$3.$2.$1.in-addr.arpa")
export PATH=$PATH:/opt/pontus/pontus-samba/current/bin/:/opt/pontus/pontus-samba/current/sbin/:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/root/bin
samba-tool domain provision --domain=PONTUSVISION --host-name=$HOSTNAME --host-ip $IPADDR --site=PONTUSVISION.COM --adminpass=pa55wordpa55wordPASSWD999 --krbtgtpass=pa55wordpa55wordPASSWD999 --machinepass=pa55wordpa55wordPASSWD999 --dnspass=pa55wordpa55wordPASSWD999  --ldapadminpass=pa55wordpa55wordPASSWD999 --server-role=dc --use-rfc2307 --realm=PONTUSVISION.COM



samba_upgradedns --dns-backend=SAMBA_INTERNAL

samba-tool dns add 127.0.0.1  pontusvision.com ${SHOST} A $IPADDR --password=pa55wordpa55wordPASSWD999 --username=Administrator

samba-tool dns zonecreate  `hostname` $IPADDR_REV_LOOKUP --password=pa55wordpa55wordPASSWD999 --username=Administrator
samba-tool dns add 127.0.0.1 $IPADDR_REV_LOOKUP $IPADDR_REV_LOOKUP  PTR ${SHOST}.pontusvision.com -UAdministrator --password=pa55wordpa55wordPASSWD999
samba-tool user setexpiry Administrator --noexpiry



samba-tool user create kafka/${SHOST}.pontusvision.com pa55wordBig4Data4Admin
samba-tool spn add kafka/${SHOST}.pontusvision.com  kafka/${SHOST}.pontusvision.com --realm=PONTUSVISION.COM
samba-tool domain exportkeytab /etc/security/keytabs/kafka.service.keytab --principal=kafka/${SHOST}.pontusvision.com@PONTUSVISION.COM
samba-tool user setexpiry kafka/`hostname -f` --noexpiry
samba-tool user create hbase/${SHOST}.pontusvision.com pa55wordBig4Data4Admin
samba-tool spn add hbase/${SHOST}.pontusvision.com  hbase/${SHOST}.pontusvision.com --realm=PONTUSVISION.COM
samba-tool domain exportkeytab /etc/security/keytabs/hbase.service.keytab --principal=hbase/${SHOST}.pontusvision.com@PONTUSVISION.COM
samba-tool user setexpiry hbase/`hostname -f` --noexpiry
samba-tool user create zookeeper/${SHOST}.pontusvision.com pa55wordBig4Data4Admin
samba-tool spn add zookeeper/${SHOST}.pontusvision.com zookeeper/${SHOST}.pontusvision.com --realm=PONTUSVISION.COM
samba-tool domain exportkeytab /etc/security/keytabs/zookeeper.service.keytab --principal=zookeeper/${SHOST}.pontusvision.com@PONTUSVISION.COM
samba-tool user setexpiry zookeeper/`hostname -f` --noexpiry

