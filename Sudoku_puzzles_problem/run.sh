#!/bin/bash

# This script runs the solution for Problem 1.
javac -cp . *.java
java -cp . -ea Main
java -cp . -ea Test

return_code=$?
echo $return_code
exit $return_code
