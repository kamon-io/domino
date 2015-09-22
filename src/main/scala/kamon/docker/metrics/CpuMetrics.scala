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
import kamon.docker.stats.DockerStats.CpuStats
import kamon.metric.instrument.InstrumentFactory
import kamon.metric.{ EntityRecorderFactory, GenericEntityRecorder }

class CpuMetrics(instrumentFactory: InstrumentFactory) extends GenericEntityRecorder(instrumentFactory) {
  import CpuMetrics._

  val systemCpuUsage = DiffRecordingHistogram(histogram("system-cpu-usage"))
  val totalUsage = DiffRecordingHistogram(histogram("total-usage"))
  val usageInKernelMode = DiffRecordingHistogram(histogram("usage-in-kernel-mode"))
  val usageInUserMode = DiffRecordingHistogram(histogram("usage-in-user-mode"))
  val percentUsage = histogram("percent-cpu-usage")

  def update(cpuStats: CpuStats): Unit = {
    val cpu = cpuStats.`cpu_usage`
    val systemCpu = cpuStats.`system_cpu_usage`
    val totalCpu = cpu.`total_usage`

    systemCpuUsage.record(systemCpu)
    totalUsage.record(totalCpu)
    percentUsage.record(updatePercent(totalCpu, systemCpu, cpu.`percpu_usage`.size))
    usageInKernelMode.record(cpu.`usage_in_kernelmode`)
    usageInUserMode.record(cpu.`usage_in_usermode`)
  }
}

object CpuMetrics extends EntityRecorderFactory[CpuMetrics] {
  private var lastObservedCpuUsage = 0L
  private var lastObserverSystemCpuUsage = 0L

  override def category = "docker-cpu"
  override def createRecorder(instrumentFactory: InstrumentFactory): CpuMetrics = new CpuMetrics(instrumentFactory)

  def apply(containerId: String): (CpuStats) ⇒ Unit = Kamon.metrics.entity(CpuMetrics, containerId).update

  def updatePercent(totalCpuUsage: Long, systemCpuUsage: Long, processors: Int) = {
    val percent = {
      if (lastObservedCpuUsage > 0L && lastObserverSystemCpuUsage > 0L) {
        calculateCpuPercent(totalCpuUsage, systemCpuUsage, processors)
      } else 0L
    }

    lastObservedCpuUsage = totalCpuUsage
    lastObserverSystemCpuUsage = systemCpuUsage
    percent
  }

  private def calculateCpuPercent(totalContainerCpu: Long, totalSystemCpu: Long, numProcessors: Int): Long = {
    // The CPU values returned by the docker api are cumulative for the life of the process, which is not what we want.
    val cpuDelta = totalContainerCpu - lastObservedCpuUsage
    val systemCpuDelta = totalSystemCpu - lastObserverSystemCpuUsage
    // based on how the "docker stats" command calculates the cpu percent
    // https://github.com/docker/docker/blob/eb79acd7a0db494d9c6d1b1e970bdabf7c44ae4e/api/client/commands.go#L2758
    if (cpuDelta > 0L && systemCpuDelta > 0L) {
      BigDecimal(cpuDelta.toDouble / systemCpuDelta.toDouble * numProcessors * 100.0).setScale(0, BigDecimal.RoundingMode.HALF_UP).toLong
    } else 0L
  }
}