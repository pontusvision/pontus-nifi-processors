#!/bin/bash

while [[ '1' != $(netstat -an |grep '0.0.0.0:636'|wc -l) ]]; do
  sleep 5;
done
