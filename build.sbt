name := """sqlkit-mapper"""
organization := "sqlkit"

ThisBuild / version := "0.1.0-SNAPSHOT"

//ThisBuild / scalaVersion := "2.13.14"


val Scala213 = "2.13.14"
val Scala212 = "2.12.20"

lazy val sqlkitmapper = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sqlkit-mapper",
    //crossScalaVersions := Seq(Scala212),
    sbtPlugin := true,
    libraryDependencies ++= Seq(
      "com.zaxxer" % "HikariCP" % "5.1.0",

      "mysql" % "mysql-connector-java" % "5.1.44" % "test",
      "org.scalatest" %% "scalatest" % "3.2.11" % "test",
      "org.slf4j" % "slf4j-nop" % "1.7.31" % Test
    )
  )
