/*
 * =========================================================================================
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

package kamon.docker

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import kamon.docker.metrics.{CpuMetrics, MemoryMetrics, NetworkMetrics}
import kamon.docker.stats.DockerStats._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.concurrent.Future

object KamonDocker extends App with KamonSupport {

  import Configuration._

  type ContainerStats = JValue

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val formats = DefaultFormats

  val logger = Logging(system, "Kamon-Docker")

  val network = Flow[ContainerStats].map(stats => (stats \ "network").extract[NetworkStats])
  val memory = Flow[ContainerStats].map(stats => (stats \ "memory_stats").extract[MemoryStats])
  val cpu = Flow[ContainerStats].map(stats => (stats \ "cpu_stats").extract[CpuStats])

  val writeNetwork = Sink.foreach(NetworkMetrics())
  val writeMemory = Sink.foreach(MemoryMetrics())
  val writeCpu = Sink.foreach(CpuMetrics())

  val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] = Http().outgoingConnection(dockerHost, dockerPort)

  val metricsWriter = Sink[ContainerStats]() { implicit builder =>
    import FlowGraph.Implicits._

    val stats = builder.add(Broadcast[ContainerStats](3))

    stats ~> network ~> writeNetwork
    stats ~> memory ~> writeMemory
    stats ~> cpu ~> writeCpu
    stats.in
  }

  val chunkConsumer = Sink.foreach[HttpResponse] { response =>
    if (response.status == StatusCodes.OK) {
      logger.info(s"Getting stats from container-id: $containerId")
      response.entity.dataBytes.map(chunk => parse(chunk.utf8String)).to(metricsWriter).run()
    } else {
      logger.error(s"Cannot connect to the Docker container: $containerId, with response status ${response.status}")
    }
  }

  val stats =
    Source.single(HttpRequest(uri = s"/containers/$containerId/stats"))
      .via(connectionFlow)
      .runWith(chunkConsumer)

  System.in.read()
  system.shutdown()
  system.awaitTermination()
}


