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

package kamon.domino.settings

import java.util.Map.Entry
import com.typesafe.config.{ ConfigFactory, ConfigObject, ConfigValue }
import scala.collection.JavaConverters._

object Settings {
  private val config = ConfigFactory.load()

  val dockerHost = config.getString("docker.host")
  val dockerPort = config.getInt("docker.port")

  lazy val containers: Map[String, String] = {
    val list: Iterable[ConfigObject] = config.getObjectList("docker.containers").asScala
    (for {
      item: ConfigObject ← list
      entry: Entry[String, ConfigValue] ← item.entrySet().asScala
      containerId = entry.getKey
      containerAlias = entry.getValue.unwrapped().toString
    } yield (containerId, containerAlias)).toMap
  }
}
