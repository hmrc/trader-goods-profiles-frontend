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

package testModels

import play.api.libs.json.{OFormat, OWrites, Reads, __}
import utils.Constants.{niphlNumberKey, nirmsNumberKey, ukimsNumberKey}

case class DataStoreProfile(
  eori: String,
  actorId: String,
  ukimsNumber: String,
  nirmsNumber: Option[String],
  niphlNumber: Option[String]
)

object DataStoreProfile {

  val reads: Reads[DataStoreProfile] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "eori").read[String] and
        (__ \ "actorId").read[String] and
        (__ \ ukimsNumberKey).read[String] and
        (__ \ nirmsNumberKey).readNullable[String] and
        (__ \ niphlNumberKey).readNullable[String]
    )(DataStoreProfile.apply _)
  }

  val writes: OWrites[DataStoreProfile] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "eori").write[String] and
        (__ \ "actorId").write[String] and
        (__ \ ukimsNumberKey).write[String] and
        (__ \ nirmsNumberKey).writeNullable[String] and
        (__ \ niphlNumberKey).writeNullable[String]
    )(o => Tuple.fromProductTyped(o))
  }

  implicit val format: OFormat[DataStoreProfile] = OFormat(reads, writes)
}
