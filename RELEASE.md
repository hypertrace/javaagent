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
./gradlew printVersion # -Prelease - prints the release version, it's not used at the moment.
```

## Release CI job

The release CI job performs the following actions:
1. creates git tag for the next version
2. publishes artifacts to remove github repository with the new version
3. pushes the new git tag to the origin repository

Run `./gradlew publishToMavenLocal` and `ls ~/.m2/repository/org/hypertrace/agent` to find out which
artifacts are being published.

## Configuration

* Add SSH key
   * Generate SSH key `ssh-keygen -t rsa -b 4096 -m PEM -C <email>` without passphrase
   * Add private key to CircleCI project settings with `github.com` domain: https://app.circleci.com/settings/project/github/hypertrace/javaagent/ssh
   * Add public key to Github project deploy keys and check "allow write access" https://github.com/hypertrace/javaagent/settings/keys
   * Add fingerprint from CircleCI project settings to `./circleci/config.yml`
* Configure CI to release on `release-` tag or merge to the main branch.
* Configure CI to have access to bintray. E.g. add `hypertrace-publishing` context to the publish job.
* Configure gradle 
   * Add `org.hypertrace.version-settings` [Hypertrace version plugin](https://github.com/hypertrace/hypertrace-gradle-version-settings-plugin)
   * Add `org.hypertrace.publish-plugin` [Hypertrace publish plugin](https://github.com/hypertrace/hypertrace-gradle-publish-plugin)
