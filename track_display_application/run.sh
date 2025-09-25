#!/bin/bash

rm -f *.class

echo "Compiling Java files..."
javac *.java

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
    echo "Running tests..."
    java Test
else
    echo "✗ Compilation failed"
    exit 1
fi
