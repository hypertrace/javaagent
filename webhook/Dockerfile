FROM golang:1.15.6-alpine AS build
ENV CGO_ENABLED 0

RUN apk add git make openssl

WORKDIR /go/src/github.com/hypertrace/javaagent-webhook
ADD . .
RUN go get -v ./...
RUN go build -v -o javaagent-webhook ./cmd/
RUN go test -v ./... -cover

FROM scratch
WORKDIR /app
COPY --from=build /go/src/github.com/hypertrace/javaagent-webhook/javaagent-webhook .
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
CMD ["/app/javaagent-webhook"]
