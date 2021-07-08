# Release

This repository uses automated release process. The release build is triggered by pushing a release tag e.g.
`git tag 0.2.0 && git push origin 0.2.0` to release version `0.2.0`. 

The version is supplied from the
[Hypertrace version plugin](https://github.com/hypertrace/hypertrace-gradle-version-settings-plugin)
that uses git history (e.g. the latest tag) to derive the version. For instance if the last
commit has a tag then the version from tag is used. If the last commit does not have a tag then
the last tag version with `-SNAPSHOT` is used.

## Print the current version:

```bash
./gradlew printVersion # -Prelease - prints the next release version, it's not used at the moment.
```

## Release CI job

The release CI job performs following actions:
1. runs unit and smoke tests
2. publishes java artifacts to maven repository
3. publishes docker image
4. creates GitHub release and uploads java agent to assets

Run `./gradlew publishToMavenLocal` and `ls ~/.m2/repository/org/hypertrace/agent` to find out which
artifacts are being published.
