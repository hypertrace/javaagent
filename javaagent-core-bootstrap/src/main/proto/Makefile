.PHONY: default help lint generate-env-vars init-git-submodule

default: help

help: ## Prints this help.
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-18s\033[0m %s\n", $$1, $$2}'

lint: ## Lints the proto files.
	protolint config.proto

generate-env-vars: init-git-submodule ## Generates the ENV_VARS.md with all environment variables.
	docker build -t hypertrace/agent-config/env-vars-generator tools/env-vars-generator
	docker run \
	-v $(PWD)/ENV_VARS.md:/usr/local/ENV_VARS.md \
	-v $(PWD)/config.proto:/usr/local/config.proto \
	hypertrace/agent-config/env-vars-generator \
	-o /usr/local/ENV_VARS.md \
	/usr/local/config.proto

init-git-submodule:
	git submodule update --init
