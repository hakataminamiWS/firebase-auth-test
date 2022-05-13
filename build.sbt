name := """firebase-auth-test"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.8"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

// https://mvnrepository.com/artifact/org.pac4j/play-pac4j
libraryDependencies += "org.pac4j" %% "play-pac4j" % "11.1.0-PLAY2.8"
// https://mvnrepository.com/artifact/org.apache.shiro/shiro-core
libraryDependencies += "org.apache.shiro" % "shiro-core" % "1.9.0"

// https://mvnrepository.com/artifact/org.pac4j/pac4j-oidc
libraryDependencies += "org.pac4j" % "pac4j-oidc" % "5.4.3"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.4"

// https://mvnrepository.com/artifact/com.google.firebase/firebase-admin
libraryDependencies += "com.google.firebase" % "firebase-admin" % "8.1.0"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
