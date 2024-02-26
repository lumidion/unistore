addCommandAlias("format", "scalafmtAll; scalafmtSbt")

inThisBuild(
  List(
    name := "unistore",
    organization := "com.lumidion",
    homepage := Some(url("https://github.com/lumidion/unistore")),
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
    scalaVersion := scala3.value,
    scalacOptions ++= Seq(
      "-Werror",
      "-Wunused:all",
      "-deprecation",
      "-source:future"
    ),
    libraryDependencies := Seq(
      "dev.zio" %% "zio-aws-s3" % "7.21.15.7",
      "dev.zio" %% "zio-aws-sts" % "7.21.15.7",
      "dev.zio" %% "zio-aws-netty" % "7.21.15.7"
    )
  )
