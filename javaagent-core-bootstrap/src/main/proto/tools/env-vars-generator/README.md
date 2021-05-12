# Env vars generator

This tool allows us to generate the [list of env vars](../../ENV_VARS.md) from the [config.proto](../../config.proto) file.

## Usage

You can run it using docker (if not familiar with Golang):

```bash
docker build -t hypertrace/agent-config/env-vars-generator .
docker run \
-v $(PWD)/../../ENV_VARS.md:/usr/local/ENV_VARS.md \
-v $(PWD)/../../config.proto:/usr/local/config.proto \
hypertrace/agent-config/env-vars-generator \
-o /usr/local/ENV_VARS.md \
/usr/local/config.proto
```

or using golang directly

```bash
cd ../.. # back to the main folder
go run tools/env-vars-generator/main.go config.proto
```
