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

package navigation

import controllers.routes
import models._
import pages._
import pages.download.RequestDataPage
import pages.goodsRecord.{CommodityCodePage, CommodityCodeUpdatePage}
import play.api.mvc.Call
import queries.{CategorisationDetailsQuery, LongerCommodityQuery}
import utils.Constants._

import javax.inject.{Inject, Singleton}

@Singleton
class Navigation @Inject() () extends Navigator {

  val normalRoutes: Page => UserAnswers => Call = {
    case p: LongerCommodityCodePage                =>
      _ => routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(NormalMode, p.recordId)
    case HasCorrectGoodsPage                       => answers => navigateFromHasCorrectGoods(answers)
    case p: HasCorrectGoodsCommodityCodeUpdatePage => answers => navigateFromHasCorrectGoodsUpdate(answers, p.recordId)
    case p: HasCorrectGoodsLongerCommodityCodePage =>
      answers => navigateFromHasCorrectGoodsLongerCommodityCode(p.recordId, answers, NormalMode)
    case p: ReviewReasonPage                       => _ => controllers.goodsRecord.routes.SingleRecordController.onPageLoad(p.recordId)
    case RequestDataPage                           => _ => controllers.download.routes.DownloadRequestSuccessController.onPageLoad()
    case _                                         => _ => routes.IndexController.onPageLoad()
  }

  val checkRoutes: Page => UserAnswers => Call = {
    case p: LongerCommodityCodePage                =>
      _ => routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(CheckMode, p.recordId)
    case HasCorrectGoodsPage                       => answers => navigateFromHasCorrectGoodsCheck(answers)
    case p: HasCorrectGoodsCommodityCodeUpdatePage =>
      answers => navigateFromHasCorrectGoodsUpdateCheck(answers, p.recordId)
    case p: HasCorrectGoodsLongerCommodityCodePage =>
      answers => navigateFromHasCorrectGoodsLongerCommodityCode(p.recordId, answers, CheckMode)
    case _                                         => _ => controllers.problem.routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromHasCorrectGoods(answers: UserAnswers): Call =
    answers
      .get(HasCorrectGoodsPage)
      .map {
        case true  => controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad()
        case false => controllers.goodsRecord.routes.CommodityCodeController.onPageLoadCreate(NormalMode)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoodsCheck(answers: UserAnswers): Call =
    answers
      .get(HasCorrectGoodsPage)
      .map {
        case true  =>
          if (answers.isDefined(CommodityCodePage)) {
            controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad()
          } else {
            controllers.goodsRecord.routes.CommodityCodeController.onPageLoadCreate(CheckMode)
          }
        case false => controllers.goodsRecord.routes.CommodityCodeController.onPageLoadCreate(CheckMode)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoodsUpdate(answers: UserAnswers, recordId: String): Call =
    answers
      .get(HasCorrectGoodsCommodityCodeUpdatePage(recordId))
      .map {
        case true  => controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadCommodityCode(recordId)
        case false => controllers.goodsRecord.routes.CommodityCodeController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoodsUpdateCheck(answers: UserAnswers, recordId: String): Call =
    answers
      .get(HasCorrectGoodsCommodityCodeUpdatePage(recordId))
      .map {
        case true  =>
          if (answers.isDefined(CommodityCodeUpdatePage(recordId))) {
            controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadCommodityCode(recordId)
          } else {
            controllers.goodsRecord.routes.CommodityCodeController.onPageLoadUpdate(CheckMode, recordId)
          }
        case false => controllers.goodsRecord.routes.CommodityCodeController.onPageLoadUpdate(CheckMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def navigateFromHasCorrectGoodsLongerCommodityCode(
    recordId: String,
    answers: UserAnswers,
    mode: Mode
  ): Call = {
    for {
      categorisationInfo <- answers.get(CategorisationDetailsQuery(recordId))
      commodity          <- answers.get(LongerCommodityQuery(recordId))
    } yield
      if (categorisationInfo.commodityCode == getShortenedCommodityCode(commodity)) {
        controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(mode, recordId)
      } else {
        answers.get(HasCorrectGoodsLongerCommodityCodePage(recordId)) match {
          case Some(true)  =>
            controllers.categorisation.routes.CategorisationPreparationController
              .startLongerCategorisation(mode, recordId)
          case Some(false) => controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(mode, recordId)
          case None        => controllers.problem.routes.JourneyRecoveryController.onPageLoad()
        }
      }
  }.getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad())

  private def getShortenedCommodityCode(commodity: Commodity) =
    commodity.commodityCode.reverse
      .dropWhile(char => char == '0')
      .reverse
      .padTo(minimumLengthOfCommodityCode, '0')
      .mkString

}
