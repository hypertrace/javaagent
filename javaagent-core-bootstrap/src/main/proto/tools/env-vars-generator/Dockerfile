FROM golang:1.15-alpine

RUN apk add --update git

ARG WORKDIR=/go/src/github.com/hypertrace/agent-config

RUN mkdir -p $WORKDIR/tools/env-vars-generator

WORKDIR $WORKDIR

COPY . $WORKDIR/tools/env-vars-generator

RUN go get ./...

ENTRYPOINT ["/usr/local/go/bin/go", "run", "tools/env-vars-generator/main.go"]