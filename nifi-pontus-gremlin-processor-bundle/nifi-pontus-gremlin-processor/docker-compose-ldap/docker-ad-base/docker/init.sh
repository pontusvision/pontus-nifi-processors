#!/bin/bash

if [[ ! -f /samba-configured ]]; then
  touch /samba-configured
  rm -rf /etc/pki/*
  /create-keys.sh
  /pre-config-samba.sh
  /post-config-samba.sh

fi
samba -F

