#!/bin/sh

cd Protos
protoc -I=. --java_out=../src ./AvalancheMessages.proto
