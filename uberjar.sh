#!/bin/bash

rm -rf classes
mkdir classes
clj -M -e "(compile 'start)"
clj -M:uberdeps --main-class start