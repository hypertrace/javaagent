rootProject.name = "hypertrace-otel-agent"

include(":javaagent")
include("instrumentation")
include("instrumentation:servlet:servlet-3.1")
findProject(":instrumentation:servlet:servlet-3.1")?.name = "servlet-3.1"
include("instrumentation:servlet:servlet-2.3")
findProject(":instrumentation:servlet:servlet-2.3")?.name = "servlet-2.3"
include("smoke-tests")
include("blocking")
include("javaagent-tooling")
include("javaagent-bootstrap")
include("javaagent-core")
