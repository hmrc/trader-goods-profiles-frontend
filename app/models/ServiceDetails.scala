/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import play.api.{ConfigLoader, Configuration}

case class ServiceDetails(protocol: String, host: String, port: Int) {
  lazy val baseUrl: String = s"$protocol://$host:$port"
}

object ServiceDetails {

  implicit lazy val configLoader: ConfigLoader[ServiceDetails] = ConfigLoader { config => prefix =>
    val routerConfig = Configuration(config).get[Configuration](prefix)
    val protocol     = routerConfig.get[String]("protocol")
    val host         = routerConfig.get[String]("host")
    val port         = routerConfig.get[Int]("port")

    ServiceDetails(protocol, host, port)
  }
}
