#! /usr/bin/bash

./initialiseServer.sh &
./initialiseNode.sh $1
cd ./JPPF-6.2-admin-ui;
ant
