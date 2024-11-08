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
import models.GoodsRecordsPagination.firstPage
import models._
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages._
import play.api.http.Status.SEE_OTHER
import queries.{CategorisationDetailsQuery, LongerCommodityQuery}
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import java.time.Instant

class NavigatorSpec extends SpecBase with BeforeAndAfterEach {

  private val mockCategorisationService = mock[CategorisationService]
  private val navigator                 = new Navigator(mockCategorisationService)

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

      "in Require Advice Journey" - {

        "must go from AdviceStartPage to NamePage" in {

          navigator.nextPage(AdviceStartPage(testRecordId), NormalMode, emptyUserAnswers) mustBe routes.NameController
            .onPageLoad(NormalMode, testRecordId)
        }

        "must go from NamePage to EmailPage" in {

          navigator.nextPage(NamePage(testRecordId), NormalMode, emptyUserAnswers) mustBe routes.EmailController
            .onPageLoad(
              NormalMode,
              testRecordId
            )
        }

        "must go from EmailPage to CyaRequestAdviceController" in {

          navigator.nextPage(
            EmailPage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CyaRequestAdviceController.onPageLoad(testRecordId)
        }

        "must go from CyaRequestAdviceController to AdviceSuccess" in {

          navigator.nextPage(
            CyaRequestAdvicePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.AdviceSuccessController
            .onPageLoad(testRecordId)
        }

      }

      "in Withdraw Advice Journey" - {

        "must go from WithdrawAdviceStartPage to ReasonForWithdrawAdvicePage when answer is Yes" in {
          val answers = UserAnswers(userAnswersId).set(WithdrawAdviceStartPage(testRecordId), true).success.value
          navigator.nextPage(
            WithdrawAdviceStartPage(testRecordId),
            NormalMode,
            answers
          ) mustBe routes.ReasonForWithdrawAdviceController
            .onPageLoad(testRecordId)
        }

        "must go from WithdrawAdviceStartPage to SingleRecordPage when answer is No" in {
          val answers = UserAnswers(userAnswersId).set(WithdrawAdviceStartPage(testRecordId), false).success.value
          navigator.nextPage(
            WithdrawAdviceStartPage(testRecordId),
            NormalMode,
            answers
          ) mustBe routes.SingleRecordController
            .onPageLoad(testRecordId)
        }

        "must go from ReasonForWithdrawAdvicePage to WithdrawAdviceSuccessPage" in {

          navigator.nextPage(
            ReasonForWithdrawAdvicePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.WithdrawAdviceSuccessController
            .onPageLoad(
              testRecordId
            )
        }

        "must go to JourneyRecoveryController when there is no answer for WithdrawAdviceStartPage" in {
          val continueUrl = RedirectUrl(routes.SingleRecordController.onPageLoad(testRecordId).url)
          navigator.nextPage(
            WithdrawAdviceStartPage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.JourneyRecoveryController
            .onPageLoad(Some(continueUrl))
        }
      }

      "in Create Record Journey" - {

        "must go from CreateRecordStartPage to TraderReferencePage" in {

          navigator.nextPage(
            CreateRecordStartPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.TraderReferenceController
            .onPageLoadCreate(NormalMode)
        }

        "must go from TraderReferencePage to GoodsDescriptionPage" in {

          navigator.nextPage(
            TraderReferencePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.GoodsDescriptionController
            .onPageLoadCreate(NormalMode)
        }

        "must go from GoodsDescriptionPage to CountryOfOriginPage" in {
          navigator.nextPage(
            GoodsDescriptionPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CountryOfOriginController.onPageLoadCreate(NormalMode)
        }

        "must go from CountryOfOriginPage to CommodityCodePage" in {
          navigator.nextPage(
            CountryOfOriginPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CommodityCodeController.onPageLoadCreate(NormalMode)
        }

        "must go from CommodityCodePage to HasCorrectGoodsPage" in {

          navigator.nextPage(
            CommodityCodePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.HasCorrectGoodsController.onPageLoadCreate(NormalMode)
        }

        "must go from HasCorrectGoodsPage" - {

          "to CyaCreateRecord when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasCorrectGoodsPage, true).success.value
            navigator.nextPage(
              HasCorrectGoodsPage,
              NormalMode,
              answers
            ) mustBe routes.CyaCreateRecordController.onPageLoad()
          }

          "to CommodityCodePage when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasCorrectGoodsPage, false).success.value
            navigator.nextPage(
              HasCorrectGoodsPage,
              NormalMode,
              answers
            ) mustBe routes.CommodityCodeController
              .onPageLoadCreate(NormalMode)
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasCorrectGoodsPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from CyaCreateRecord to CreateRecordSuccess" in {

          navigator.nextPage(
            CyaCreateRecordPage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CreateRecordSuccessController
            .onPageLoad(testRecordId)
        }

      }

      "in Update Record Journey" - {

        "if not answered" - {
          val continueUrl = RedirectUrl(routes.SingleRecordController.onPageLoad(testRecordId).url)

          "must go from HasCountryOfOriginChangePage to JourneyRecoveryController" in {
            navigator.nextPage(
              HasCountryOfOriginChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController.onPageLoad(Some(continueUrl))
          }

          "must go from HasGoodsDescriptionChangePage to JourneyRecoveryController" in {
            navigator.nextPage(
              HasGoodsDescriptionChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController.onPageLoad(Some(continueUrl))
          }

          "must go from HasCommodityCodeChangePage to JourneyRecoveryController" in {
            navigator.nextPage(
              HasCommodityCodeChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController.onPageLoad(Some(continueUrl))
          }
        }

        "if answer is Yes" - {

          "must go from HasCountryOfOriginChangePage to CountryOfOriginUpdatePage" in {
            navigator.nextPage(
              HasCountryOfOriginChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers.set(HasCountryOfOriginChangePage(testRecordId), true).success.value
            ) mustBe routes.CountryOfOriginController.onPageLoadUpdate(NormalMode, testRecordId)
          }

          "must go from HasGoodsDescriptionChangePage to GoodsDescriptionUpdatePage" in {
            navigator.nextPage(
              HasGoodsDescriptionChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers.set(HasGoodsDescriptionChangePage(testRecordId), true).success.value
            ) mustBe routes.GoodsDescriptionController.onPageLoadUpdate(NormalMode, testRecordId)
          }

          "must go from HasCommodityCodeChangePage to CommodityCodeUpdatePage" in {
            navigator.nextPage(
              HasCommodityCodeChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers.set(HasCommodityCodeChangePage(testRecordId), true).success.value
            ) mustBe routes.CommodityCodeController.onPageLoadUpdate(NormalMode, testRecordId)
          }
        }

        "if answer is No" - {

          "must go from HasCountryOfOriginChangePage to SingleRecordController" in {
            navigator.nextPage(
              HasCountryOfOriginChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers.set(HasCountryOfOriginChangePage(testRecordId), false).success.value
            ) mustBe routes.SingleRecordController.onPageLoad(testRecordId)
          }

          "must go from HasGoodsDescriptionChangePage to SingleRecordController" in {
            navigator.nextPage(
              HasGoodsDescriptionChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers.set(HasGoodsDescriptionChangePage(testRecordId), false).success.value
            ) mustBe routes.SingleRecordController.onPageLoad(testRecordId)
          }

          "must go from HasCommodityCodeChangePage to SingleRecordController" in {
            navigator.nextPage(
              HasCommodityCodeChangePage(testRecordId),
              NormalMode,
              emptyUserAnswers.set(HasCommodityCodeChangePage(testRecordId), false).success.value
            ) mustBe routes.SingleRecordController.onPageLoad(testRecordId)
          }
        }

        "must go from CountryOfOriginUpdatePage to CyaUpdateRecord" in {
          navigator.nextPage(
            CountryOfOriginUpdatePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CyaUpdateRecordController.onPageLoadCountryOfOrigin(testRecordId)
        }

        "must go from TraderReferenceUpdatePage to CyaUpdateRecord" in {
          navigator.nextPage(
            TraderReferenceUpdatePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CyaUpdateRecordController.onPageLoadTraderReference(testRecordId)
        }

        "must go from GoodsDescriptionUpdatePage to CyaUpdateRecord" in {
          navigator.nextPage(
            GoodsDescriptionUpdatePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CyaUpdateRecordController.onPageLoadGoodsDescription(testRecordId)
        }

        "must go from CommodityCodeUpdatePage to HasCorrectGoodsCommodityCodeUpdatePage" in {
          navigator.nextPage(
            CommodityCodeUpdatePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.HasCorrectGoodsController.onPageLoadUpdate(NormalMode, testRecordId)
        }

        "must go from longer commodity result page to" - {
          "to categorisation preparation page when answer is yes" in {
            val userAnswers =
              emptyUserAnswers
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

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              NormalMode,
              userAnswers
            ) mustEqual
              controllers.categorisation.routes.CategorisationPreparationController
                .startLongerCategorisation(NormalMode, testRecordId)

          }

          "to longer commodity page when answer is no" in {
            val userAnswers =
              emptyUserAnswers
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

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              NormalMode,
              userAnswers
            ) mustEqual
              controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

          }

          "to longer commodity code page when the longer commodity code is same as short commodity code" in {
            val userAnswers =
              emptyUserAnswers
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

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              NormalMode,
              userAnswers
            ) mustEqual
              controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

          }

        }

        "to journey recovery page" - {
          "when categorisation details not set" in {
            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController.onPageLoad()
          }

          "when longer commodity query is not set" in {

            val userAnswers = emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              NormalMode,
              userAnswers
            ) mustBe routes.JourneyRecoveryController.onPageLoad()
          }

          "when answer is not set" in {

            val userAnswers = emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value
              .set(LongerCommodityQuery(testRecordId), testCommodity.copy(commodityCode = "998877776"))
              .success
              .value
            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              NormalMode,
              userAnswers
            ) mustBe routes.JourneyRecoveryController.onPageLoad()
          }
        }

        "must go from HasCorrectGoodsCommodityCodeUpdatePage" - {

          "to CyaUpdateRecord when answer is Yes" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value

            navigator.nextPage(
              HasCorrectGoodsCommodityCodeUpdatePage(testRecordId),
              NormalMode,
              answers
            ) mustBe routes.CyaUpdateRecordController.onPageLoadCommodityCode(testRecordId)
          }

          "to CommodityCodePage when answer is No" in {

            val answers =
              UserAnswers(userAnswersId).set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), false).success.value
            navigator.nextPage(
              HasCorrectGoodsCommodityCodeUpdatePage(testRecordId),
              NormalMode,
              answers
            ) mustBe routes.CommodityCodeController
              .onPageLoadUpdate(NormalMode, testRecordId)
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasCorrectGoodsCommodityCodeUpdatePage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from CyaUpdateRecord to SingleRecordController" in {

          navigator.nextPage(
            CyaUpdateRecordPage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.SingleRecordController
            .onPageLoad(testRecordId)
        }

      }

      "must go from ReviewReasonPage to Single Record page" in {
        val recordId = testRecordId
        navigator.nextPage(
          ReviewReasonPage(recordId),
          NormalMode,
          emptyUserAnswers
        ) mustEqual routes.SingleRecordController.onPageLoad(recordId)
      }

      "in Viewing Goods Record Journey" - {
        "must go from RemoveGoodsRecordPage to page 1 of GoodsRecordsController" in {
          navigator.nextPage(
            RemoveGoodsRecordPage,
            NormalMode,
            emptyUserAnswers
          ) mustEqual routes.GoodsRecordsController
            .onPageLoad(firstPage)
        }

        "must go from PreviousMovementsRecordsPage to page 1 of the GoodsRecordController" in {
          navigator.nextPage(
            PreviousMovementRecordsPage,
            NormalMode,
            emptyUserAnswers
          ) mustEqual routes.GoodsRecordsController
            .onPageLoad(firstPage)
        }

      }
      "to journey recovery page" - {
        "when categorisation details not set" in {
          navigator.nextPage(
            HasCorrectGoodsLongerCommodityCodePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.JourneyRecoveryController.onPageLoad()
        }

        "when longer commodity query is not set" in {

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

          navigator.nextPage(
            HasCorrectGoodsLongerCommodityCodePage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe routes.JourneyRecoveryController.onPageLoad()
        }

        "when answer is not set" in {

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value
            .set(LongerCommodityQuery(testRecordId), testCommodity.copy(commodityCode = "998877776"))
            .success
            .value
          navigator.nextPage(
            HasCorrectGoodsLongerCommodityCodePage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }

      "in Data Download Journey" - {

        "must go from RequestDataPage to DownloadRequestSuccessController" in {

          navigator.nextPage(
            RequestDataPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.DownloadRequestSuccessController.onPageLoad()
        }
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          emptyUserAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "in Require Advice Journey" - {

        "must go from NamePage to CyaRequestAdviceController" in {

          navigator.nextPage(
            NamePage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaRequestAdviceController.onPageLoad(testRecordId)
        }

        "must go from EmailPage to CyaRequestAdviceController" in {

          navigator.nextPage(
            EmailPage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaRequestAdviceController.onPageLoad(testRecordId)
        }
      }

      "in Create Record Journey" - {

        "must go from TraderReferencePage to CyaCreateRecord" in {

          navigator.nextPage(
            TraderReferencePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaCreateRecordController.onPageLoad()
        }

        "must go from GoodsDescriptionPage to CyaCreateRecord" in {
          navigator.nextPage(
            GoodsDescriptionPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaCreateRecordController.onPageLoad()
        }

        "must go from CountryOfOriginPage to CyaCreateRecord" in {
          navigator.nextPage(
            CountryOfOriginPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaCreateRecordController.onPageLoad()
        }

        "must go from CommodityCodePage to HasCorrectGoodsPage" in {

          navigator.nextPage(
            CommodityCodePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.HasCorrectGoodsController.onPageLoadCreate(CheckMode)
        }

        "must go from HasCorrectGoodsPage" - {

          "when answer is Yes" - {

            "to CommodityCodePage when CommodityCodePage is empty" in {

              val answers = UserAnswers(userAnswersId).set(HasCorrectGoodsPage, true).success.value
              navigator.nextPage(
                HasCorrectGoodsPage,
                CheckMode,
                answers
              ) mustBe routes.CommodityCodeController
                .onPageLoadCreate(CheckMode)
            }

            "to CyaCreateRecord when CommodityCodePage is answered" in {

              val answers =
                UserAnswers(userAnswersId)
                  .set(CommodityCodePage, "1234")
                  .success
                  .value
                  .set(HasCorrectGoodsPage, true)
                  .success
                  .value
              navigator.nextPage(
                HasCorrectGoodsPage,
                CheckMode,
                answers
              ) mustBe routes.CyaCreateRecordController.onPageLoad()
            }
          }

          "to CommodityCodePage when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasCorrectGoodsPage, false).success.value
            navigator.nextPage(
              HasCorrectGoodsPage,
              CheckMode,
              answers
            ) mustBe routes.CommodityCodeController.onPageLoadCreate(CheckMode)
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasCorrectGoodsPage,
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }
      }

      "in Update Record Journey" - {

        "must go from CountryOfOriginPage to CyaUpdateRecord" in {
          navigator.nextPage(
            CountryOfOriginUpdatePage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaUpdateRecordController.onPageLoadCountryOfOrigin(testRecordId)
        }

        "must go from TraderReferencePage to CyaUpdateRecord" in {
          navigator.nextPage(
            TraderReferenceUpdatePage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaUpdateRecordController.onPageLoadTraderReference(testRecordId)
        }

        "must go from GoodsDescriptionPage to CyaUpdateRecord" in {
          navigator.nextPage(
            GoodsDescriptionUpdatePage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaUpdateRecordController.onPageLoadGoodsDescription(testRecordId)
        }

        "must go from CommodityCodePage to HasCorrectGoodsCommodityCodeUpdatePage" in {
          navigator.nextPage(
            CommodityCodeUpdatePage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.HasCorrectGoodsController.onPageLoadUpdate(CheckMode, testRecordId)
        }

        "must go from HasCorrectGoodsCommodityCodeUpdatePage" - {

          "when answer is Yes" - {

            "to CommodityCodePage when CommodityCodePage is empty" in {

              val answers =
                UserAnswers(userAnswersId).set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true).success.value
              navigator.nextPage(
                HasCorrectGoodsCommodityCodeUpdatePage(testRecordId),
                CheckMode,
                answers
              ) mustBe routes.CommodityCodeController
                .onPageLoadUpdate(
                  CheckMode,
                  testRecordId
                )
            }

            "to CyaUpdateRecord when CommodityCodePage is answered" in {

              val answers =
                UserAnswers(userAnswersId)
                  .set(CommodityCodeUpdatePage(testRecordId), "1234")
                  .success
                  .value
                  .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
                  .success
                  .value
              navigator.nextPage(
                HasCorrectGoodsCommodityCodeUpdatePage(testRecordId),
                CheckMode,
                answers
              ) mustBe routes.CyaUpdateRecordController.onPageLoadCommodityCode(testRecordId)
            }
          }

          "to CommodityCodePage when answer is No" in {

            val answers =
              UserAnswers(userAnswersId).set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), false).success.value
            navigator.nextPage(
              HasCorrectGoodsCommodityCodeUpdatePage(testRecordId),
              CheckMode,
              answers
            ) mustBe routes.CommodityCodeController.onPageLoadUpdate(CheckMode, testRecordId)
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasCorrectGoodsCommodityCodeUpdatePage(testRecordId),
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

      }

    }

    ".journeyRecovery" - {

      "redirect to JourneyRecovery" - {

        "with no ContinueUrl if none supplied" in {
          val result = navigator.journeyRecovery()
          result.header.status mustEqual SEE_OTHER
          result.header.headers("Location") mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }

        "with ContinueUrl if one supplied" in {
          val redirectUrl = Some(RedirectUrl("/redirectUrl"))
          val result      = navigator.journeyRecovery(redirectUrl)
          result.header.status mustEqual SEE_OTHER
          result.header.headers("Location") mustEqual routes.JourneyRecoveryController.onPageLoad(redirectUrl).url
        }
      }
    }
  }
}
