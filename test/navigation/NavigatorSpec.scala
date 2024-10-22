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
import models.ott.{CategorisationInfo, CategoryAssessment}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages._
import play.api.http.Status.SEE_OTHER
import queries._
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants.firstAssessmentNumber

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

      "in Create Profile Journey" - {

        "must go from ProfileSetupPage" - {

          "to UseExistingUkimsNumber when historic data" in {

            val userAnswers = emptyUserAnswers
              .set(
                HistoricProfileDataQuery,
                HistoricProfileData("GB123456789", "GB123456789", Some("XIUKIMS1234567890"), None, None)
              )
              .success
              .value

            navigator.nextPage(ProfileSetupPage, NormalMode, userAnswers) mustBe routes.UseExistingUkimsNumberController
              .onPageLoad()
          }

          "to UkimsNumberPage when no historic data" in {

            navigator.nextPage(ProfileSetupPage, NormalMode, emptyUserAnswers) mustBe routes.UkimsNumberController
              .onPageLoadCreate(NormalMode)
          }
        }

        "must go from UkimsNumberPage to HasNirmsPage" in {

          navigator.nextPage(UkimsNumberPage, NormalMode, emptyUserAnswers) mustBe routes.HasNirmsController
            .onPageLoadCreate(
              NormalMode
            )
        }

        "must go from UseExistingNirmsPage" - {

          "to HasNirmsPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(UseExistingUkimsNumberPage, true).success.value
            navigator.nextPage(UseExistingUkimsNumberPage, NormalMode, answers) mustBe routes.HasNirmsController
              .onPageLoadCreate(
                NormalMode
              )
          }

          "to UkimsNumberController when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(UseExistingUkimsNumberPage, false).success.value
            navigator.nextPage(UseExistingUkimsNumberPage, NormalMode, answers) mustBe routes.UkimsNumberController
              .onPageLoadCreate(
                NormalMode
              )
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              UseExistingUkimsNumberPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from HasNirmsPage" - {

          "to NirmsNumberPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, true).success.value
            navigator.nextPage(HasNirmsPage, NormalMode, answers) mustBe routes.NirmsNumberController.onPageLoadCreate(
              NormalMode
            )
          }

          "to HasNiphlPage when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, false).success.value
            navigator.nextPage(HasNirmsPage, NormalMode, answers) mustBe routes.HasNiphlController.onPageLoadCreate(
              NormalMode
            )
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasNirmsPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from NirmsNumberPage to HasNiphlPage" in {

          navigator.nextPage(NirmsNumberPage, NormalMode, emptyUserAnswers) mustBe routes.HasNiphlController
            .onPageLoadCreate(
              NormalMode
            )
        }

        "must go from HasNiphlPage" - {

          "to NiphlNumberPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlPage, true).success.value
            navigator.nextPage(HasNiphlPage, NormalMode, answers) mustBe routes.NiphlNumberController.onPageLoadCreate(
              NormalMode
            )
          }

          "to CyaCreateProfile when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlPage, false).success.value
            navigator.nextPage(HasNiphlPage, NormalMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad()
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasNiphlPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from NiphlNumberPage to CyaCreateProfile" in {

          navigator.nextPage(
            NiphlNumberPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CyaCreateProfileController.onPageLoad()
        }

        "must go from CyaCreateProfile to CreateProfileSuccess" in {

          navigator.nextPage(
            CyaCreateProfilePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CreateProfileSuccessController.onPageLoad()
        }
      }

      "in Update Profile Journey" - {

        "must go from UkimsNumberUpdatePage to CyaMaintainProfilePage" in {

          navigator.nextPage(
            UkimsNumberUpdatePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CyaMaintainProfileController.onPageLoadUkimsNumber()
        }

        "must go from HasNirmsUpdatePage" - {

          "to NirmsNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsUpdatePage, true).success.value
            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              answers
            ) mustBe routes.NirmsNumberController.onPageLoadUpdate(NormalMode)
          }

          "to RemoveNirmsPage when answer is No and Nirms number is associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value
              .set(TraderProfileQuery, TraderProfile("actorId", "ukims", Some("nirms"), Some("niphl"), false))
              .success
              .value

            navigator.nextPage(HasNirmsUpdatePage, NormalMode, answers) mustBe routes.RemoveNirmsController
              .onPageLoad()
          }

          "to RemoveNirmsPage when answer is No and Nirms number is not associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value
              .set(TraderProfileQuery, TraderProfile("actorId", "ukims", None, Some("niphl"), false))
              .success
              .value

            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              answers
            ) mustBe routes.CyaMaintainProfileController.onPageLoadNirms()
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }

          "to JourneyRecoveryPage when TraderProfileQuery not present" in {
            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value

            val continueUrl = RedirectUrl(routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              answers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }
        }

        "must go from RemoveNirmsPage" - {

          "to CyaMaintainProfile when user answered No" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNirmsPage, false).success.value

            navigator.nextPage(
              RemoveNirmsPage,
              NormalMode,
              answers
            ) mustBe routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
          }

          "to Cya NIRMS registered when user answered yes" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNirmsPage, true).success.value

            navigator.nextPage(
              RemoveNirmsPage,
              NormalMode,
              answers
            ) mustBe routes.CyaMaintainProfileController.onPageLoadNirms()
          }

          "to ProfilePage when answer is not present" in {

            navigator.nextPage(
              RemoveNirmsPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.ProfileController.onPageLoad()
          }
        }

        "must go from NirmsNumberUpdatePage to CyaMaintainProfile" in {

          navigator.nextPage(
            NirmsNumberUpdatePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
        }

        "must go from CyaMaintainProfilePage to CyaMaintainProfile" in {

          navigator.nextPage(
            CyaMaintainProfilePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.ProfileController.onPageLoad()
        }

        "must go from HasNiphlUpdatePage" - {

          "to NiphlNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlUpdatePage, true).success.value
            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              answers
            ) mustBe routes.NiphlNumberController.onPageLoadUpdate(NormalMode)
          }

          "to RemoveNiphlPage when answer is No and Niphl number is associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value
              .set(
                TraderProfileQuery,
                TraderProfile("actorId", "ukims", Some("nirms"), Some("niphl"), eoriChanged = false)
              )
              .success
              .value

            navigator.nextPage(HasNiphlUpdatePage, NormalMode, answers) mustBe routes.RemoveNiphlController.onPageLoad()
          }

          "to RemoveNiphlPage when answer is No and Niphl number is not associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value
              .set(TraderProfileQuery, TraderProfile("actorId", "ukims", Some("nirms"), None, eoriChanged = false))
              .success
              .value

            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              answers
            ) mustBe routes.CyaMaintainProfileController.onPageLoadNiphl()
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }

          "to JourneyRecoveryPage when TraderProfileQuery not present" in {
            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value

            val continueUrl = RedirectUrl(routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              answers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }
        }

        "must go from RemoveNiphlPage" - {
          "to Check Your Answers for Niphls Number when user answered No" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNiphlPage, false).success.value
            navigator.nextPage(
              RemoveNiphlPage,
              NormalMode,
              answers
            ) mustBe routes.CyaMaintainProfileController.onPageLoadNiphlNumber()
          }

          "to Check Your Answers for NIPHL registered when user answered yes" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNiphlPage, true).success.value
            navigator.nextPage(
              RemoveNiphlPage,
              NormalMode,
              answers
            ) mustBe routes.CyaMaintainProfileController.onPageLoadNiphl()
          }

          "to ProfilePage when answer is not present" in {

            navigator.nextPage(
              RemoveNiphlPage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.ProfileController.onPageLoad()
          }
        }

        "must go from NiphlNumberUpdatePage to ProfilePage" in {

          navigator.nextPage(
            NiphlNumberUpdatePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CyaMaintainProfileController.onPageLoadNiphlNumber()
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

      "in Categorisation Journey" - {

        "must go from categorisation preparation" - {

          "to category guidance page" - {
            "if assessments need answering" in {
              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value

              navigator.nextPage(CategorisationPreparationPage(testRecordId), NormalMode, userAnswers) mustEqual
                routes.CategoryGuidanceController.onPageLoad(testRecordId)

            }

            "if NIPHL is authorised and has a NIPHL assessment and a category 1 assessment" in {

              val categoryInfoWithNiphlAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                None,
                Seq(category1Niphl, category1, category2NoExemptions),
                Seq(category1),
                None,
                1,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNiphlAssessments)
                .success
                .value

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategoryGuidanceController.onPageLoad(testRecordId)

            }

            "if NIRMS is authorised and has a NIRMS assessment, category 1 assessment and category 2 assessment" in {

              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                None,
                Seq(category2Nirms, category1, category2),
                Seq(category1, category2),
                None,
                1,
                isTraderNirmsAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategoryGuidanceController.onPageLoad(testRecordId)

            }

            "if NIRMS is authorised and has a NIRMS assessment, category 1 assessment and category 2 assessment with no exemptions" in {

              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                None,
                Seq(category2Nirms, category1, category2NoExemptions),
                Seq(category1),
                None,
                1,
                isTraderNirmsAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategoryGuidanceController.onPageLoad(testRecordId)

            }

            "if NIRMS is not authorised and has a NIRMS assessment, category 1 assessment and category 2 assessment" in {

              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                None,
                Seq(category2Nirms, category1, category2),
                Seq(category1, category2),
                None,
                1
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategoryGuidanceController.onPageLoad(testRecordId)

            }

            "if NIRMS is not authorised and has a NIRMS assessment, category 1 assessment and category 2 assessment with no exemptions" in {

              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                None,
                Seq(category2Nirms, category1, category2NoExemptions),
                Seq(category1),
                None,
                1
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategoryGuidanceController.onPageLoad(testRecordId)

            }
          }

          "to expired commodity code controller page when commodity code is expired on the same day" in {
            val userAnswers = emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfoWithExpiredCommodityCode)
              .success
              .value

            navigator.nextPage(CategorisationPreparationPage(testRecordId), NormalMode, userAnswers) mustEqual
              routes.ExpiredCommodityCodeController.onPageLoad(testRecordId)

          }

          "to category result page" - {

            "for standard goods no assessment when there are no assessments" in {

              val categoryInfoNoAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                Some(validityEndDate),
                Seq.empty,
                Seq.empty,
                None,
                1
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(StandardGoodsNoAssessmentsScenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategorisationResultController
                .onPageLoad(testRecordId, StandardGoodsNoAssessmentsScenario)

            }

            "for category 1 no exemptions when there is a category 1 assessment with no exemptions" in {

              val categoryInfoNoAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                Some(validityEndDate),
                Seq(CategoryAssessment("assessmentId", 1, Seq.empty, "measure description")),
                Seq.empty,
                None,
                1
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category1NoExemptionsScenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategorisationResultController
                .onPageLoad(testRecordId, Category1NoExemptionsScenario)

            }

            "NIPHL is not authorised and has NIPHL assesments" in {

              val categoryInfoWithNiphlAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                None,
                Seq(category1Niphl, category2NoExemptions),
                Seq.empty,
                None,
                1
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNiphlAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category1Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategorisationResultController
                .onPageLoad(testRecordId, Category1Scenario)

            }

            "NIPHL is authorised and has one NIPHL assessments and category 2 assessment with no exemptions" in {

              val categoryInfoWithNiphlAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                None,
                Seq(category1Niphl, category2NoExemptions),
                Seq.empty,
                None,
                1,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNiphlAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategorisationResultController
                .onPageLoad(testRecordId, Category2Scenario)

            }

            "Niphl is not authorised and has one Niphl assessment and category 2 no exemptions and other category 1 questions" in {

              val categoryInfoWithNiphlAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                None,
                Seq(category1Niphl, category2NoExemptions, category1),
                Seq.empty,
                None,
                1
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNiphlAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category1Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategorisationResultController
                .onPageLoad(testRecordId, Category1Scenario)

            }

            "when only Nirms assessment and have Nirms and is six-digit code with descendants" in {
              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234560000",
                "BV",
                None,
                Seq(category2Nirms),
                Seq.empty,
                None,
                1,
                isTraderNiphlAuthorised = true,
                isTraderNirmsAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(StandardGoodsScenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategorisationResultController
                .onPageLoad(testRecordId, StandardGoodsScenario)

            }

            "when only Nirms assessment and do not have Nirms and is six-digit code without descendants" in {
              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234560000",
                "BV",
                None,
                Seq(category2Nirms),
                Seq.empty,
                None,
                0
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategorisationResultController
                .onPageLoad(testRecordId, Category2Scenario)

            }

            "when only Nirms assessment and do not have Nirms and is ten-digit code" in {
              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234567899",
                "BV",
                None,
                Seq(category2Nirms),
                Seq.empty,
                None,
                1
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategorisationResultController
                .onPageLoad(testRecordId, Category2Scenario)

            }

          }

          "to longer commodity code page" - {

            "when Niphl assessment and has Niphl and is six-digit code with descendants" in {
              val categoryInfoWithNiphlAssessments = CategorisationInfo(
                "1234560000",
                "BV",
                None,
                Seq(category1Niphl, category2NoExemptions),
                Seq.empty,
                None,
                1,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNiphlAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

            }

            "when only Nirms assessment and does not have Nirms and is six-digit code with descendants" in {
              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234560000",
                "BV",
                None,
                Seq(category2Nirms),
                Seq.empty,
                None,
                1,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

            }

            "when no category 1 exemptions but are category 2 exemptions with no answers and six-digit code with descendants" in {
              val categoryInfoNoAssessments = CategorisationInfo(
                "12345600",
                "BV",
                None,
                Seq(category2NoExemptions),
                Seq.empty,
                None,
                1
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

            }

          }

          "to has supplementary unit page" - {

            "when Niphl assessment and has Niphl and is six-digit code with no descendants" in {
              val categoryInfoWithNiphlAssessments = CategorisationInfo(
                "1234560000",
                "BV",
                None,
                Seq(category1Niphl, category2NoExemptions),
                Seq.empty,
                Some("kg"),
                0,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNiphlAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

            "when only Nirms assessment and does not have Nirms and is six-digit code with no descendants" in {
              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234560000",
                "BV",
                None,
                Seq(category2Nirms),
                Seq.empty,
                Some("kg"),
                0,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

            "when no category 1 exemptions but are category 2 exemptions with no answers and six-digit code with no descendants" in {
              val categoryInfoNoAssessments = CategorisationInfo(
                "12345600",
                "BV",
                None,
                Seq(category2NoExemptions),
                Seq.empty,
                Some("kg"),
                0
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

            "when Niphl assessment and has Niphl and is ten-digit code" in {
              val categoryInfoWithNiphlAssessments = CategorisationInfo(
                "1234567891",
                "BV",
                None,
                Seq(category1Niphl, category2NoExemptions),
                Seq.empty,
                Some("kg"),
                0,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNiphlAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

            "when only Nirms assessment and does not have Nirms and is ten-digit code" in {
              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234567891",
                "BV",
                None,
                Seq(category2Nirms),
                Seq.empty,
                Some("kg"),
                1,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

            "when no category 1 exemptions but are category 2 exemptions with no answers and ten-digit code" in {
              val categoryInfoNoAssessments = CategorisationInfo(
                "1234567891",
                "BV",
                None,
                Seq(category2NoExemptions),
                Seq.empty,
                Some("kg"),
                0
              )

              val userAnswers = emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                CategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

          }

          "to journey recovery page when there's no categorisation info" in {
            navigator.nextPage(
              CategorisationPreparationPage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController.onPageLoad()
          }
        }

        "must go from category guidance to the first assessment page" in {

          navigator.nextPage(CategoryGuidancePage(testRecordId), NormalMode, emptyUserAnswers) mustEqual
            routes.AssessmentController.onPageLoad(NormalMode, testRecordId, firstAssessmentNumber)

        }

        "must go from assessment page" - {

          "to the next assessment if answer is yes and there are more assessments" in {
            val userAnswers =
              emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                .success
                .value

            navigator.nextPage(AssessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
              routes.AssessmentController.onPageLoad(NormalMode, testRecordId, 2)

          }

          "to the check your answers page" - {
            "if answer is yes" - {

              "and there are no more assessments" in {
                val userAnswers =
                  emptyUserAnswers
                    .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value

                navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
                  routes.CyaCategorisationController.onPageLoad(testRecordId)

              }

              "the answered questions are all category 1" - {

                "and there are no category 2 questions" in {
                  val catInfo = categorisationInfo.copy(
                    categoryAssessments = Seq(category1, category2),
                    categoryAssessmentsThatNeedAnswers = Seq(category1, category2)
                  )

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfo)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), NormalMode, userAnswers) mustEqual
                    routes.CyaCategorisationController.onPageLoad(testRecordId)

                }

                "and unanswerable category 2 questions and the commodity code length is 10 digits" in {
                  val catInfo = categorisationInfo.copy(
                    categoryAssessmentsThatNeedAnswers = Seq(category1, category2)
                  )

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfo)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), NormalMode, userAnswers) mustEqual
                    routes.CyaCategorisationController.onPageLoad(testRecordId)
                }

                "and unanswerable category 2 questions and the commodity code length is 8 digits" in {
                  val catInfo = categorisationInfo.copy(
                    commodityCode = "12345678",
                    categoryAssessmentsThatNeedAnswers = Seq(category1, category2)
                  )

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfo)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), NormalMode, userAnswers) mustEqual
                    routes.CyaCategorisationController.onPageLoad(testRecordId)
                }

                "and unanswerable category 2 questions and the commodity code length is 6 digits with no descendants" in {
                  val catInfo = categorisationInfo.copy(
                    commodityCode = "123456",
                    categoryAssessmentsThatNeedAnswers = Seq(category1, category2),
                    descendantCount = 0
                  )

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfo)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), NormalMode, userAnswers) mustEqual
                    routes.CyaCategorisationController.onPageLoad(testRecordId)
                }

              }

            }

            "if the answer is no for category 1 assessment" - {
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "if category 2 question has been answered no and 10 digits and there's not a measurement unit" in {
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(measurementUnit = None))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "if category 2 question has been answered no and 8 digits and there's not a measurement unit" in {

              val catInfo     = categorisationInfo.copy(measurementUnit = None, commodityCode = "12345678")
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), catInfo)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "if category 2 question has been answered no and 6 digits with no descendants and there's not a measurement unit" in {

              val catInfo = categorisationInfo.copy(
                commodityCode = "123456",
                descendantCount = 0,
                measurementUnit = None
              )

              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), catInfo)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "if NIPHL is authorised and has NIPHL assessment and the answers are yes for all category 1 assessments" in {

              val categoryInfoWithNiphlAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                None,
                Seq(category1Niphl, category1, category2NoExemptions),
                Seq(category1),
                None,
                1,
                isTraderNiphlAuthorised = true
              )

              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), categoryInfoWithNiphlAssessments)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)
            }
          }

          "to the has supplementary unit page when category 2 question has been answered no and there's a measurement unit" - {
            "and commodity code is 10 digits" in {
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
                routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

            "and commodity code is 8 digits" in {

              val catInfo     = categorisationInfo.copy(commodityCode = "12345678")
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), catInfo)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
                routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

            "and commodity code is 6 digits with no descendants" in {

              val catInfo     = categorisationInfo.copy(commodityCode = "123456", descendantCount = 0)
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), catInfo)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
                routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

          }

          "to the longer commodity code page" - {

            "when answer No to Category 2 assessment" - {

              "and six digit commodity code and descendant count is not zero" in {
                val catInfo6Digits = categorisationInfo.copy(commodityCode = "123456")

                val userAnswers =
                  emptyUserAnswers
                    .set(CategorisationDetailsQuery(testRecordId), catInfo6Digits)
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                    .success
                    .value

                navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
                  routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

              }

              "and six digit commodity code with four padded zeroes and descendant count is not zero" in {
                val catInfo6Digits = categorisationInfo.copy(commodityCode = "1234560000")

                val userAnswers =
                  emptyUserAnswers
                    .set(CategorisationDetailsQuery(testRecordId), catInfo6Digits)
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                    .success
                    .value

                navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
                  routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

              }

              "and six digit commodity code with two padded zeroes and descendant count is not zero" in {
                val catInfo6Digits = categorisationInfo.copy(commodityCode = "12345600")

                val userAnswers =
                  emptyUserAnswers
                    .set(CategorisationDetailsQuery(testRecordId), catInfo6Digits)
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                    .success
                    .value

                navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
                  routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

              }

            }

            "when answer Yes to Category 1 assessment" - {

              "and Category 2 questions exist but cannot be answered" - {

                val catInfoNoCat2Exempts =
                  categorisationInfo.copy("123456", categoryAssessmentsThatNeedAnswers = Seq(category1, category2))

                "and six digit commodity code and descendant count is not zero" in {

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfoNoCat2Exempts)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), NormalMode, userAnswers) mustEqual
                    routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

                }

                "and six digit commodity code with four padded zeroes and descendant count is not zero" in {

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfoNoCat2Exempts)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), NormalMode, userAnswers) mustEqual
                    routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

                }

                "and six digit commodity code with two padded zeroes and descendant count is not zero" in {

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfoNoCat2Exempts)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), NormalMode, userAnswers) mustEqual
                    routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

                }
              }
            }

          }

          "to journey recovery" - {

            "if categorisation details are not defined" in {
              navigator.nextPage(AssessmentPage(testRecordId, 0), NormalMode, emptyUserAnswers) mustEqual
                routes.JourneyRecoveryController.onPageLoad()
            }

            "if assessment answer is not defined" in {
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
                routes.JourneyRecoveryController.onPageLoad()
            }

            "if assessment question is not defined" in {
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 3), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 3), NormalMode, userAnswers) mustEqual
                routes.JourneyRecoveryController.onPageLoad()
            }

          }

        }

        "must go from the has supplementary unit page" - {

          "to the supplementary unit question when answer is yes" in {

            val userAnswers = emptyUserAnswers
              .set(HasSupplementaryUnitPage(testRecordId), true)
              .success
              .value

            navigator.nextPage(HasSupplementaryUnitPage(testRecordId), NormalMode, userAnswers) mustBe
              routes.SupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

          }

          "to the cya when answer is no" in {

            val userAnswers = emptyUserAnswers
              .set(HasSupplementaryUnitPage(testRecordId), false)
              .success
              .value

            navigator.nextPage(HasSupplementaryUnitPage(testRecordId), NormalMode, userAnswers) mustBe
              routes.CyaCategorisationController.onPageLoad(testRecordId)

          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasSupplementaryUnitPage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }

        }

        "must go from the supplementary unit page to the check your answers" in {

          navigator.nextPage(SupplementaryUnitPage(testRecordId), NormalMode, emptyUserAnswers) mustBe
            routes.CyaCategorisationController.onPageLoad(testRecordId)

        }

        "must go from check your answers page" - {

          "to category 1 result when categorisation result is so" in {
            val userAnswers =
              emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value

            when(mockCategorisationService.calculateResult(any(), any(), any())).thenReturn(Category1Scenario)

            navigator.nextPage(CyaCategorisationPage(testRecordId), NormalMode, userAnswers) mustBe
              routes.CategorisationResultController.onPageLoad(testRecordId, Category1Scenario)
          }

          "to category 2 result when categorisation result is so" in {
            val userAnswers =
              emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value

            when(mockCategorisationService.calculateResult(any(), any(), any())).thenReturn(Category2Scenario)

            navigator.nextPage(CyaCategorisationPage(testRecordId), NormalMode, userAnswers) mustBe
              routes.CategorisationResultController.onPageLoad(testRecordId, Category2Scenario)
          }

          "to standard goods result when categorisation result is so" in {
            val userAnswers =
              emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value

            when(mockCategorisationService.calculateResult(any(), any(), any())).thenReturn(StandardGoodsScenario)

            navigator.nextPage(CyaCategorisationPage(testRecordId), NormalMode, userAnswers) mustBe
              routes.CategorisationResultController.onPageLoad(testRecordId, StandardGoodsScenario)
          }

          "use recategorisation answers if longer commodity code entered" in {
            val longerCommodity = categorisationInfo.copy(commodityCode = "1111111111")

            val userAnswers =
              emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(LongerCategorisationDetailsQuery(testRecordId), longerCommodity)
                .success
                .value

            when(mockCategorisationService.calculateResult(eqTo(categorisationInfo), any(), any()))
              .thenReturn(Category1Scenario)
            when(mockCategorisationService.calculateResult(eqTo(longerCommodity), any(), any()))
              .thenReturn(Category2Scenario)

            navigator.nextPage(CyaCategorisationPage(testRecordId), NormalMode, userAnswers) mustBe
              routes.CategorisationResultController.onPageLoad(testRecordId, Category2Scenario)
          }

          "to journey recovery when no categorisation info is found" in {
            navigator.nextPage(CyaCategorisationPage(testRecordId), NormalMode, emptyUserAnswers) mustBe
              routes.JourneyRecoveryController.onPageLoad(
                Some(RedirectUrl(routes.CategorisationPreparationController.startCategorisation(testRecordId).url))
              )
          }
        }

        "must go from longer commodity code to longer commodity code result page" in {
          navigator.nextPage(LongerCommodityCodePage(testRecordId), NormalMode, emptyUserAnswers) mustEqual
            routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(NormalMode, testRecordId)
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
              routes.CategorisationPreparationController.startLongerCategorisation(NormalMode, testRecordId)

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
              routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

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
              routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

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
        }

        "must go from reassessment preparation" - {

          "to first assessment page when" - {
            "first reassessment is unanswered" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), NormalMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(NormalMode, testRecordId, firstAssessmentNumber)

            }

            "first reassessment is set to NotAnsweredYet" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), NormalMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(NormalMode, testRecordId, firstAssessmentNumber)

            }

            "first reassessment is answered but answer was not copied from shorter assessment" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                )
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), NormalMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(NormalMode, testRecordId, firstAssessmentNumber)

            }
          }

          "to the third assessment page when the first two are answered" - {

            "third reassessment is unanswered" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 1),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), NormalMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(NormalMode, testRecordId, 3)

            }

            "third reassessment is set to NotAnsweredYet" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 1),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value
                .set(ReassessmentPage(testRecordId, 2), ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), NormalMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(NormalMode, testRecordId, 3)

            }

          }

          "to the CYA if no questions need reassessing" - {

            "because one is answered no" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 1),
                  ReassessmentAnswer(AssessmentAnswer.NoExemption, isAnswerCopiedFromPreviousAssessment = true)
                )
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), NormalMode, userAnswers) mustBe
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "because all have already been answered" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 1),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 2),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), NormalMode, userAnswers) mustBe
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

          }

          "to category result page" - {

            "for standard goods no assessment when there are no assessments" in {

              val categoryInfoNoAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                Some(validityEndDate),
                Seq.empty,
                Seq.empty,
                None,
                1
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(StandardGoodsNoAssessmentsScenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategorisationResultController
                .onPageLoad(testRecordId, StandardGoodsNoAssessmentsScenario)

            }

            "for category 1 no exemptions when there is a category 1 assessment with no exemptions" in {

              val categoryInfoNoAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                Some(validityEndDate),
                Seq(CategoryAssessment("assessmentId", 1, Seq.empty, "measure description")),
                Seq.empty,
                None,
                1
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category1NoExemptionsScenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.CategorisationResultController
                .onPageLoad(testRecordId, Category1NoExemptionsScenario)

            }

          }

          "to has supplementary unit page" - {

            "when Niphl assessment and has Niphl and is six-digit code with no descendants" in {
              val categoryInfoWithNiphlAssessments = CategorisationInfo(
                "1234560000",
                "BV",
                None,
                Seq(category1Niphl, category2NoExemptions),
                Seq.empty,
                Some("kg"),
                0,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoWithNiphlAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

            "when only Nirms assessment and does not have Nirms and is six-digit code with no descendants" in {
              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234560000",
                "BV",
                None,
                Seq(category2Nirms),
                Seq.empty,
                Some("kg"),
                0,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

            "when no category 1 exemptions but are category 2 exemptions with no answers and six-digit code with no descendants" in {
              val categoryInfoNoAssessments = CategorisationInfo(
                "12345600",
                "BV",
                None,
                Seq(category2NoExemptions),
                Seq.empty,
                Some("kg"),
                0
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

            "when Niphl assessment and has Niphl and is ten-digit code" in {
              val categoryInfoWithNiphlAssessments = CategorisationInfo(
                "1234567891",
                "BV",
                None,
                Seq(category1Niphl, category2NoExemptions),
                Seq.empty,
                Some("kg"),
                0,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoWithNiphlAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

            "when only Nirms assessment and does not have Nirms and is ten-digit code" in {
              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234567891",
                "BV",
                None,
                Seq(category2Nirms),
                Seq.empty,
                Some("kg"),
                1,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

            "when no category 1 exemptions but are category 2 exemptions with no answers and ten-digit code" in {
              val categoryInfoNoAssessments = CategorisationInfo(
                "1234567891",
                "BV",
                None,
                Seq(category2NoExemptions),
                Seq.empty,
                Some("kg"),
                0
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                NormalMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

            }

          }

          "to journey recovery page when there's no categorisation info" in {
            navigator.nextPage(
              RecategorisationPreparationPage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController.onPageLoad()
          }
        }

        "must go from reassessment page" - {

          "to the next reassessment if answer is yes and there are more assessments" - {

            "and next question is not set" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(NormalMode, testRecordId, 2)

            }

            "and next question is set to not answered placeholder" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(ReassessmentPage(testRecordId, 1), ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(NormalMode, testRecordId, 2)

            }

            "and next question is answered but was not copied from shorter assessment" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(NormalMode, testRecordId, 2)

            }

          }

          "to a later reassessment if the next one is answered yes" - {

            "and the one after is not set" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(NormalMode, testRecordId, 3)

            }

            "and the one after is set to not answered placeholder" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 2),
                    ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet, isAnswerCopiedFromPreviousAssessment = true)
                  )
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(NormalMode, testRecordId, 3)

            }

          }

          "to the check your answers page" - {
            "if answer is yes" - {

              "and there are no more assessments" in {
                val userAnswers =
                  emptyUserAnswers
                    .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                    .success
                    .value
                    .set(
                      ReassessmentPage(testRecordId, 0),
                      ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    )
                    .success
                    .value
                    .set(
                      ReassessmentPage(testRecordId, 1),
                      ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    )
                    .success
                    .value
                    .set(
                      ReassessmentPage(testRecordId, 2),
                      ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    )
                    .success
                    .value

                navigator.nextPage(ReassessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
                  routes.CyaCategorisationController.onPageLoad(testRecordId)

              }

              "and the next one is answered no" in {
                val userAnswers =
                  emptyUserAnswers
                    .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                    .success
                    .value
                    .set(
                      ReassessmentPage(testRecordId, 0),
                      ReassessmentAnswer(
                        AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                        isAnswerCopiedFromPreviousAssessment = true
                      )
                    )
                    .success
                    .value
                    .set(
                      ReassessmentPage(testRecordId, 1),
                      ReassessmentAnswer(AssessmentAnswer.NoExemption, isAnswerCopiedFromPreviousAssessment = true)
                    )
                    .success
                    .value

                navigator.nextPage(ReassessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
                  routes.CyaCategorisationController.onPageLoad(testRecordId)

              }

              "and the next one is answered yes and the one after is no exception" in {
                val userAnswers =
                  emptyUserAnswers
                    .set(
                      LongerCategorisationDetailsQuery(testRecordId),
                      categorisationInfo.copy(measurementUnit = None)
                    )
                    .success
                    .value
                    .set(
                      ReassessmentPage(testRecordId, 0),
                      ReassessmentAnswer(
                        AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                        isAnswerCopiedFromPreviousAssessment = true
                      )
                    )
                    .success
                    .value
                    .set(
                      ReassessmentPage(testRecordId, 1),
                      ReassessmentAnswer(
                        AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                        isAnswerCopiedFromPreviousAssessment = true
                      )
                    )
                    .success
                    .value
                    .set(
                      ReassessmentPage(testRecordId, 2),
                      ReassessmentAnswer(AssessmentAnswer.NoExemption, isAnswerCopiedFromPreviousAssessment = true)
                    )
                    .success
                    .value

                navigator.nextPage(ReassessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
                  routes.CyaCategorisationController.onPageLoad(testRecordId)

              }
            }

            "if the Assessment answer is no for category 1 assessment" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.NoExemption))
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "if category 2 question has been answered no and there's not a measurement unit" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo.copy(measurementUnit = None))
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(ReassessmentPage(testRecordId, 2), ReassessmentAnswer(AssessmentAnswer.NoExemption))
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

          }

          "to the has supplementary unit page when category 2 question has been answered no and there's a measurement unit" in {
            val userAnswers =
              emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                )
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 1),
                  ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                )
                .success
                .value
                .set(ReassessmentPage(testRecordId, 2), ReassessmentAnswer(AssessmentAnswer.NoExemption))
                .success
                .value

            navigator.nextPage(ReassessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
              routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

          }

          "to journey recovery" - {

            "if categorisation details are not defined" in {
              navigator.nextPage(ReassessmentPage(testRecordId, 0), NormalMode, emptyUserAnswers) mustEqual
                routes.JourneyRecoveryController.onPageLoad()
            }

            "if assessment answer is not defined" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
                routes.JourneyRecoveryController.onPageLoad()
            }

            "if assessment question is not defined" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 2),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(ReassessmentPage(testRecordId, 3), ReassessmentAnswer(AssessmentAnswer.NoExemption))
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 3), NormalMode, userAnswers) mustEqual
                routes.JourneyRecoveryController.onPageLoad()
            }

          }

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
      "in Supplementary Unit Update Journey" - {

        "must go from HasSupplementaryUnitUpdatePage" - {

          "to SupplementaryUnitUpdatePage when answer is Yes" in {

            val answers =
              UserAnswers(userAnswersId).set(HasSupplementaryUnitUpdatePage(testRecordId), true).success.value
            navigator.nextPage(
              HasSupplementaryUnitUpdatePage(testRecordId),
              NormalMode,
              answers
            ) mustBe routes.SupplementaryUnitController
              .onPageLoadUpdate(
                NormalMode,
                testRecordId
              )
          }

          "to CyaSupplementaryUnitController when answer is No" in {

            val answers =
              UserAnswers(userAnswersId).set(HasSupplementaryUnitUpdatePage(testRecordId), false).success.value
            navigator.nextPage(
              HasSupplementaryUnitUpdatePage(testRecordId),
              NormalMode,
              answers
            ) mustBe routes.CyaSupplementaryUnitController
              .onPageLoad(
                testRecordId
              )
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(routes.SingleRecordController.onPageLoad(testRecordId).url)
            navigator.nextPage(
              HasSupplementaryUnitUpdatePage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }
        }

        "must go from SupplementaryUnitUpdatePage to CyaSupplementaryUnitController" in {

          navigator.nextPage(
            SupplementaryUnitUpdatePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CyaSupplementaryUnitController.onPageLoad(
            testRecordId
          )
        }

        "must go from CyaSupplementaryUnitController to SingleRecordController" in {

          navigator.nextPage(
            CyaSupplementaryUnitPage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.SingleRecordController
            .onPageLoad(testRecordId)
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

      "in Supplementary Unit Update Journey" - {

        "must go from HasSupplementaryUnitUpdatePage" - {

          "to SupplementaryUnitUpdatePage when answer is Yes and supplementary unit is not defined" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasSupplementaryUnitUpdatePage(testRecordId), true)
              .success
              .value
              .remove(SupplementaryUnitUpdatePage(testRecordId))
              .success
              .value

            navigator.nextPage(
              HasSupplementaryUnitUpdatePage(testRecordId),
              CheckMode,
              answers
            ) mustBe routes.SupplementaryUnitController
              .onPageLoadUpdate(
                CheckMode,
                testRecordId
              )
          }

          "to CyaSupplementaryUnitController when answer is Yes and supplementary unit is already defined" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasSupplementaryUnitUpdatePage(testRecordId), true)
              .success
              .value
              .set(SupplementaryUnitUpdatePage(testRecordId), "974.0")
              .success
              .value

            navigator.nextPage(
              HasSupplementaryUnitUpdatePage(testRecordId),
              CheckMode,
              answers
            ) mustBe routes.CyaSupplementaryUnitController
              .onPageLoad(testRecordId)
          }

          "to CyaSupplementaryUnitController when answer is No" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasSupplementaryUnitUpdatePage(testRecordId), false)
              .success
              .value

            navigator.nextPage(
              HasSupplementaryUnitUpdatePage(testRecordId),
              CheckMode,
              answers
            ) mustBe routes.CyaSupplementaryUnitController
              .onPageLoad(testRecordId)
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasSupplementaryUnitUpdatePage(testRecordId),
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from SupplementaryUnitUpdatePage to CyaSupplementaryUnitController" in {

          navigator.nextPage(
            SupplementaryUnitUpdatePage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaSupplementaryUnitController
            .onPageLoad(testRecordId)
        }
      }

      "in Create Profile Journey" - {
        "must go from UkimsNumberPage to CyaCreateProfile" in {

          navigator.nextPage(
            UkimsNumberPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaCreateProfileController.onPageLoad()
        }

        "must go from HasNirmsPage" - {

          "when answer is Yes" - {

            "to NirmsNumberPage when NirmsNumberPage is empty" in {

              val answers = UserAnswers(userAnswersId).set(HasNirmsPage, true).success.value
              navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.NirmsNumberController.onPageLoadCreate(
                CheckMode
              )
            }

            "to CyaCreateProfile when NirmsNumberPage is answered" in {

              val answers =
                UserAnswers(userAnswersId)
                  .set(HasNirmsPage, true)
                  .success
                  .value
                  .set(NirmsNumberPage, "1234")
                  .success
                  .value
              navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad()
            }
          }
          "to CyaCreateProfile when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, false).success.value
            navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad()
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasNirmsPage,
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from NirmsNumberPage to CyaCreateProfile" in {

          navigator.nextPage(
            NirmsNumberPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaCreateProfileController.onPageLoad()
        }

        "must go from HasNiphlPage" - {

          "when answer is Yes" - {

            "to NiphlNumberPage when NiphlNumberPage is empty" in {

              val answers = UserAnswers(userAnswersId).set(HasNiphlPage, true).success.value
              navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.NiphlNumberController.onPageLoadCreate(
                CheckMode
              )
            }

            "to CyaCreateProfile when NiphlNumberPage is answered" in {

              val answers =
                UserAnswers(userAnswersId)
                  .set(HasNiphlPage, true)
                  .success
                  .value
                  .set(NiphlNumberPage, "1234")
                  .success
                  .value
              navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad()
            }
          }

          "to CyaCreateProfile when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlPage, false).success.value
            navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad()
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasNiphlPage,
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from NiphlNumberPage to CyaCreateProfile" in {

          navigator.nextPage(
            NiphlNumberPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaCreateProfileController.onPageLoad()
        }
      }

      "in Update Profile Journey" - {

        "must go from UkimsNumberUpdatePage to CyaMaintainProfilePage" in {

          navigator.nextPage(
            UkimsNumberUpdatePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaMaintainProfileController.onPageLoadUkimsNumber()
        }

        "must go from RemoveNirmsPage" - {

          "to CyaMaintainProfile when user answered No and NimrsNumberUpdate is defined" in {

            val answers = UserAnswers(userAnswersId)
              .set(RemoveNirmsPage, false)
              .success
              .value
              .set(NirmsNumberUpdatePage, "some nirms")
              .success
              .value

            navigator.nextPage(
              RemoveNirmsPage,
              CheckMode,
              answers
            ) mustBe routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
          }

          "to CyaMaintainProfile when user answered No" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNirmsPage, false).success.value

            navigator.nextPage(
              RemoveNirmsPage,
              CheckMode,
              answers
            ) mustBe routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
          }

          "to Cya NIRMS registered when user answered yes" in {

            val answers = UserAnswers(userAnswersId).set(RemoveNirmsPage, true).success.value

            navigator.nextPage(
              RemoveNirmsPage,
              CheckMode,
              answers
            ) mustBe routes.CyaMaintainProfileController.onPageLoadNirms()
          }

          "must go from NirmsNumberUpdatePage to CyaMaintainProfile" in {

            navigator.nextPage(
              NirmsNumberUpdatePage,
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
          }
        }

        "must go from HasNirmsUpdatePage" - {

          "to NirmsNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsUpdatePage, true).success.value
            navigator.nextPage(
              HasNirmsUpdatePage,
              CheckMode,
              answers
            ) mustBe routes.NirmsNumberController.onPageLoadUpdate(CheckMode)
          }

          "to RemoveNirmsPage when answer is No and Nirms number is associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value
              .set(
                TraderProfileQuery,
                TraderProfile("actorId", "ukims", Some("nirms"), Some("niphl"), eoriChanged = false)
              )
              .success
              .value
            navigator.nextPage(HasNirmsUpdatePage, CheckMode, answers) mustBe routes.RemoveNirmsController
              .onPageLoad()
          }

          "to RemoveNirmsPage when answer is No and Nirms number is not associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value
              .set(TraderProfileQuery, TraderProfile("actorId", "ukims", None, Some("niphl"), eoriChanged = false))
              .success
              .value

            navigator.nextPage(
              HasNirmsUpdatePage,
              CheckMode,
              answers
            ) mustBe routes.CyaMaintainProfileController.onPageLoadNirms()
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNirmsUpdatePage,
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }

          "to JourneyRecoveryPage when TraderProfileQuery not present" in {
            val answers = UserAnswers(userAnswersId)
              .set(HasNirmsUpdatePage, false)
              .success
              .value

            val continueUrl = RedirectUrl(routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNirmsUpdatePage,
              CheckMode,
              answers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }
        }

        "must go from NirmsNumberUpdatePage to CyaMaintainProfile" in {

          navigator.nextPage(
            NirmsNumberUpdatePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaMaintainProfileController.onPageLoadNirmsNumber()
        }

        "must go from HasNiphlUpdatePage" - {

          "to NiphlNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlUpdatePage, true).success.value
            navigator.nextPage(
              HasNiphlUpdatePage,
              CheckMode,
              answers
            ) mustBe routes.NiphlNumberController.onPageLoadUpdate(CheckMode)
          }

          "to RemoveNiphlPage when answer is No and Niphl number is associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value
              .set(
                TraderProfileQuery,
                TraderProfile("actorId", "ukims", Some("nirms"), Some("niphl"), eoriChanged = false)
              )
              .success
              .value

            navigator.nextPage(HasNiphlUpdatePage, CheckMode, answers) mustBe routes.RemoveNiphlController.onPageLoad()
          }

          "to RemoveNiphlPage when answer is No and Niphl number is not associated to profile" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value
              .set(TraderProfileQuery, TraderProfile("actorId", "ukims", Some("nirms"), None, eoriChanged = false))
              .success
              .value

            navigator.nextPage(
              HasNiphlUpdatePage,
              CheckMode,
              answers
            ) mustBe routes.CyaMaintainProfileController.onPageLoadNiphl()
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val continueUrl = RedirectUrl(routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNiphlUpdatePage,
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }

          "to JourneyRecoveryPage when TraderProfileQuery not present" in {
            val answers = UserAnswers(userAnswersId)
              .set(HasNiphlUpdatePage, false)
              .success
              .value

            val continueUrl = RedirectUrl(routes.ProfileController.onPageLoad().url)

            navigator.nextPage(
              HasNiphlUpdatePage,
              CheckMode,
              answers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad(Some(continueUrl))
          }
        }

        "must go from NiphlNumberUpdatePage to CyaMaintainProfile" in {

          navigator.nextPage(
            NiphlNumberUpdatePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaMaintainProfileController.onPageLoadNiphlNumber()
        }

        "must go from CyaMaintainProfilePage to CyaMaintainProfile" in {

          navigator.nextPage(
            CyaMaintainProfilePage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.ProfileController.onPageLoad()
        }
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

      "in Categorisation Journey" - {

        "must go from assessment page" - {

          "to the next assessment if answer is yes and there are more assessments and the next one is not answered" in {
            val userAnswers =
              emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                .success
                .value

            navigator.nextPage(AssessmentPage(testRecordId, 0), CheckMode, userAnswers) mustEqual
              routes.AssessmentController.onPageLoad(CheckMode, testRecordId, 2)

          }

          "to the check your answers page" - {
            "if answer is yes" - {

              "and there are no more assessments" in {
                val userAnswers =
                  emptyUserAnswers
                    .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value

                navigator.nextPage(AssessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                  routes.CyaCategorisationController.onPageLoad(testRecordId)

              }

              "the answered questions are all category 1" - {

                "and there are no category 2 questions" in {
                  val catInfo = categorisationInfo.copy(
                    categoryAssessments = Seq(category1, category2),
                    categoryAssessmentsThatNeedAnswers = Seq(category1, category2)
                  )

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfo)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), CheckMode, userAnswers) mustEqual
                    routes.CyaCategorisationController.onPageLoad(testRecordId)

                }

                "and unanswerable category 2 questions and the commodity code length is 10 digits" in {
                  val catInfo = categorisationInfo.copy(
                    categoryAssessmentsThatNeedAnswers = Seq(category1, category2)
                  )

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfo)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), CheckMode, userAnswers) mustEqual
                    routes.CyaCategorisationController.onPageLoad(testRecordId)
                }

                "and unanswerable category 2 questions and the commodity code length is 8 digits" in {
                  val catInfo = categorisationInfo.copy(
                    commodityCode = "12345678",
                    categoryAssessmentsThatNeedAnswers = Seq(category1, category2)
                  )

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfo)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), CheckMode, userAnswers) mustEqual
                    routes.CyaCategorisationController.onPageLoad(testRecordId)
                }

                "and unanswerable category 2 questions and the commodity code length is 6 digits with no descendants" in {
                  val catInfo = categorisationInfo.copy(
                    commodityCode = "123456",
                    categoryAssessmentsThatNeedAnswers = Seq(category1, category2),
                    descendantCount = 0
                  )

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfo)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), CheckMode, userAnswers) mustEqual
                    routes.CyaCategorisationController.onPageLoad(testRecordId)
                }

              }

            }

            "if the Assessment answer is no for category 1 assessment" in {
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 0), CheckMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "if category 2 question has been answered no and 10 digits and there's not a measurement unit" in {
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), categorisationInfo.copy(measurementUnit = None))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "if category 2 question has been answered no and 8 digits and there's not a measurement unit" in {

              val catInfo     = categorisationInfo.copy(measurementUnit = None, commodityCode = "12345678")
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), catInfo)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "if category 2 question has been answered no and 6 digits with no descendants and there's not a measurement unit" in {

              val catInfo = categorisationInfo.copy(
                commodityCode = "123456",
                descendantCount = 0,
                measurementUnit = None
              )

              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), catInfo)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "when category 2 question has been answered no and there's a measurement unit and that has been answered already" in {
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                  .success
                  .value
                  .set(HasSupplementaryUnitPage(testRecordId), false)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

          }

          "to the has supplementary unit page" - {
            "when category 2 question has been answered no and there's a measurement unit and that has not been answered already" - {

              "and commodity code is 10 digits" in {
                val userAnswers =
                  emptyUserAnswers
                    .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                    .success
                    .value

                navigator.nextPage(AssessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                  routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

              }

              "and commodity code is 8 digits" in {

                val catInfo     = categorisationInfo.copy(commodityCode = "12345678")
                val userAnswers =
                  emptyUserAnswers
                    .set(CategorisationDetailsQuery(testRecordId), catInfo)
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                    .success
                    .value

                navigator.nextPage(AssessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                  routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

              }

              "and commodity code is 6 digits with no descendants" in {

                val catInfo     = categorisationInfo.copy(commodityCode = "123456", descendantCount = 0)
                val userAnswers =
                  emptyUserAnswers
                    .set(CategorisationDetailsQuery(testRecordId), catInfo)
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                    .success
                    .value

                navigator.nextPage(AssessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                  routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

              }

            }
          }

          "to the longer commodity code page" - {

            "when answer No to Category 2 assessment" - {

              "and six digit commodity code and descendant count is not zero" in {
                val catInfo6Digits = categorisationInfo.copy(commodityCode = "123456")

                val userAnswers =
                  emptyUserAnswers
                    .set(CategorisationDetailsQuery(testRecordId), catInfo6Digits)
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                    .success
                    .value

                navigator.nextPage(AssessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                  routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

              }

              "and six digit commodity code with four padded zeroes and descendant count is not zero" in {
                val catInfo6Digits = categorisationInfo.copy(commodityCode = "1234560000")

                val userAnswers =
                  emptyUserAnswers
                    .set(CategorisationDetailsQuery(testRecordId), catInfo6Digits)
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                    .success
                    .value

                navigator.nextPage(AssessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                  routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

              }

              "and six digit commodity code with two padded zeroes and descendant count is not zero" in {
                val catInfo6Digits = categorisationInfo.copy(commodityCode = "12345600")

                val userAnswers =
                  emptyUserAnswers
                    .set(CategorisationDetailsQuery(testRecordId), catInfo6Digits)
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                    .success
                    .value
                    .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.NoExemption)
                    .success
                    .value

                navigator.nextPage(AssessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                  routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

              }

            }

            "when answer Yes to Category 1 assessment" - {

              "and Category 2 questions exist but cannot be answered" - {

                val catInfoNoCat2Exempts =
                  categorisationInfo.copy("123456", categoryAssessmentsThatNeedAnswers = Seq(category1, category2))

                "and six digit commodity code and descendant count is not zero" in {

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfoNoCat2Exempts)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), CheckMode, userAnswers) mustEqual
                    routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

                }

                "and six digit commodity code with four padded zeroes and descendant count is not zero" in {

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfoNoCat2Exempts)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), CheckMode, userAnswers) mustEqual
                    routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

                }

                "and six digit commodity code with two padded zeroes and descendant count is not zero" in {

                  val userAnswers =
                    emptyUserAnswers
                      .set(CategorisationDetailsQuery(testRecordId), catInfoNoCat2Exempts)
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value
                      .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                      .success
                      .value

                  navigator.nextPage(AssessmentPage(testRecordId, 1), CheckMode, userAnswers) mustEqual
                    routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

                }
              }
            }

          }

          "to journey recovery" - {

            "if categorisation details are not defined" in {
              navigator.nextPage(AssessmentPage(testRecordId, 0), CheckMode, emptyUserAnswers) mustEqual
                routes.JourneyRecoveryController.onPageLoad()
            }

            "if assessment answer is not defined" in {
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 0), CheckMode, userAnswers) mustEqual
                routes.JourneyRecoveryController.onPageLoad()
            }

            "if assessment question is not defined" in {
              val userAnswers =
                emptyUserAnswers
                  .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 1), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 2), AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  .success
                  .value
                  .set(AssessmentPage(testRecordId, 3), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(AssessmentPage(testRecordId, 3), CheckMode, userAnswers) mustEqual
                routes.JourneyRecoveryController.onPageLoad()
            }

          }

        }

        "must go from longer commodity code to has correct longer commodity page" in {
          navigator.nextPage(LongerCommodityCodePage(testRecordId), CheckMode, emptyUserAnswers) mustEqual
            routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(CheckMode, testRecordId)
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
              CheckMode,
              userAnswers
            ) mustEqual
              routes.CategorisationPreparationController.startLongerCategorisation(CheckMode, testRecordId)

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
              CheckMode,
              userAnswers
            ) mustEqual
              routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

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
              CheckMode,
              userAnswers
            ) mustEqual
              routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

          }

          "to journey recovery page" - {
            "when categorisation details not set" in {
              navigator.nextPage(
                HasCorrectGoodsLongerCommodityCodePage(testRecordId),
                CheckMode,
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
                CheckMode,
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
                CheckMode,
                userAnswers
              ) mustBe routes.JourneyRecoveryController.onPageLoad()
            }
          }
        }

        "must go from reassessment preparation" - {

          "to first assessment page when" - {
            "first reassessment is unanswered" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), CheckMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(CheckMode, testRecordId, firstAssessmentNumber)

            }

            "first reassessment is set to NotAnsweredYet" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), CheckMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(CheckMode, testRecordId, firstAssessmentNumber)

            }

            "first reassessment is answered but answer was not copied from shorter assessment" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                )
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), CheckMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(CheckMode, testRecordId, firstAssessmentNumber)

            }
          }

          "to the third assessment page when the first two are answered" - {

            "third reassessment is unanswered" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 1),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), CheckMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(CheckMode, testRecordId, 3)

            }

            "third reassessment is set to NotAnsweredYet" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 1),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value
                .set(ReassessmentPage(testRecordId, 2), ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), CheckMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(CheckMode, testRecordId, 3)

            }

          }

          "to the CYA if no questions need reassessing" - {

            "because one is answered no" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 1),
                  ReassessmentAnswer(AssessmentAnswer.NoExemption, isAnswerCopiedFromPreviousAssessment = true)
                )
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), CheckMode, userAnswers) mustBe
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "because all have already been answered" in {
              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 1),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 2),
                  ReassessmentAnswer(
                    AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                    isAnswerCopiedFromPreviousAssessment = true
                  )
                )
                .success
                .value

              navigator.nextPage(RecategorisationPreparationPage(testRecordId), CheckMode, userAnswers) mustBe
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

          }

          "to category result page" - {

            "for standard goods no assessment when there are no assessments" in {

              val categoryInfoNoAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                Some(validityEndDate),
                Seq.empty,
                Seq.empty,
                None,
                1
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(StandardGoodsNoAssessmentsScenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                CheckMode,
                userAnswers
              ) mustBe routes.CategorisationResultController
                .onPageLoad(testRecordId, StandardGoodsNoAssessmentsScenario)

            }

            "for category 1 no exemptions when there is a category 1 assessment with no exemptions" in {

              val categoryInfoNoAssessments = CategorisationInfo(
                "1234567890",
                "BV",
                Some(validityEndDate),
                Seq(CategoryAssessment("assessmentId", 1, Seq.empty, "measure description")),
                Seq.empty,
                None,
                1
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category1NoExemptionsScenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                CheckMode,
                userAnswers
              ) mustBe routes.CategorisationResultController
                .onPageLoad(testRecordId, Category1NoExemptionsScenario)

            }

          }

          "to has supplementary unit page" - {

            "when Niphl assessment and has Niphl and is six-digit code with no descendants" in {
              val categoryInfoWithNiphlAssessments = CategorisationInfo(
                "1234560000",
                "BV",
                None,
                Seq(category1Niphl, category2NoExemptions),
                Seq.empty,
                Some("kg"),
                0,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoWithNiphlAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                CheckMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

            }

            "when only Nirms assessment and does not have Nirms and is six-digit code with no descendants" in {
              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234560000",
                "BV",
                None,
                Seq(category2Nirms),
                Seq.empty,
                Some("kg"),
                0,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                CheckMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

            }

            "when no category 1 exemptions but are category 2 exemptions with no answers and six-digit code with no descendants" in {
              val categoryInfoNoAssessments = CategorisationInfo(
                "12345600",
                "BV",
                None,
                Seq(category2NoExemptions),
                Seq.empty,
                Some("kg"),
                0
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                CheckMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

            }

            "when Niphl assessment and has Niphl and is ten-digit code" in {
              val categoryInfoWithNiphlAssessments = CategorisationInfo(
                "1234567891",
                "BV",
                None,
                Seq(category1Niphl, category2NoExemptions),
                Seq.empty,
                Some("kg"),
                0,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoWithNiphlAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                CheckMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

            }

            "when only Nirms assessment and does not have Nirms and is ten-digit code" in {
              val categoryInfoWithNirmsAssessments = CategorisationInfo(
                "1234567891",
                "BV",
                None,
                Seq(category2Nirms),
                Seq.empty,
                Some("kg"),
                1,
                isTraderNiphlAuthorised = true
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoWithNirmsAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                CheckMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

            }

            "when no category 1 exemptions but are category 2 exemptions with no answers and ten-digit code" in {
              val categoryInfoNoAssessments = CategorisationInfo(
                "1234567891",
                "BV",
                None,
                Seq(category2NoExemptions),
                Seq.empty,
                Some("kg"),
                0
              )

              val userAnswers = emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
                .success
                .value

              when(mockCategorisationService.calculateResult(any(), any(), any()))
                .thenReturn(Category2Scenario)

              navigator.nextPage(
                RecategorisationPreparationPage(testRecordId),
                CheckMode,
                userAnswers
              ) mustBe routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

            }

          }

          "to journey recovery page when there's no categorisation info" in {
            navigator.nextPage(
              RecategorisationPreparationPage(testRecordId),
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController.onPageLoad()
          }
        }

        "must go from reassessment page" - {

          "to the next reassessment if answer is yes and there are more assessments" - {

            "and next question is not set" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), CheckMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(CheckMode, testRecordId, 2)

            }

            "and next question is set to not answered placeholder" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(ReassessmentPage(testRecordId, 1), ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet))
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), CheckMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(CheckMode, testRecordId, 2)

            }

            "and next question is answered but was not copied from shorter assessment" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), CheckMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(CheckMode, testRecordId, 2)

            }
          }

          "to a later reassessment if the next one is answered yes" - {

            "and the one after is not set" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), CheckMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(CheckMode, testRecordId, 3)

            }

            "and the one after is set to not answered placeholder" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 2),
                    ReassessmentAnswer(AssessmentAnswer.NotAnsweredYet, isAnswerCopiedFromPreviousAssessment = true)
                  )
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), CheckMode, userAnswers) mustEqual
                routes.AssessmentController.onPageLoadReassessment(CheckMode, testRecordId, 3)

            }

          }

          "to the check your answers page" - {
            "if answer is yes and there are no more assessments" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 2),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "if answer is yes and the next one is answered no" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(AssessmentAnswer.NoExemption, isAnswerCopiedFromPreviousAssessment = true)
                  )
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), CheckMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "if answer is yes and the next question is answered" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 2),
                    ReassessmentAnswer(
                      AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                      isAnswerCopiedFromPreviousAssessment = true
                    )
                  )
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 1), CheckMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "if the Assessment answer is no for category 1 assessment" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.NoExemption))
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), CheckMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

            "if category 2 question has been answered no and there's not a measurement unit" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo.copy(measurementUnit = None))
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(ReassessmentPage(testRecordId, 2), ReassessmentAnswer(AssessmentAnswer.NoExemption))
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
                routes.CyaCategorisationController.onPageLoad(testRecordId)

            }

          }

          "to the has supplementary unit page when category 2 question has been answered no and there's a measurement unit" in {
            val userAnswers =
              emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                )
                .success
                .value
                .set(
                  ReassessmentPage(testRecordId, 1),
                  ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                )
                .success
                .value
                .set(ReassessmentPage(testRecordId, 2), ReassessmentAnswer(AssessmentAnswer.NoExemption))
                .success
                .value

            navigator.nextPage(ReassessmentPage(testRecordId, 2), CheckMode, userAnswers) mustEqual
              routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

          }

          "to journey recovery" - {

            "if categorisation details are not defined" in {
              navigator.nextPage(ReassessmentPage(testRecordId, 0), CheckMode, emptyUserAnswers) mustEqual
                routes.JourneyRecoveryController.onPageLoad()
            }

            "if assessment answer is not defined" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 0), CheckMode, userAnswers) mustEqual
                routes.JourneyRecoveryController.onPageLoad()
            }

            "if assessment question is not defined" in {
              val userAnswers =
                emptyUserAnswers
                  .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 0),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 1),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(
                    ReassessmentPage(testRecordId, 2),
                    ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("TEST_CODE")))
                  )
                  .success
                  .value
                  .set(ReassessmentPage(testRecordId, 3), ReassessmentAnswer(AssessmentAnswer.NoExemption))
                  .success
                  .value

              navigator.nextPage(ReassessmentPage(testRecordId, 3), CheckMode, userAnswers) mustEqual
                routes.JourneyRecoveryController.onPageLoad()
            }

          }

        }

        "must go from the has supplementary unit page" - {

          "to the supplementary unit question when answer is yes" in {

            val userAnswers = emptyUserAnswers
              .set(HasSupplementaryUnitPage(testRecordId), true)
              .success
              .value

            navigator.nextPage(HasSupplementaryUnitPage(testRecordId), CheckMode, userAnswers) mustBe
              routes.SupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

          }

          "to the cya when answer is no" in {

            val userAnswers = emptyUserAnswers
              .set(HasSupplementaryUnitPage(testRecordId), false)
              .success
              .value

            navigator.nextPage(HasSupplementaryUnitPage(testRecordId), CheckMode, userAnswers) mustBe
              routes.CyaCategorisationController.onPageLoad(testRecordId)

          }

          "to the cya when answer is yes and the answer is already defined" in {

            val userAnswers = emptyUserAnswers
              .set(HasSupplementaryUnitPage(testRecordId), true)
              .success
              .value
              .set(SupplementaryUnitPage(testRecordId), "1234")
              .success
              .value

            navigator.nextPage(HasSupplementaryUnitPage(testRecordId), CheckMode, userAnswers) mustBe
              routes.CyaCategorisationController.onPageLoad(testRecordId)

          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasSupplementaryUnitPage(testRecordId),
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }

        }

        "must go from the supplementary unit page to the check your answers" in {

          navigator.nextPage(SupplementaryUnitPage(testRecordId), CheckMode, emptyUserAnswers) mustBe
            routes.CyaCategorisationController.onPageLoad(testRecordId)

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
