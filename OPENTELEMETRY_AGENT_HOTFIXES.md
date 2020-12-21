# Hotfixes for OpenTelemetry javaagent

All fixes to the core [open-telemetry/opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
should be submitted to the upstream project. However, merge and release into upstream might take some time
therefore urgent hotfixes can be submitted to a fork [hypertrace/opentelemetry-java-instrumentation](https://github.com/hypertrace/opentelemetry-java-instrumentation).

Changes to the fork are done in maintenance branches e.g. `v0.13.x` and locally tested and released from there.
The fixed artifacts are published to the [Hypertrace Binray](https://bintray.com/hypertrace/maven).
Then just change the version(s) in gradle scripts in this repository to consume the fixed version.

## Hotfix release of hypertrace/opentelemetry-java-instrumentation

This [commit](https://github.com/hypertrace/opentelemetry-java-instrumentation/commit/dfb7fcfdff3f04a3076f27dc9962d426180d14a7)
shows an example of release configuration for `javaagent` artifact. 

```bash
git checkout -b v0.13.x
# make required changes and run tests locally
git tag 0.13.2-ht # use -ht suffix to denote that this is forked version
ORG_GRADLE_PROJECT_publishUser=<bintray-user> ORG_GRADLE_PROJECT_publishApiKey=<bintray-api-key> ./gradlew bintrayPublish -Prelease.useLastTag=true
```
