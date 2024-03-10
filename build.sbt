addCommandAlias("format", "scalafmtAll; scalafmtSbt")

inThisBuild(
  List(
    name := "unistore",
    organization := "com.lumidion",
    description := "Unistore is a library for a parsing single files in a cloud-agnostic manner, most commonly for application configuration.",
    homepage := Some(url("https://www.lumidion.com")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        id = "andrapyre",
        name = "David Doyle",
        email = "david@lumidion.com",
        url = url("https://www.lumidion.com/about")
      )
    )
  )
)

lazy val root = (project in file("."))
  .enablePlugins(
//    WebsitePlugin,
    ZioSbtEcosystemPlugin,
    ZioSbtCiPlugin
  )
  .settings(
    name := "unistore",
    crossScalaVersions := Seq(scala3.value, scala213.value),
    scalacOptions ++= {
      if (scalaVersion.value == scala3.value) {
        Seq(
          "-Werror",
          "-Wunused:all",
          "-deprecation"
        )
      } else
        Seq(
          "-Xsource:3"
        )
    },
    libraryDependencies := Seq(
      "dev.zio" %% "zio-aws-s3" % "7.21.15.7",
      "dev.zio" %% "zio-aws-sts" % "7.21.15.7",
      "dev.zio" %% "zio-aws-netty" % "7.21.15.7"
    )
  )
