load("//:generate_workspace.bzl", "generated_java_libraries")

generated_java_libraries()

java_import(
    name = "org_iota_jota",
    jars = [
        "jota-1.0.0-beta8-jar-with-dependencies.jar",
    ],
)

java_binary(
    name = "Test",
    srcs = ["Test.java"],
    main_class = "org.iota.compass.thirdparty.Test",
    visibility = ["//visibility:public"],
    deps = [
        "@org_iota_jota//jar",
        ],

)