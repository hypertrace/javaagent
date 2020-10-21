
.PHONY: build
build:
	# TODO remove once https://github.com/Traceableai/opentelemetry-javaagent/issues/46 is fixed
	./gradlew :instrumentation:servlet:servlet-3.1:assemble build

.PHONY: test
test:
	./gradlew check

.PHONY: format
format:
	./gradlew spotlessApply

.PHONY: clean
clean:
	./gradlew clean
