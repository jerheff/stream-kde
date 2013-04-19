name := "kde"

version := "0.1"

scalaVersion := "2.9.2"

// scalaVersion := "2.10.0"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Spray Repository" at "http://repo.spray.cc/"

libraryDependencies += "org.spark-project" % "spark-streaming_2.9.2" % "0.7.0"

libraryDependencies += "xerces" % "xercesImpl" % "2.9.1"

libraryDependencies += "xalan" % "xalan" % "2.7.1"

// libraryDependencies += "com.azavea.geotrellis" % "geotrellis_2.10" % "0.8.0"

// doesn't work: libraryDependencies += "com.azavea.geotrellis" % "geotrellis_2.9.2" % "0.7.0"

libraryDependencies += "com.azavea.geotrellis" %% "geotrellis" % "0.9.0-SNAPSHOT"
