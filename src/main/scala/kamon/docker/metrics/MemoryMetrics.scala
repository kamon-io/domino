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

import akka.event.{ LoggingAdapter, NoLogging }
import kamon.Kamon
import kamon.docker.stats.DockerStats.MemoryStats
import kamon.metric.instrument.{ Memory, InstrumentFactory }
import kamon.metric.{ EntityRecorderFactory, GenericEntityRecorder }

class MemoryMetrics(instrumentFactory: InstrumentFactory) extends GenericEntityRecorder(instrumentFactory) {

  val usage = histogram("usage", Memory.Bytes)
  val limit = histogram("limit", Memory.Bytes)
  val maxUsage = histogram("max-usage", Memory.Bytes)

  def update(memoryStats: MemoryStats): Unit = {
    usage.record(memoryStats.`usage`)
    maxUsage.record(memoryStats.`max_usage`)
    limit.record(memoryStats.`limit`)
  }
}

object MemoryMetrics extends EntityRecorderFactory[MemoryMetrics] {
  override def category = "docker-memory"
  override def createRecorder(instrumentFactory: InstrumentFactory): MemoryMetrics = new MemoryMetrics(instrumentFactory)

  def apply(containerId: String): (MemoryStats) ⇒ Unit = Kamon.metrics.entity(MemoryMetrics, containerId).update
}