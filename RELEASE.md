# Release

This repository uses automated build process. The release build is triggered by pushing a release tag e.g.
`git tag release-1` (`1` is a sequence number to trigger the release). 
The release version is supplied from the
[Hypertrace version plugin](https://github.com/hypertrace/hypertrace-gradle-version-settings-plugin)
that uses git history and [semantic versioning settings](./semantic-build-versioning.gradle) to
decide what the next release version should be.

The major version is incremented if commit history contains:
* Prefix `BREAKING CHANGE`
* Feature suffix `!` e.g. `feat!` or `feat(scope)!`

The minor version is incremented if commit history contains:
* `feat` or `feat(scope)`

The patch version is incremented in other cases.

## Print the next release version run:

```bash
./gradlew printVersion -Prelease
```

## Configuration

* Add SSH key
   * Generate SSH key `ssh-keygen -t rsa -b 4096 -m PEM -C <email>` without passphrase
   * Add private key to CircleCI project settings with `github.com` domain: https://app.circleci.com/settings/project/github/Traceableai/opentelemetry-javaagent/ssh
   * Add public key to Github project deploy keys and check "allow write access" https://github.com/Traceableai/opentelemetry-javaagent/settings/keys
   * Add fingerprint from CircleCI project settings to `./circleci/config.yml`
* Configure CI to release on `release-` tag or merge to the main branch.
* Configure CI to have access to bintray. E.g. add `hypertrace-publishing` context to the publish job.
* Configure gradle 
   * Add `org.hypertrace.version-settings` [Hypertrace version plugin](https://github.com/hypertrace/hypertrace-gradle-version-settings-plugin)
   * Add `org.hypertrace.publish-plugin` [Hypertrace publish plugin](https://github.com/hypertrace/hypertrace-gradle-publish-plugin)
