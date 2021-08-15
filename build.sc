import mill._
import mill.define.Sources
import mill.scalajslib.ScalaJSModule
import mill.scalalib._
import mill.scalalib.publish._

object gbge extends Module {
  val ___scalaVersion = "2.13.6"
  val ___scalaJSVersion = "1.7.0"

  trait CommonPublishModule extends PublishModule {
    override def publishVersion = "0.0.1"
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
        ivy"com.lihaoyi::upickle::1.4.0",
        ivy"dev.zio::zio::1.0.3",
        ivy"com.github.julien-truffaut::monocle-core::2.1.0",
        ivy"com.github.julien-truffaut::monocle-macro::2.1.0"
      )
      override def millSourcePath = shared.millSourcePath
      override def sources: Sources = super.sources
      override def artifactName: T[String] = "gbgeShared"
    }

    object jvm extends ScalaModule with CommonPublishModule with Common {
      object test extends Tests {
        override def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.2.2")
        def testFrameworks = Seq("org.scalatest.tools.Framework")
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
      ivy"com.lihaoyi::cask:0.7.11",
      ivy"com.lihaoyi::requests:0.6.9",
      ivy"com.lihaoyi::os-lib:0.7.8"
    )

    object test extends Tests {
      override def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.2.2")
      def testFrameworks = Seq("org.scalatest.tools.Framework")
    }
  }

  object ui extends ScalaJSModule with CommonPublishModule {
    override def artifactName: T[String] = "gbgeUI"
    def scalaVersion = ___scalaVersion
    override def moduleDeps = Seq(shared.js)
    def scalaJSVersion = ___scalaJSVersion

    override def scalacOptions = Seq("-Xxml:-coalescing")

    override def ivyDeps = Agg(
      ivy"org.scala-js::scalajs-dom::1.1.0",
      ivy"com.github.japgolly.scalajs-react::core::1.7.7",
      ivy"com.lihaoyi::pprint::0.6.0"
    )

    object test extends Tests {
      override def ivyDeps = Agg(
        ivy"org.scalatest::scalatest:3.2.3",
        ivy"org.scalamock::scalamock:4.4.0"
      )
      def testFrameworks = Seq("org.scalatest.tools.Framework")
    }
  }
}
