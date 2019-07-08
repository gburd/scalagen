organization in ThisBuild := "com.mysema.scalagen"

version in ThisBuild := "0.3.2"

name := "scalagen-root"

lazy val sclVersions = List("2.11.8")

scalaVersion in ThisBuild := sclVersions.head

lazy val scalagen = project

logBuffered in Test := false
logBuffered := false
scalacOptions ++= Seq("-deprecation", "-feature", "-language:existentials", "-target:jvm-1.8")
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
