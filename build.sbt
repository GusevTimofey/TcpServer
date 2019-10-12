name := "TcpFibers"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies += "org.typelevel" %% "cats-effect" % "2.0.0" withSources() withJavadoc()

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds",
  "-Ypartial-unification"
)
