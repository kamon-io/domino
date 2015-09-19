name := "kamon-docker"

scalaVersion := "2.11.6"

version := "1.0"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.11"

libraryDependencies += "com.typesafe.akka" %% "akka-stream-experimental" % "1.0"

libraryDependencies += "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0"

libraryDependencies += "com.typesafe.akka" %% "akka-http-experimental" % "1.0"

libraryDependencies += "io.kamon" %% "kamon-core" % "0.5.1"

libraryDependencies += "io.kamon" %% "kamon-statsd" % "0.5.1"

enablePlugins(JavaAppPackaging)

mainClass in Compile := Some("kamon.docker.KamonDocker")