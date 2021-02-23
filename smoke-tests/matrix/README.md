# Smoke Test Environment Matrix
This project builds docker images containing a simple test web application deployed to various
application servers or servlet containers. For each server several relevant versions are chosen.
In addition we build separate images for several support major java versions.
This way we can test our agent with many different combinations of runtime environment,
its version and running on different JVM versions from different vendors.

Images from this project are only published when changes are made in `'smoke-tests/matrix/**'`
See build workflow `.github/workflows/build-test-matrix.yaml` for details.