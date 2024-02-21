addCommandAlias("format", "scalafmtAll; scalafmtSbt")

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

ThisBuild / name := "unistore"

ThisBuild / scalacOptions ++= Seq(
  "-Werror",
  "-Wunused:all",
  "-deprecation",
  "-source:future"
)

lazy val root = (project in file("."))
  .enablePlugins(
//    WebsitePlugin,
    ZioSbtEcosystemPlugin,
    ZioSbtCiPlugin
  )
  .settings(
    scalaVersion := scala3.value,
    organization := "com.lumidion",
    name := "unistore",
    libraryDependencies := Seq(
      "dev.zio" %% "zio-aws-s3" % "7.21.15.7",
      "dev.zio" %% "zio-aws-sts" % "7.21.15.7",
      "dev.zio" %% "zio-aws-netty" % "7.21.15.7"
    )
  )
