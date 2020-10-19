rootProject.name = "hypertrace-otel-agent"

include(":javaagent")
include("instrumentation")
include("instrumentation:servlet:servlet-3.0")
findProject(":instrumentation:servlet:servlet-3.0")?.name = "servlet-3.0"
include("instrumentation:servlet:servlet-2.2")
findProject(":instrumentation:servlet:servlet-2.2")?.name = "servlet-2.2"
include("smoke-tests")
include("blocking")
include("javaagent-tooling")
include("javaagent-bootstrap")
include("javaagent-core")
