#!/bin/bash

if [[ -f /pre-config-samba.sh ]]; then
  /post-config-samba.sh && rm /pre-config-samba.sh

fi
samba -F
