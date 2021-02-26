import Dependencies._

ThisBuild / scalaVersion     := "2.13.4"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

val circeVersion = "0.12.3"

lazy val `with-aggregate` =
  (project in file("with-aggregate"))
    .settings(
      libraryDependencies ++= Seq(
        scalaTest % Test,
        "org.scalikejdbc" %% "scalikejdbc"       % "3.5.0",
        "com.h2database"  %  "h2"                % "1.4.200",
        "ch.qos.logback"  %  "logback-classic"   % "1.2.3"
      )
    )
    .settings(
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-core",
        "io.circe" %% "circe-generic",
        "io.circe" %% "circe-parser"
      ).map(_ % circeVersion)
    )


lazy val `without-aggregate` =
  (project in file("without-aggregate"))
    .settings(
      libraryDependencies ++= Seq(
        scalaTest % Test,
        "org.scalikejdbc" %% "scalikejdbc"       % "3.5.0",
        "com.h2database"  %  "h2"                % "1.4.200",
        "ch.qos.logback"  %  "logback-classic"   % "1.2.3"
      )
    )


lazy val root = (project in file("."))
  .aggregate(`with-aggregate`, `without-aggregate`)


// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
