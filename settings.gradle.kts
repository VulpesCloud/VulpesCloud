rootProject.name = "VulpesCloud"
include("VulpesCloud-api")
include("VulpesCloud-node")
include("VulpesCloud-launcher")
include("VulpesCloud-wrapper")
include("VulpesCloud-connector")
include("VulpesCloud-bridge")
include("VulpesCloud-modules")
include("VulpesCloud-modules:VulpesCloud-example-module")
findProject(":VulpesCloud-modules:VulpesCloud-example-module")?.name = "VulpesCloud-example-module"
