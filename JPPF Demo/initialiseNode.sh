#! /usr/bin/bash

cd ./JPPF-6.2-node;
for i in $(seq 1 $1); do
	xterm -hold -title "Node $i" -e "./startNode.sh" &
done;
