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

import play.api.libs.json._
import play.api.mvc.Call

sealed trait ReviewReason {
  val messageKey: String
  val linkKey: Option[String]

  def url(recordId: String): Option[Call]
}

object ReviewReason {

  val values: Seq[ReviewReason] = Seq(Commodity, Inadequate, Unclear, Measure, Mismatch)

  case object Commodity extends ReviewReason { // TODO: Update messageKey and values as part of TGP-2945
    val messageKey: String      = "singleRecord.commodityReviewReason"
    val linkKey: Option[String] = None

    override def url(recordId: String): Option[Call] = Some(
      controllers.problem.routes.JourneyRecoveryController.onPageLoad()
    )
  }

  case object Inadequate extends ReviewReason {
    val messageKey: String      = "singleRecord.inadequateReviewReason"
    val linkKey: Option[String] = Some("singleRecord.inadequateReviewReason.linkText")

    override def url(recordId: String): Option[Call] =
      Some(controllers.goodsRecord.routes.HasGoodsDescriptionChangeController.onPageLoad(NormalMode, recordId))
  }

  case object Unclear extends ReviewReason {
    val messageKey: String      = "singleRecord.unclearReviewReason"
    val linkKey: Option[String] = Some("singleRecord.unclearReviewReason.linkText")

    override def url(recordId: String): Option[Call] =
      Some(controllers.goodsRecord.routes.HasGoodsDescriptionChangeController.onPageLoad(NormalMode, recordId))

  }

  case object Measure extends ReviewReason {
    val messageKey: String      = "singleRecord.measureReviewReason"
    val linkKey: Option[String] = Some("singleRecord.measureReviewReason.linkText")

    override def url(recordId: String): Option[Call] =
      Some(
        controllers.categorisation.routes.CategorisationPreparationController.startCategorisation(recordId)
      ) // TODO: Is this URL right?
  }

  case object Mismatch extends ReviewReason {
    val messageKey: String      = "singleRecord.mismatchReviewReason"
    val linkKey: Option[String] = None

    override def url(recordId: String): Option[Call] = None
  }

  implicit val writes: Writes[ReviewReason] = Writes[ReviewReason] {
    case Commodity  => JsString("commodity")
    case Inadequate => JsString("inadequate")
    case Unclear    => JsString("unclear")
    case Measure    => JsString("measure")
    case Mismatch   => JsString("mismatch")
  }

  implicit val reads: Reads[ReviewReason] = Reads[ReviewReason] {
    case JsString("commodity")  => JsSuccess(Commodity)
    case JsString("inadequate") => JsSuccess(Inadequate)
    case JsString("unclear")    => JsSuccess(Unclear)
    case JsString("measure")    => JsSuccess(Measure)
    case JsString("mismatch")   => JsSuccess(Mismatch)
    case other                  => JsError(s"[ReviewReason] Reads unknown ReviewReason: $other")
  }
}