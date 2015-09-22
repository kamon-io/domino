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
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import kamon.docker.metrics.{ CpuMetrics, MemoryMetrics, NetworkMetrics }
import kamon.docker.stats.DockerStats._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object DockerMonitor extends App with KamonSupport {

  import settings.Settings._

  type ContainerStats = JValue

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val formats = DefaultFormats

  val logger = Logging(system, "Docker-Monitor")

  val network = Flow[ContainerStats].map(stats ⇒ (stats \ "network").extract[NetworkStats])
  val memory = Flow[ContainerStats].map(stats ⇒ (stats \ "memory_stats").extract[MemoryStats])
  val cpu = Flow[ContainerStats].map(stats ⇒ (stats \ "cpu_stats").extract[CpuStats])

  val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] = Http().outgoingConnection(dockerHost, dockerPort)

  def flowWriter(writeNetwork: Sink[NetworkStats, Future[Unit]], writeMemory: Sink[MemoryStats, Future[Unit]], writeCpu: Sink[CpuStats, Future[Unit]]) = {
    Sink[ContainerStats]() { implicit builder ⇒
      import FlowGraph.Implicits._

      val stats = builder.add(Broadcast[ContainerStats](3))

      stats ~> network ~> writeNetwork
      stats ~> memory ~> writeMemory
      stats ~> cpu ~> writeCpu
      stats.in
    }
  }

  def chunkConsumer(containerAlias: String) = Sink.foreach[HttpResponse] { response ⇒
    if (response.status == StatusCodes.OK) {
      logger.info(s"Getting stats from container: $containerAlias")

      val writeNetwork = Sink.foreach(NetworkMetrics(containerAlias))
      val writeMemory = Sink.foreach(MemoryMetrics(containerAlias))
      val writeCpu = Sink.foreach(CpuMetrics(containerAlias))

      response.entity.dataBytes.map(chunk ⇒ parse(chunk.utf8String)).to(flowWriter(writeNetwork, writeMemory, writeCpu)).run()
    } else {
      logger.error(s"Cannot connect to the Docker container: $containerAlias, with response status ${response.status}")
    }
  }

  def makeRequest(containerId: String, containerAlias: String): Future[Unit] = Future {
    Source.single(HttpRequest(uri = s"/containers/$containerId/stats"))
      .via(connectionFlow)
      .runWith(chunkConsumer(containerAlias))
  }

  containers.foreach {
    case (containerId, containerAlias) ⇒
      makeRequest(containerId, containerAlias)
  }

  System.in.read()
  system.shutdown()
  system.awaitTermination()
}

