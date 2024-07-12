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

package models.ott

import models.{Enumerable, WithName}
import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait Exemption {

  val id: String
  val code: String
  val description: String
  val exemptionType: ExemptionType
}

sealed trait ExemptionType

object ExemptionType extends Enumerable.Implicits {

  case object Certificate extends WithName("certificate") with ExemptionType
  case object AdditionalCode extends WithName("additionalCode") with ExemptionType
  case object OtherExemption extends WithName("exemption") with ExemptionType

  private val values = Seq(Certificate, AdditionalCode, OtherExemption)

  implicit val enumerable: Enumerable[ExemptionType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}

final case class Certificate(id: String, code: String, description: String) extends Exemption {

  override val exemptionType: ExemptionType = ExemptionType.Certificate
}

object Certificate {

  implicit lazy val reads: Reads[Certificate] =
    (__ \ "exemptionType")
      .read[ExemptionType]
      .flatMap[ExemptionType] { et =>
        if (et == ExemptionType.Certificate) {
          Reads(_ => JsSuccess(et))
        } else {
          Reads(_ => JsError("exemptionType mustEqual `certificate"))
        }
      }
      .andKeep(
        (
          (__ \ "id").read[String] and
            (__ \ "code").read[String] and
            (__ \ "description").read[String]
        )(Certificate(_, _, _))
      )

  implicit lazy val writes: OWrites[Certificate] =
    (
      (__ \ "exemptionType").write[ExemptionType] and
        (__ \ "id").write[String] and
        (__ \ "code").write[String] and
        (__ \ "description").write[String]
    )(x => (x.exemptionType, x.id, x.code, x.description))
}

final case class AdditionalCode(id: String, code: String, description: String) extends Exemption {

  override val exemptionType: ExemptionType = ExemptionType.AdditionalCode
}

object AdditionalCode {

  implicit lazy val reads: Reads[AdditionalCode] =
    (__ \ "exemptionType")
      .read[ExemptionType]
      .flatMap[ExemptionType] { et =>
        if (et == ExemptionType.AdditionalCode) {
          Reads(_ => JsSuccess(et))
        } else {
          Reads(_ => JsError("exemptionType mustEqual `additionalCode"))
        }
      }
      .andKeep(
        (
          (__ \ "id").read[String] and
            (__ \ "code").read[String] and
            (__ \ "description").read[String]
        )(AdditionalCode(_, _, _))
      )

  implicit lazy val writes: OWrites[AdditionalCode] =
    (
      (__ \ "exemptionType").write[ExemptionType] and
        (__ \ "id").write[String] and
        (__ \ "code").write[String] and
        (__ \ "description").write[String]
    )(x => (x.exemptionType, x.id, x.code, x.description))
}

final case class OtherExemption(id: String, code: String, description: String) extends Exemption {

  override val exemptionType: ExemptionType = ExemptionType.OtherExemption
}

object OtherExemption {

  implicit lazy val reads: Reads[OtherExemption] =
    (__ \ "exemptionType")
      .read[ExemptionType]
      .flatMap[ExemptionType] { et =>
        if (et == ExemptionType.OtherExemption) {
          Reads(_ => JsSuccess(et))
        } else {
          Reads(_ => JsError("exemptionType mustEqual `exemption"))
        }
      }
      .andKeep(
        (
          (__ \ "id").read[String] and
            (__ \ "code").read[String] and
            (__ \ "description").read[String]
        )(OtherExemption(_, _, _))
      )

  implicit lazy val writes: OWrites[OtherExemption] =
    (
      (__ \ "exemptionType").write[ExemptionType] and
        (__ \ "id").write[String] and
        (__ \ "code").write[String] and
        (__ \ "description").write[String]
    )(x => (x.exemptionType, x.id, x.code, x.description))
}

object Exemption {

  implicit lazy val reads: Reads[Exemption] =
    Certificate.reads.widen or (AdditionalCode.reads.widen or OtherExemption.reads.widen)

  implicit lazy val writes: OWrites[Exemption] = OWrites {
    case c: Certificate    => Json.toJsObject(c)(Certificate.writes)
    case a: AdditionalCode => Json.toJsObject(a)(AdditionalCode.writes)
    case e: OtherExemption => Json.toJsObject(e)(OtherExemption.writes)
  }
}
