ThisBuild / version := "0.3.0"

ThisBuild / scalaVersion := "3.3.5"

val zioVersion = "2.1.21"
val sttpVersion = "4.0.11"

lazy val common = crossProject(JSPlatform, JVMPlatform).in(file("common")).
  settings(
    name := "common",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio" % zioVersion,
      "dev.zio" %%% "zio-test" % zioVersion % Test,
      "dev.zio" %%% "zio-optics" % "0.2.1",
      "dev.zio" %%% "zio-schema-json" % "1.7.4", // it has to be the exact version that zio-http uses
      "dev.optics" %%% "monocle-core" % "3.2.0",
      "dev.optics" %%% "monocle-macro" % "3.2.0",
      "com.softwaremill.sttp.client4" %%% "zio" % sttpVersion,
      "com.softwaremill.sttp.client4" %%% "zio-json" % sttpVersion,
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
    "dev.zio" %% "zio-http" % "3.5.1",
    "com.lihaoyi" %% "os-lib" % "0.11.5",
    "dev.zio" %% "zio-mock" % "1.0.0-RC12",
    "com.softwaremill.sttp.client4" %% "armeria-backend-zio" % sttpVersion
  )
).dependsOn(common.jvm)

val uiGlueCommitHash: String = "846288b0b7840ce2f0741df9191f608e34ca85ca"

lazy val ui = project.in(file("ui")).settings(
  name := "ui",
  Compile / scalaJSUseMainModuleInitializer:= false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "2.8.1",
    "com.github.japgolly.scalajs-react" %%% "core" % "2.1.3",
    "com.lihaoyi" %%% "pprint"  % "0.9.0",
    "com.github.kurgansoft.uiglue" %%% "uiglue" % uiGlueCommitHash,
  ),
  resolvers += "jitpack" at "https://jitpack.io"
).dependsOn(common.js).enablePlugins(ScalaJSPlugin)