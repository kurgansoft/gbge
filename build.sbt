ThisBuild / version := "0.3.0"
ThisBuild / scalaVersion := "3.9.0"
ThisBuild / organization := "com.github.kurgansoft"

val zioVersion = "2.1.27"
val sttpVersion = "4.1.0"

lazy val common = crossProject(JSPlatform, JVMPlatform).in(file("common")).
  settings(
    name := "common",
    scalacOptions ++= Seq("-experimental"),
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio" % zioVersion,
      "dev.zio" %%% "zio-test" % zioVersion % Test,
      "dev.zio" %%% "zio-blocks-next-schema" % "0.0.56",
      "com.softwaremill.sttp.client4" %%% "zio" % sttpVersion,
      "com.lihaoyi" %%% "pprint" % "0.9.6"
    )
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := false
   )

lazy val backend = project.in(file("backend")).settings(
  name := "backend",
  scalacOptions ++= Seq("-experimental"),
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio-http" % "3.11.6",
    "com.lihaoyi" %% "os-lib" % "0.11.9",
    "dev.zio" %% "zio-mock" % "1.0.0-RC12"
  ),
).dependsOn(common.jvm)

val uiGlueCommitHash: String = "59ad8414ba2910d574eca9dac430a1546f43c409"

lazy val ui = project.in(file("ui")).settings(
  name := "ui",
  scalacOptions ++= Seq(
    "-experimental",
    "-Xmax-inlines",
    "100"
  ),
  Compile / scalaJSUseMainModuleInitializer:= false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "2.8.1",
    "com.github.japgolly.scalajs-react" %%% "core" % "2.1.3",
    "com.github.kurgansoft.uiglue" %%% "uiglue" % uiGlueCommitHash,
  ),

  resolvers += "jitpack" at "https://jitpack.io"
).dependsOn(common.js).enablePlugins(ScalaJSPlugin)