#!/bin/bash

#echo $(grep $(hostname) /etc/hosts | cut -f1) $(hostname -s).pontusvision.com >> /etc/hosts

if [[ -f "/etc/krb5.conf" ]]; then
  mv /etc/krb5.conf /etc/krb5.conf.orig
fi

cat << 'EOF2' >> /etc/krb5.conf
[libdefaults]
  renew_lifetime = 700d
  forwardable = true
  default_realm = PONTUSVISION.COM
  ticket_lifetime = 24d
  dns_lookup_realm = false
  dns_lookup_kdc = false
  default_ccache_name = /tmp/krb5cc_%{uid}
  #default_tgs_enctypes = aes des3-cbc-sha1 rc4 des-cbc-md5
  #default_tkt_enctypes = aes des3-cbc-sha1 rc4 des-cbc-md5

[domain_realm]
  pontusvision.com = PONTUSVISION.COM

[logging]
  default = FILE:/var/log/krb5kdc.log
  admin_server = FILE:/var/log/kadmind.log
  kdc = FILE:/var/log/krb5kdc.log

[realms]
  PONTUSVISION.COM = {
    admin_server = $(hostname -s).pontusvision.com
    kdc = $(hostname -s).pontusvision.com
  }

EOF2

>  /etc/samba/smb.conf
cat << EOF2 >> /etc/samba/smb.conf
# Global parameters
[global]
        dns forwarder = 8.8.8.8
        netbios name = PONTUS-SANDBOX
        realm = PONTUSVISION.COM
        server role = active directory domain controller
        workgroup = PONTUSVISION
        idmap_ldb:use rfc2307 = yes
        kdc:user ticket lifetime = 10000
        kdc:service ticket lifetime = 10000
        kdc:renewal lifetime = 168000
        tls enabled  = yes
        tls keyfile  = /etc/pki/private/$(hostname -s).pem
        tls certfile = /etc/pki/private/$(hostname -s).crt
        tls cafile   = /etc/pki/private/root-ca.crt




[netlogon]
        path = /var/locks/sysvol/pontusvision.com/scripts
        read only = No

[sysvol]
        path = /var/locks/sysvol
        read only = No
EOF2



if [[ ! -d '/etc/security/keytabs' ]] ; then 
  mkdir /etc/security/keytabs 
fi


mkdir -p /var/locks/sysvol/pontusvision.com/scripts

