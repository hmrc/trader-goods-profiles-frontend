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

import models.ott.CategorisationInfo
import pages.{CountryOfOriginPage, GoodsDescriptionPage, Page, QuestionPage, TraderReferencePage}
import play.api.mvc.JavascriptLiteral
import controllers.routes

sealed trait PageUpdate

case object CountryOfOriginPageUpdate extends PageUpdate
case object GoodsDescriptionPageUpdate extends PageUpdate
case object TraderReferencePageUpdate extends PageUpdate

object PageUpdate {

  def getPage(pageUpdate: PageUpdate, recordId: String): QuestionPage[String]   =
    pageUpdate match {
      case CountryOfOriginPageUpdate  => CountryOfOriginPage(recordId)
      case GoodsDescriptionPageUpdate => GoodsDescriptionPage(recordId)
      case TraderReferencePageUpdate  => TraderReferencePage(recordId)
    }
  def getPageUpdateLabel(pageUpdate: PageUpdate): String                        =
    pageUpdate match {
      case CountryOfOriginPageUpdate  => "countryOfOrigin.checkYourAnswersLabel"
      case GoodsDescriptionPageUpdate => "goodsDescription.checkYourAnswersLabel"
      case TraderReferencePageUpdate  => "traderReference.checkYourAnswersLabel"
    }
  def getPageUpdateHidden(pageUpdate: PageUpdate): String                       =
    pageUpdate match {
      case CountryOfOriginPageUpdate  => "countryOfOrigin.change.hidden"
      case GoodsDescriptionPageUpdate => "goodsDescription.change.hidden"
      case TraderReferencePageUpdate  => "traderReference.change.hidden"
    }
  def getPageUpdateChangeLink(pageUpdate: PageUpdate, recordId: String): String =
    pageUpdate match {
      case CountryOfOriginPageUpdate  => routes.CountryOfOriginController.onPageLoad(CheckMode, recordId).url
      case GoodsDescriptionPageUpdate => routes.GoodsDescriptionController.onPageLoad(CheckMode, recordId).url
      case TraderReferencePageUpdate  => routes.TraderReferenceController.onPageLoad(CheckMode, recordId).url
    }
  implicit val jsLiteral: JavascriptLiteral[PageUpdate]                         = {
    case CountryOfOriginPageUpdate  => "CountryOfOriginPageUpdate"
    case GoodsDescriptionPageUpdate => "GoodsDescriptionPageUpdate"
    case TraderReferencePageUpdate  => "TraderReferencePageUpdate"

  }
}
