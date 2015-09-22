/* =========================================================================================
 * Copyright © 2013-2015 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

import sbt._

object Dependencies {

  val resolutionRepos = Seq(
    "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/"
  )

  val StreamsVersion  = "1.0"
  val KamonVersion    = "0.5.1"

  val json4sJackson   = "org.json4s"          %% "json4s-jackson"               % "3.2.11"
  val akkaStream      = "com.typesafe.akka"   %% "akka-stream-experimental"     % StreamsVersion
  val akkaCore        = "com.typesafe.akka"   %% "akka-http-core-experimental"  % StreamsVersion
  val akkaHttp        = "com.typesafe.akka"   %% "akka-http-experimental"       % StreamsVersion
  val kamonCore       = "io.kamon"            %% "kamon-core"                   % KamonVersion
  val kamonStatsd     = "io.kamon"            %% "kamon-statsd"                 % KamonVersion
}