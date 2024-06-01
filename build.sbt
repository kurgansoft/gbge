ThisBuild / version := "0.3.0"

ThisBuild / scalaVersion := "3.3.0"

val zioVersion = "2.1.1"

lazy val common = crossProject(JSPlatform, JVMPlatform).in(file("common")).
  settings(
    name := "common",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio" % zioVersion,
      "com.lihaoyi" %%% "upickle" % "3.3.1",
      "dev.optics" %%% "monocle-core" % "3.2.0",
      "dev.optics" %%% "monocle-macro" % "3.2.0",
    )
  )
  .jvmSettings(
    target := file("target/jvm")
  )
  .jsSettings(
    target := file("target/js"),
    scalaJSUseMainModuleInitializer := false
   )

lazy val backend = project.in(file("backend")).settings(
  name := "backend",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "cask" % "0.9.2",
    "com.lihaoyi" %% "os-lib" % "0.10.1",
    "org.scalatest" %% "scalatest" % "3.3.0-alpha.1" % Test
  )
).dependsOn(common.jvm)

lazy val ui = project.in(file("ui")).settings(
  name := "ui",
  Compile / scalaJSUseMainModuleInitializer:= false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "2.6.0",
    "com.github.japgolly.scalajs-react" %%% "core" % "2.1.1",
    "com.lihaoyi" %%% "pprint"  % "0.7.1",
    "com.github.kurgansoft" % "uiglue" % "ee37499c4c3dd9b90b273e747cc29bd86c42e5ad",
    "com.softwaremill.sttp.client3" %%% "zio" % "3.9.6",
    "org.scalatest" %%% "scalatest" % "3.3.0-alpha.1" % Test
  ),
  resolvers += "jitpack" at "https://jitpack.io"
).dependsOn(common.js).enablePlugins(ScalaJSPlugin)