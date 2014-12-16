#!/bin/bash

#This script sets the preferred options for tomcat7 on the ediosn

#tomcat defaults file
f=$1

#existing JAVA_OPTS
old="-Djava.awt.headless=true -Xmx128m -XX:+UseConcMarkSweepGC"

#new JAVA_OPTS
new="-Djava.awt.headless=true -Xmx256m -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode"

#Replace string and remove old file.
sed "s/$old/$new/g" $f > $f.new
                rm $f
                mv $f.new $f
                echo $f done
