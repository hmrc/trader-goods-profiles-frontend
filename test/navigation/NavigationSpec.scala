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
import base.TestConstants.{testRecordId, userAnswersId}
import controllers.routes
import models._
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages._
import pages.goodsRecord.{CommodityCodePage, CommodityCodeUpdatePage}
import play.api.http.Status.SEE_OTHER
import queries.{CategorisationDetailsQuery, LongerCommodityQuery}
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import java.time.Instant

class NavigationSpec extends SpecBase with BeforeAndAfterEach {

  private val mockCategorisationService = mock[CategorisationService]
  private val navigator                 = new Navigation(mockCategorisationService)

  override def beforeEach(): Unit = {
    reset(mockCategorisationService)
    super.beforeEach()
  }

  "Navigator" - {
    "in Normal mode" - {
      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, emptyUserAnswers) mustBe routes.IndexController.onPageLoad()
      }

      "in Create Record Journey" - {
        "must go from HasCorrectGoodsPage" - {
          "to CyaCreateRecord when answer is Yes" in {
            val answers = UserAnswers(userAnswersId).set(HasCorrectGoodsPage, true).success.value

            navigator.nextPage(HasCorrectGoodsPage, NormalMode, answers) mustBe
              controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad()
          }

          "to CommodityCodePage when answer is No" in {
            val answers = UserAnswers(userAnswersId).set(HasCorrectGoodsPage, false).success.value

            navigator.nextPage(HasCorrectGoodsPage, NormalMode, answers) mustBe
              controllers.goodsRecord.commodityCode.routes.CreateCommodityCodeController.onPageLoad(NormalMode)
          }

          "to JourneyRecoveryPage when answer is not present" in {
            navigator.nextPage(HasCorrectGoodsPage, NormalMode, emptyUserAnswers) mustBe
              controllers.problem.routes.JourneyRecoveryController.onPageLoad()
          }
        }
      }

      "in Update Record Journey" - {
        "must go from longer commodity result page to" - {
          "to categorisation preparation page when answer is yes" in {
            val userAnswers = emptyUserAnswers
              .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
              .success
              .value
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "123456"))
              .success
              .value
              .set(
                LongerCommodityQuery(testRecordId),
                Commodity("123456012", List("Description", "Other"), Instant.now, None)
              )
              .success
              .value

            navigator.nextPage(HasCorrectGoodsLongerCommodityCodePage(testRecordId), NormalMode, userAnswers) mustEqual
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
          }

          "to categorisation preparation page when answer is yes and the longer commodity code is same as short commodity code" in {
            val userAnswers = emptyUserAnswers
              .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
              .success
              .value
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "123456"))
              .success
              .value
              .set(
                LongerCommodityQuery(testRecordId),
                Commodity("1234560000", List("Description", "Other"), Instant.now, None)
              )
              .success
              .value

            navigator.nextPage(HasCorrectGoodsLongerCommodityCodePage(testRecordId), NormalMode, userAnswers) mustEqual
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)
          }

          "to longer commodity page when answer is no" in {
            val userAnswers = emptyUserAnswers
              .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), false)
              .success
              .value
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "123456"))
              .success
              .value
              .set(
                LongerCommodityQuery(testRecordId),
                Commodity("123456012", List("Description", "Other"), Instant.now, None)
              )
              .success
              .value

            navigator.nextPage(HasCorrectGoodsLongerCommodityCodePage(testRecordId), NormalMode, userAnswers) mustEqual
              controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)
          }

          "to longer commodity code page when the longer commodity code is same as short commodity code" in {
            val userAnswers = emptyUserAnswers
              .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
              .success
              .value
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(commodityCode = "123456"))
              .success
              .value
              .set(
                LongerCommodityQuery(testRecordId),
                Commodity("1234560", List("Description", "Other"), Instant.now, None)
              )
              .success
              .value

            navigator.nextPage(HasCorrectGoodsLongerCommodityCodePage(testRecordId), NormalMode, userAnswers) mustEqual
              controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)
          }
        }

        "to journey recovery page" - {
          "when categorisation details not set" in {
            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe
              controllers.problem.routes.JourneyRecoveryController.onPageLoad()
          }

          "when longer commodity query is not set" in {
            val userAnswers =
              emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            navigator.nextPage(HasCorrectGoodsLongerCommodityCodePage(testRecordId), NormalMode, userAnswers) mustBe
              controllers.problem.routes.JourneyRecoveryController.onPageLoad()
          }

          "when answer is not set" in {
            val userAnswers = emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value
              .set(LongerCommodityQuery(testRecordId), testCommodity.copy(commodityCode = "998877776"))
              .success
              .value
            navigator.nextPage(HasCorrectGoodsLongerCommodityCodePage(testRecordId), NormalMode, userAnswers) mustBe
              controllers.problem.routes.JourneyRecoveryController.onPageLoad()
          }
        }

        "must go from HasCorrectGoodsCommodityCodeUpdatePage" - {
          "to CyaUpdateRecord when answer is Yes" in {
            val answers =
              UserAnswers(userAnswersId).set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true).success.value

            navigator.nextPage(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), NormalMode, answers) mustBe
              controllers.goodsRecord.commodityCode.routes.UpdatedCommodityCodeController.onPageLoad(testRecordId)
          }

          "to CommodityCodePage when answer is No" in {
            val answers =
              UserAnswers(userAnswersId).set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), false).success.value

            navigator.nextPage(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), NormalMode, answers) mustBe
              controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController
                .onPageLoad(NormalMode, testRecordId)
          }

          "to Updated Commodity Code page when answer is not present" in {
            navigator.nextPage(
              HasCorrectGoodsCommodityCodeUpdatePage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe
              controllers.goodsRecord.commodityCode.routes.UpdatedCommodityCodeController.onPageLoad(testRecordId)
          }
        }
      }

      "must go from ReviewReasonPage to Single Record page" in {
        val recordId = testRecordId

        navigator.nextPage(ReviewReasonPage(recordId), NormalMode, emptyUserAnswers) mustEqual
          controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
      }
    }

    "in Check mode" - {
      "must go from a page that doesn't exist in the edit route map to Index" in {
        case object UnknownPage extends Page

        navigator.nextPage(UnknownPage, CheckMode, emptyUserAnswers) mustBe
          controllers.problem.routes.JourneyRecoveryController.onPageLoad()
      }

      "in Create Record Journey" - {
        "must go from HasCorrectGoodsPage" - {
          "when answer is Yes" - {
            "to CommodityCodePage when CommodityCodePage is empty" in {
              val answers = UserAnswers(userAnswersId).set(HasCorrectGoodsPage, true).success.value

              navigator.nextPage(HasCorrectGoodsPage, CheckMode, answers) mustBe
                controllers.goodsRecord.commodityCode.routes.CreateCommodityCodeController.onPageLoad(CheckMode)
            }

            "to CyaCreateRecord when CommodityCodePage is answered" in {
              val answers = UserAnswers(userAnswersId)
                .set(CommodityCodePage, "1234")
                .success
                .value
                .set(HasCorrectGoodsPage, true)
                .success
                .value

              navigator.nextPage(HasCorrectGoodsPage, CheckMode, answers) mustBe
                controllers.goodsRecord.routes.CyaCreateRecordController.onPageLoad()
            }
          }

          "to CommodityCodePage when answer is No" in {
            val answers = UserAnswers(userAnswersId).set(HasCorrectGoodsPage, false).success.value

            navigator.nextPage(HasCorrectGoodsPage, CheckMode, answers) mustBe
              controllers.goodsRecord.commodityCode.routes.CreateCommodityCodeController.onPageLoad(CheckMode)
          }

          "to JourneyRecoveryPage when answer is not present" in {
            navigator.nextPage(HasCorrectGoodsPage, CheckMode, emptyUserAnswers) mustBe
              controllers.problem.routes.JourneyRecoveryController.onPageLoad()
          }
        }
      }

      "in Update Record Journey" - {
        "must go from HasCorrectGoodsCommodityCodeUpdatePage" - {
          "when answer is Yes" - {
            "to CommodityCodePage when CommodityCodePage is empty" in {
              val answers =
                UserAnswers(userAnswersId).set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true).success.value

              navigator.nextPage(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), CheckMode, answers) mustBe
                controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController
                  .onPageLoad(CheckMode, testRecordId)
            }

            "to CyaUpdateRecord when CommodityCodePage is answered" in {
              val answers = UserAnswers(userAnswersId)
                .set(CommodityCodeUpdatePage(testRecordId), "1234")
                .success
                .value
                .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
                .success
                .value

              navigator.nextPage(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), CheckMode, answers) mustBe
                controllers.goodsRecord.commodityCode.routes.CommodityCodeCyaController.onPageLoad(testRecordId)
            }
          }

          "to CommodityCodePage when answer is No" in {
            val answers =
              UserAnswers(userAnswersId).set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), false).success.value

            navigator.nextPage(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), CheckMode, answers) mustBe
              controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController
                .onPageLoad(CheckMode, testRecordId)
          }

          "to JourneyRecoveryPage when answer is not present" in {
            navigator.nextPage(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), CheckMode, emptyUserAnswers) mustBe
              controllers.problem.routes.JourneyRecoveryController.onPageLoad()
          }
        }
      }
    }

    ".journeyRecovery" - {
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
}
