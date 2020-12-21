# Hotfixes for OpenTelemetry javaagent

All fixes for the core [open-telemetry/opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
should be submitted to the upstream project. However, merge and release into upstream might take some time
therefore urgent hotfixes can be submitted to the fork [hypertrace/opentelemetry-java-instrumentation](https://github.com/hypertrace/opentelemetry-java-instrumentation).

Changes to the fork are done in a maintenance branches e.g. `v0.13.x` and locally tested and released.
The fixed artifacts are published to the [Hypertrace Binray](https://bintray.com/hypertrace/maven).
Then the released artifacts can be consumed by version(s) change in gradle scripts in this repository.

## Hotfix release of hypertrace/opentelemetry-java-instrumentation

This [commit](https://github.com/hypertrace/opentelemetry-java-instrumentation/commit/88c2b113d0cc2fad01ddde9cccdab3b09cbfb6da)
shows an example of release configuration for `javaagent` artifact. 

```bash
git checkout -b v0.13.x
# make required changes and run tests locally
git tag 0.13.2-ht # use -ht suffix to denote that this is forked version
ORG_GRADLE_PROJECT_publishUser=<bintray-user> ORG_GRADLE_PROJECT_publishApiKey=<bintray-api-key> ./gradlew bintrayPublish -Prelease.useLastTag=true
```
