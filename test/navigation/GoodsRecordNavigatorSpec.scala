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

import base.SpecBase
import base.TestConstants.testRecordId
import controllers.routes
import models.{CheckMode, NormalMode}
import org.scalatest.BeforeAndAfterEach
import pages.Page
import pages.goodsRecord._
import play.api.http.Status.SEE_OTHER
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

class GoodsRecordNavigatorSpec extends SpecBase with BeforeAndAfterEach {
  private val navigator = new GoodsRecordNavigator()

  "GoodsRecordNavigator" - {

    "in Normal mode" - {
      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, emptyUserAnswers) mustBe routes.IndexController.onPageLoad()
      }

      "in Create Record Journey" - {

        "must go from CreateRecordStartPage to ProductReferencePage" in {

          navigator.nextPage(
            CreateRecordStartPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.ProductReferenceController
            .onPageLoadCreate(NormalMode)
        }

        "must go from ProductReferencePage to GoodsDescriptionPage" in {

          navigator.nextPage(
            ProductReferencePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.GoodsDescriptionController
            .onPageLoadCreate(NormalMode)
        }

        "must go from GoodsDescriptionPage to CountryOfOriginPage" in {
          navigator.nextPage(
            GoodsDescriptionPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.countryOfOrigin.routes.CreateCountryOfOriginController.onPageLoad(NormalMode)
        }

        "must go from CountryOfOriginPage to CommodityCodePage" in {
          navigator.nextPage(
            CountryOfOriginPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.commodityCode.routes.CreateCommodityCodeController.onPageLoad(NormalMode)
        }

        "must go from CommodityCodePage to HasCorrectGoodsPage" in {

          navigator.nextPage(
            CommodityCodePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.hasCorrectGoods.routes.CreateController.onPageLoad(NormalMode)
        }

        "must go from CyaCreateRecord to CreateRecordSuccess" in {

          navigator.nextPage(
            CyaCreateRecordPage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.CreateRecordSuccessController
            .onPageLoad(testRecordId)
        }

      }

      "in Update Record Journey" - {

        "if not answered" - {
          val continueUrl =
            RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url)

          "must go from HasCountryOfOriginChangePage to JourneyRecoveryController" in {
            navigator.nextPage(
              HasCountryOfOriginChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl))
          }

          "must go from HasGoodsDescriptionChangePage to JourneyRecoveryController" in {
            navigator.nextPage(
              HasGoodsDescriptionChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl))
          }

          "must go from HasCommodityCodeChangePage to JourneyRecoveryController" in {
            navigator.nextPage(
              HasCommodityCodeChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe controllers.problem.routes.JourneyRecoveryController.onPageLoad(Some(continueUrl))
          }
        }

        "if answer is Yes" - {

          "must go from HasCountryOfOriginChangePage to CountryOfOriginUpdatePage" in {
            navigator.nextPage(
              HasCountryOfOriginChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers.set(HasCountryOfOriginChangePage(testRecordId), true).success.value
            ) mustBe controllers.goodsRecord.countryOfOrigin.routes.UpdateCountryOfOriginController
              .onPageLoad(NormalMode, testRecordId)
          }

          "must go from HasGoodsDescriptionChangePage to GoodsDescriptionUpdatePage" in {
            navigator.nextPage(
              HasGoodsDescriptionChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers.set(HasGoodsDescriptionChangePage(testRecordId), true).success.value
            ) mustBe controllers.goodsRecord.routes.GoodsDescriptionController
              .onPageLoadUpdate(NormalMode, testRecordId)
          }

          "must go from HasCommodityCodeChangePage to CommodityCodeUpdatePage" in {
            navigator.nextPage(
              HasCommodityCodeChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers.set(HasCommodityCodeChangePage(testRecordId), true).success.value
            ) mustBe controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController
              .onPageLoad(NormalMode, testRecordId)
          }
        }

        "if answer is No" - {

          "must go from HasCountryOfOriginChangePage to SingleRecordController" in {
            navigator.nextPage(
              HasCountryOfOriginChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers.set(HasCountryOfOriginChangePage(testRecordId), false).success.value
            ) mustBe controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId)
          }

          "must go from HasGoodsDescriptionChangePage to SingleRecordController" in {
            navigator.nextPage(
              HasGoodsDescriptionChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers.set(HasGoodsDescriptionChangePage(testRecordId), false).success.value
            ) mustBe controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId)
          }

          "must go from HasCommodityCodeChangePage to SingleRecordController" in {
            navigator.nextPage(
              HasCommodityCodeChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers.set(HasCommodityCodeChangePage(testRecordId), false).success.value
            ) mustBe controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId)
          }
        }

        "must go from CountryOfOriginUpdatePage to CyaUpdateRecord" in {
          navigator.nextPage(
            CountryOfOriginUpdatePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadCountryOfOrigin(testRecordId)
        }

        "must go from ProductReferenceUpdatePage to CyaUpdateRecord" in {
          navigator.nextPage(
            ProductReferenceUpdatePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadproductReference(testRecordId)
        }

        "must go from GoodsDescriptionUpdatePage to CyaUpdateRecord" in {
          navigator.nextPage(
            GoodsDescriptionUpdatePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadGoodsDescription(testRecordId)
        }

        "must go from CommodityCodeUpdatePage to HasCorrectGoodsCommodityCodeUpdatePage" in {
          navigator.nextPage(
            CommodityCodeUpdatePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.hasCorrectGoods.routes.UpdateController.onPageLoad(NormalMode, testRecordId)
        }

        "must go from CyaUpdateRecord to SingleRecordController" in {

          navigator.nextPage(
            CyaUpdateRecordPage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.SingleRecordController
            .onPageLoad(testRecordId)
        }

      }
    }

    "in Check mode" - {
      "in Create Record Journey" - {

        "must go from ProductReferencePage to CyaCreateRecord" in {

          navigator.nextPage(
            ProductReferencePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad()
        }

        "must go from GoodsDescriptionPage to CyaCreateRecord" in {
          navigator.nextPage(
            GoodsDescriptionPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad()
        }

        "must go from CountryOfOriginPage to CyaCreateRecord" in {
          navigator.nextPage(
            CountryOfOriginPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad()
        }

        "must go from CommodityCodePage to HasCorrectGoodsPage" in {

          navigator.nextPage(
            CommodityCodePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.hasCorrectGoods.routes.CreateController.onPageLoad(CheckMode)
        }
      }

      "in Update Record Journey" - {

        "must go from CountryOfOriginPage to CyaUpdateRecord" in {
          navigator.nextPage(
            CountryOfOriginUpdatePage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadCountryOfOrigin(testRecordId)
        }

        "must go from ProductReferencePage to CyaUpdateRecord" in {
          navigator.nextPage(
            ProductReferenceUpdatePage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadproductReference(testRecordId)
        }

        "must go from GoodsDescriptionPage to CyaUpdateRecord" in {
          navigator.nextPage(
            GoodsDescriptionUpdatePage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.goodsRecord.routes.CyaUpdateRecordController.onPageLoadGoodsDescription(testRecordId)
        }

        "must go from CommodityCodePage to HasCorrectGoodsCommodityCodeUpdatePage" in {
          navigator.nextPage(
            CommodityCodeUpdatePage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe controllers.hasCorrectGoods.routes.UpdateController.onPageLoad(CheckMode, testRecordId)
        }

      }
    }

    "redirect to JourneyRecovery" - {

      "with no ContinueUrl if none supplied" in {
        val result = navigator.journeyRecovery()
        result.header.status mustEqual SEE_OTHER
        result.header
          .headers("Location") mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }

      "with ContinueUrl if one supplied" in {
        val redirectUrl = Some(RedirectUrl("/redirectUrl"))
        val result      = navigator.journeyRecovery(redirectUrl)
        result.header.status mustEqual SEE_OTHER
        result.header.headers("Location") mustEqual controllers.problem.routes.JourneyRecoveryController
          .onPageLoad(redirectUrl)
          .url
      }
    }
  }

}
