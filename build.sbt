ThisBuild / version := "0.3.0"
ThisBuild / scalaVersion := "3.3.5"
ThisBuild / crossScalaVersions := List("3.3.5")
ThisBuild / organization := "com.github.kurgansoft"

val zioVersion = "2.1.26"
val sttpVersion = "4.0.26"
val tapirVersion = "1.13.27"

lazy val common = crossProject(JSPlatform, JVMPlatform).in(file("common")).
  settings(
    name := "common",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio" % zioVersion,
      "dev.zio" %%% "zio-test" % zioVersion % Test,
      "dev.zio" %%% "zio-schema-json" % "1.8.3", // it has to be the exact version that zio-http uses
      "com.softwaremill.sttp.client4" %%% "zio" % sttpVersion,
      "com.softwaremill.sttp.client4" %%% "zio-json" % sttpVersion,
      "com.lihaoyi" %%% "pprint" % "0.9.6"
    )
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := false
   )

lazy val backend = project.in(file("backend")).settings(
  name := "backend",
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio-http" % "3.11.0",
    "com.lihaoyi" %% "os-lib" % "0.11.8",
    "dev.zio" %% "zio-mock" % "1.0.0-RC12"
  ),
).dependsOn(common.jvm)

val uiGlueCommitHash: String = "59ad8414ba2910d574eca9dac430a1546f43c409"

lazy val ui = project.in(file("ui")).settings(
  name := "ui",
  Compile / scalaJSUseMainModuleInitializer:= false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "2.8.1",
    "com.github.japgolly.scalajs-react" %%% "core" % "2.1.3",
    "com.github.kurgansoft.uiglue" %%% "uiglue" % uiGlueCommitHash,
    "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client4" % tapirVersion,
    "com.softwaremill.sttp.tapir" %%% "tapir-json-zio" % tapirVersion excludeAll(
      ExclusionRule("dev.zio", "zio-json_sjs1_3")
    )
  ),
  scalacOptions ++= Seq(
    "-Xmax-inlines", "100"
  ),
  resolvers += "jitpack" at "https://jitpack.io"
).dependsOn(common.js).enablePlugins(ScalaJSPlugin)