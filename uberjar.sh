#!/bin/bash

clj -M -e "(compile 'hello)"
clj -M:uberdeps --main-class hello