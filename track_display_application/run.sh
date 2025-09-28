#!/bin/bash

set -e

echo "Track Display Application - Build & Test Script"
echo "=================================================="

# Clean up previous builds
echo "Cleaning up previous builds..."
rm -f *.class

# Compile Java files
echo "Compiling Java files..."
javac *.java

if [ $? -eq 0 ]; then
    echo "Compilation successful"
    
    echo ""
    echo "Running comprehensive test suite..."
    echo "======================================"
    java Test
    
    echo ""
    echo "Build and test completed successfully!"
    echo ""
    echo "Usage Instructions:"
    echo "----------------------"
    echo "To run the Track Display Application:"
    echo "  java Main"
    echo ""
    echo "To run tests only:"
    echo "  java Test"
    echo ""
    echo "Note: Make sure a socket server is running on localhost:532"
    echo "      to send JSON track data to the application."
    
else
    echo "Compilation failed"
    exit 1
fi