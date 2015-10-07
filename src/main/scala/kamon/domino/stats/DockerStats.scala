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

package kamon.domino.stats

object DockerStats {
  case class NetworkStats(`rx_bytes`: Long,
    `rx_packets`: Long,
    `rx_dropped`: Long,
    `rx_errors`: Long,
    `tx_bytes`: Long,
    `tx_packets`: Long,
    `tx_dropped`: Long,
    `tx_errors`: Long)

  case class MemoryStats(`max_usage`: Long,
    `usage`: Long,
    `failcnt`: Long,
    `limit`: Long)

  case class CpuStats(`cpu_usage`: CpuUsage,
    `system_cpu_usage`: Long)

  case class CpuUsage(`total_usage`: Long,
    `percpu_usage`: Seq[Long],
    `usage_in_kernelmode`: Long,
    `usage_in_usermode`: Long)
}
