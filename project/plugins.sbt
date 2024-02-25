addSbtPlugin("dev.zio" % "zio-sbt-ecosystem" % "0.4.0-alpha.23")
addSbtPlugin("dev.zio" % "zio-sbt-ci" % "0.4.0-alpha.23")
addSbtPlugin("dev.zio" % "zio-sbt-website" % "0.4.0-alpha.23")
addSbtPlugin("org.jetbrains.scala" % "sbt-ide-settings" % "1.1.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.10.0-13-8a23662c-20240226-0032")
//addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")

resolvers ++= Resolver.sonatypeOssRepos("public")
