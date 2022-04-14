import coursier.{MavenRepository, Repository}
import mill._
import mill.define.{Sources, Task}
import mill.scalajslib.ScalaJSModule
import mill.scalalib._
import mill.scalalib.publish._

object gbge extends Module {
  val ___scalaVersion = "2.13.8"
  val ___scalaJSVersion = "1.9.0"

  trait CommonPublishModule extends PublishModule {
    override def publishVersion = "0.1.0"
    override def pomSettings = PomSettings(
      "",
      "com.kurgansoft",
      "",
      List.empty,
      VersionControl.github("",""),
      Seq.empty
    )
  }

  object shared extends Module {
    trait Common extends ScalaModule {

      override def scalaVersion = ___scalaVersion
      override def ivyDeps = Agg(
        ivy"com.lihaoyi::upickle::1.5.0",
        ivy"dev.zio::zio::1.0.13",
        ivy"com.github.julien-truffaut::monocle-core::2.1.0",
        ivy"com.github.julien-truffaut::monocle-macro::2.1.0"
      )
      override def millSourcePath = shared.millSourcePath
      override def sources: Sources = super.sources
      override def artifactName: T[String] = "gbgeShared"
    }

    object jvm extends ScalaModule with CommonPublishModule with Common {
      object test extends Tests {
        override def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.2.9")
        override def testFramework = "org.scalatest.tools.Framework"
      }
    }

    object js extends ScalaJSModule with CommonPublishModule with Common {
      override def scalaJSVersion = ___scalaJSVersion
    }
  }

  object backend extends ScalaModule with CommonPublishModule {

    override def artifactName: T[String] = "gbgeBackend"

    override def scalaVersion = ___scalaVersion
    override def moduleDeps = Seq(shared.jvm)
    override def ivyDeps = Agg(
      ivy"com.lihaoyi::cask:0.8.0",
      ivy"com.lihaoyi::requests:0.7.0",
      ivy"com.lihaoyi::os-lib:0.8.1"
    )

    object test extends Tests {
      override def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.2.9")
      override def testFramework = "org.scalatest.tools.Framework"
    }
  }

  object ui extends ScalaJSModule with CommonPublishModule {
    override def artifactName: T[String] = "gbgeUI"
    def scalaVersion = ___scalaVersion
    override def moduleDeps = Seq(shared.js)
    def scalaJSVersion = ___scalaJSVersion

    override def scalacOptions = Seq("-Xxml:-coalescing")

    override def repositoriesTask: Task[Seq[Repository]] = T.task {
      super.repositoriesTask() ++ Seq(
        MavenRepository("https://jitpack.io")
      )
    }

    override def ivyDeps = Agg(
      ivy"org.scala-js::scalajs-dom::2.1.0",
      ivy"org.scala-js::scala-js-macrotask-executor::1.0.0",
      ivy"com.github.japgolly.scalajs-react::core::2.0.1",
      ivy"com.lihaoyi::pprint::0.7.1",
      ivy"com.github.kurgansoft:uiglue:eb56dbde6d"
    )

    object test extends Tests {
      override def ivyDeps = Agg(
        ivy"org.scalatest::scalatest:3.2.9",
        ivy"org.scalamock::scalamock:5.2.0"
      )
      override def testFramework = "org.scalatest.tools.Framework"
    }
  }
}
