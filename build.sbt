import xerial.sbt.Sonatype.sonatypeCentralHost

addCommandAlias("format", "scalafmtAll; scalafmtSbt")
addCommandAlias("sontest", "reload; clean; publishSigned; sonatypeBundleRelease")

//ThisBuild / version := "0.1.0-SNAPSHOT"

inThisBuild(
  List(
    name := "unistore",
    organization := "com.lumidion",
    homepage := Some(url("https://github.com/lumidion/unistore")),
    // Alternatively License.Apache2 see https://github.com/sbt/librarymanagement/blob/develop/core/src/main/scala/sbt/librarymanagement/License.scala
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        id = "andrapyre",
        name = "David Doyle",
        email = "david@lumidion.com",
        url = url("https://www.lumidion.com/about")
      )
    ),
    sonatypeCredentialHost := sonatypeCentralHost
  )
)

lazy val root = (project in file("."))
  .enablePlugins(
//    WebsitePlugin,
    ZioSbtEcosystemPlugin,
    ZioSbtCiPlugin
  )
  .settings(
//    publishTo := Some(
//      Resolver.file(
//        "sonatype-local-bundle",
//        baseDirectory.value / "target" / "sonatype-staging" / "0.1.0"
//      )
//    ),
//    version := "0.1.0",
//    publishMavenStyle := true,
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
