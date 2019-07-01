#!/bin/bash

export HOSTNAME=`hostname -f`
export IPADDR=`hostname -i`
export PATH=$PATH:/opt/pontus/pontus-samba/current/bin/:/opt/pontus/pontus-samba/current/sbin/:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/root/bin
samba-tool domain provision --domain=PONTUSVISION --host-name=$HOSTNAME --host-ip $IPADDR --site=PONTUSVISION.COM --adminpass=pa55wordpa55wordPASSWD999 --krbtgtpass=pa55wordpa55wordPASSWD999 --machinepass=pa55wordpa55wordPASSWD999 --dnspass=pa55wordpa55wordPASSWD999  --ldapadminpass=pa55wordpa55wordPASSWD999 --server-role=dc --use-rfc2307 --realm=PONTUSVISION.COM



samba_upgradedns --dns-backend=SAMBA_INTERNAL

samba-tool dns add 127.0.0.1  pontusvision.com pontus-sandbox A `hostname -i` --password=pa55wordpa55wordPASSWD999 --username=Administrator

samba-tool dns zonecreate  `hostname` 2.0.17.172.in-addr.arpa --password=pa55wordpa55wordPASSWD999 --username=Administrator
samba-tool dns add 127.0.0.1 2.0.17.172.in-addr.arpa 2.0.17.172.in-addr.arpa  PTR pontus-sandbox.pontusvision.com -UAdministrator --password=pa55wordpa55wordPASSWD999
samba-tool user setexpiry Administrator --noexpiry



samba-tool user create kafka/pontus-sandbox.pontusvision.com pa55wordBig4Data4Admin
samba-tool spn add kafka/pontus-sandbox.pontusvision.com  kafka/pontus-sandbox.pontusvision.com --realm=PONTUSVISION.COM
samba-tool domain exportkeytab /etc/security/keytabs/kafka.service.keytab --principal=kafka/pontus-sandbox.pontusvision.com@PONTUSVISION.COM
samba-tool user setexpiry kafka/`hostname -f` --noexpiry
samba-tool user create hbase/pontus-sandbox.pontusvision.com pa55wordBig4Data4Admin
samba-tool spn add hbase/pontus-sandbox.pontusvision.com  hbase/pontus-sandbox.pontusvision.com --realm=PONTUSVISION.COM
samba-tool domain exportkeytab /etc/security/keytabs/hbase.service.keytab --principal=hbase/pontus-sandbox.pontusvision.com@PONTUSVISION.COM
samba-tool user setexpiry hbase/`hostname -f` --noexpiry
samba-tool user create zookeeper/pontus-sandbox.pontusvision.com pa55wordBig4Data4Admin
samba-tool spn add zookeeper/pontus-sandbox.pontusvision.com zookeeper/pontus-sandbox.pontusvision.com --realm=PONTUSVISION.COM
samba-tool domain exportkeytab /etc/security/keytabs/zookeeper.service.keytab --principal=zookeeper/pontus-sandbox.pontusvision.com@PONTUSVISION.COM
samba-tool user setexpiry zookeeper/`hostname -f` --noexpiry

