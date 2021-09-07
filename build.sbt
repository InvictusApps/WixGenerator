name := "wix-generator"
organization := "com.preparedapp"
version := "1.0.0-SNAPSHOT"

scalaVersion := "3.0.1"

fork := true
scalacOptions ++= Seq("-deprecation")

libraryDependencies ++= Seq(
  "com.outr" %% "scribe-slf4j" % "3.5.5"
)