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
import jakarta.inject.Singleton
import models.{CheckMode, NormalMode, UserAnswers}
import pages.Page
import pages.goodsRecord._
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import javax.inject.Inject

@Singleton
class GoodsRecordNavigator @Inject() extends Navigator {

  override val normalRoutes: Page => UserAnswers => Call = {
    case CreateRecordStartPage            =>
      _ => controllers.goodsRecord.routes.ProductReferenceController.onPageLoadCreate(NormalMode)
    case ProductReferencePage             =>
      _ => controllers.goodsRecord.routes.GoodsDescriptionController.onPageLoadCreate(NormalMode)
    case p: ProductReferenceUpdatePage    =>
      _ => controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadproductReference(p.recordId)
    case GoodsDescriptionPage             =>
      _ => controllers.goodsRecord.countryOfOrigin.routes.CreateCountryOfOriginController.onPageLoad(NormalMode)
    case p: GoodsDescriptionUpdatePage    =>
      _ => controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadGoodsDescription(p.recordId)
    case CountryOfOriginPage              =>
      _ => controllers.goodsRecord.commodityCode.routes.CreateCommodityCodeController.onPageLoad(NormalMode)
    case p: CountryOfOriginUpdatePage     =>
      _ => controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadCountryOfOrigin(p.recordId)
    case CommodityCodePage                => _ => routes.HasCorrectGoodsController.onPageLoadCreate(NormalMode)
    case p: CommodityCodeUpdatePage       =>
      _ => routes.HasCorrectGoodsController.onPageLoadUpdate(NormalMode, p.recordId)
    case p: HasGoodsDescriptionChangePage => answers => navigateFromHasGoodsDescriptionChangePage(answers, p.recordId)
    case p: HasCountryOfOriginChangePage  => answers => navigateFromHasCountryOfOriginChangePage(answers, p.recordId)
    case p: HasCommodityCodeChangePage    => answers => navigateFromHasCommodityCodeChangePage(answers, p.recordId)
    case p: CyaCreateRecordPage           =>
      _ => controllers.goodsRecord.routes.CreateRecordSuccessController.onPageLoad(p.recordId)
    case p: CyaUpdateRecordPage           => _ => controllers.goodsRecord.routes.SingleRecordController.onPageLoad(p.recordId)
    case _                                => _ => routes.IndexController.onPageLoad()
  }

  override val checkRoutes: Page => UserAnswers => Call = {
    case ProductReferencePage          => _ => controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad()
    case p: ProductReferenceUpdatePage =>
      _ => controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadproductReference(p.recordId)
    case GoodsDescriptionPage          => _ => controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad()
    case p: GoodsDescriptionUpdatePage =>
      _ => controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadGoodsDescription(p.recordId)
    case CountryOfOriginPage           => _ => controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad()
    case p: CountryOfOriginUpdatePage  =>
      _ => controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadCountryOfOrigin(p.recordId)
    case CommodityCodePage             => _ => routes.HasCorrectGoodsController.onPageLoadCreate(CheckMode)
    case p: CommodityCodeUpdatePage    =>
      _ => routes.HasCorrectGoodsController.onPageLoadUpdate(CheckMode, p.recordId)
    case _                             => _ => controllers.problem.routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromHasCommodityCodeChangePage(answers: UserAnswers, recordId: String): Call = {
    val continueUrl = RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(HasCommodityCodeChangePage(recordId))
      .map {
        case false => controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
        case true  =>
          controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController.onPageLoad(NormalMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasCountryOfOriginChangePage(answers: UserAnswers, recordId: String): Call = {
    val continueUrl = RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(HasCountryOfOriginChangePage(recordId))
      .map {
        case false => controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
        case true  =>
          controllers.goodsRecord.countryOfOrigin.routes.UpdateCountryOfOriginController
            .onPageLoad(NormalMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

  private def navigateFromHasGoodsDescriptionChangePage(answers: UserAnswers, recordId: String): Call = {
    val continueUrl = RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId).url)
    answers
      .get(HasGoodsDescriptionChangePage(recordId))
      .map {
        case false => controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
        case true  => controllers.goodsRecord.routes.GoodsDescriptionController.onPageLoadUpdate(NormalMode, recordId)
      }
      .getOrElse(controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

}
