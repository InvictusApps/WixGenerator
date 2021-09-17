name := "wix-generator"
organization := "com.outr"
version := "1.0.0"

scalaVersion := "2.13.6"

publishTo := sonatypePublishToBundle.value
sonatypeProfileName := "com.outr"
publishMavenStyle := true
licenses := Seq("MIT" -> url("https://github.com/outr/youi/blob/master/LICENSE"))
sonatypeProjectHosting := Some(xerial.sbt.Sonatype.GitHubHosting("InvictusApps", "WixGenerator", "matt@outr.com"))
homepage := Some(url("https://github.com/InvictusApps/WixGenerator"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/InvictusApps/WixGenerator"),
    "scm:git@github.com:InvictusApps/WixGenerator.git"
  )
)
developers := List(
  Developer(id="darkfrog", name="Matt Hicks", email="matt@matthicks.com", url=url("http://matthicks.com"))
)

fork := true
scalacOptions ++= Seq("-deprecation")

libraryDependencies ++= Seq(
  "com.outr" %% "scribe-slf4j" % "3.5.5"
)