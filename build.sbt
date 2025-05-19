ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.13"

scalacOptions ++= Seq("-deprecation", "-feature")

lazy val root = (project in file("."))
  .settings(
    name := "RetailDiscountRuleEngine"
  )

// Required for PostgreSQL JDBC
libraryDependencies += "org.postgresql" % "postgresql" % "42.7.3"


