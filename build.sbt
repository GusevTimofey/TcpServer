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

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-io" % "2.0.0",
  "io.chrisdavenport" %% "log4cats-slf4j" % "0.4.0-M2",
  "org.slf4j" % "slf4j-simple" % "1.7.26",
  "com.comcast" %% "ip4s-cats" % "1.2.1",
  "org.scodec" %% "scodec-stream" % "2.0.0",
  "org.jline" % "jline" % "3.12.1",
  "com.monovore" %% "decline" % "0.7.0-M0"
)
