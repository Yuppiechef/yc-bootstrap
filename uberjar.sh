#!/bin/bash

rm -rf classes
mkdir classes
shadow-cljs release app
clj -M -e "(compile 'start)"
clj -M:uberdeps --main-class start