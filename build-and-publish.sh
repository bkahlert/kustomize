#!/bin/sh
docker build -t bkahlert/kustomize:latest .
docker push bkahlert/kustomize:latest
