#!/bin/ksh
judge=$1
jq '. | length' ./Out.json > tmp
read json_length < tmp
c=0
json_length=`expr $json_length - 1`
while [ $c -le $json_length ]
do
	jq ".[$c].contextProperties | length" ./Out.json > tmp
	read properties_length < tmp
	echo $properties_length
	s=$properties_length
	while [ $s -ge 0 ]
	do
		jq .[$c].contextProperties[$s].value ./Out.json | sed 's/"//g' > tmp
		read value < tmp
		echo $value
		echo $judge 
		if [ "$value" == "$judge" ]; then 
			jq .[$c].id ./Out.json | sed 's/"//g' > tmp
			echo "===================================="
			exit 0
		fi
		s=`expr $s - 1`
	done
	echo "welcome $c times"
	c=`expr $c + 1`
done
