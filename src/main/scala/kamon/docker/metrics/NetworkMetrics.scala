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

package kamon.docker.metrics

import akka.event.{ NoLogging, LoggingAdapter }
import kamon.Kamon
import kamon.docker.stats.DockerStats.NetworkStats
import kamon.metric.{ EntityRecorderFactory, GenericEntityRecorder }
import kamon.metric.instrument._

class NetworkMetrics(instrumentFactory: InstrumentFactory) extends GenericEntityRecorder(instrumentFactory) {

  val receivedBytes = histogram("rx-bytes", Memory.Bytes)
  val transmittedBytes = histogram("tx-bytes", Memory.Bytes)
  val receiveErrors = histogram("rx-errors")
  val receivePackets = histogram("tx-packets")
  val transmitErrors = histogram("tx-errors")
  val receiveDrops = histogram("rx-dropped")
  val transmitDrops = histogram("tx-dropped")
  val transmitPackets = histogram("tx-packets")

  def update(networkStats: NetworkStats): Unit = {
    receivedBytes.record(networkStats.`rx_bytes`)
    transmittedBytes.record(networkStats.`tx_bytes`)
    receiveErrors.record(networkStats.`rx_errors`)
    receivePackets.record(networkStats.`rx_packets`)
    transmitErrors.record(networkStats.`tx_errors`)
    receiveDrops.record(networkStats.`rx_dropped`)
    transmitDrops.record(networkStats.`tx_dropped`)
    transmitPackets.record(networkStats.`tx_packets`)
  }
}

object NetworkMetrics extends EntityRecorderFactory[NetworkMetrics] {
  override def category = "docker-network"
  override def createRecorder(instrumentFactory: InstrumentFactory): NetworkMetrics = new NetworkMetrics(instrumentFactory)

  def apply(containerId: String): (NetworkStats) ⇒ Unit = Kamon.metrics.entity(NetworkMetrics, containerId).update
}