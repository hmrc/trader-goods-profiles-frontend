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
import base.TestConstants.{testEori, testRecordId, userAnswersId}
import controllers.routes
import models.GoodsRecordsPagination.firstPage
import pages._
import models._
import models.ott.{CategorisationInfo, CategoryAssessment, Certificate}
import queries.RecordCategorisationsQuery
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants.firstAssessmentIndex

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, emptyUserAnswers) mustBe routes.IndexController.onPageLoad
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

        "must go from ProfileSetupPage to UkimsNumberPage" in {

          navigator.nextPage(ProfileSetupPage, NormalMode, emptyUserAnswers) mustBe routes.UkimsNumberController
            .onPageLoadCreate(NormalMode)
        }

        "must go from UkimsNumberPage to HasNirmsPage" in {

          navigator.nextPage(UkimsNumberPage, NormalMode, emptyUserAnswers) mustBe routes.HasNirmsController
            .onPageLoadCreate(
              NormalMode
            )
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
            navigator.nextPage(HasNiphlPage, NormalMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad
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
          ) mustBe routes.CyaCreateProfileController.onPageLoad
        }

        "must go from CyaCreateProfile to CreateProfileSuccess" in {

          navigator.nextPage(
            CyaCreateProfilePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CreateProfileSuccessController.onPageLoad
        }
      }

      "in Update Profile Journey" - {

        "must go from UkimsNumberUpdatePage to ProfilePage" in {

          navigator.nextPage(
            UkimsNumberUpdatePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.ProfileController.onPageLoad
        }

        "must go from HasNirmsUpdatePage" - {

          "to NirmsNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsUpdatePage, true).success.value
            navigator.nextPage(
              HasNirmsUpdatePage,
              NormalMode,
              answers
            ) mustBe routes.NirmsNumberController.onPageLoadUpdate
          }

          "to RemoveNirmsPage when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsUpdatePage, false).success.value
            navigator.nextPage(HasNirmsUpdatePage, NormalMode, answers) mustBe routes.RemoveNirmsController
              .onPageLoad()
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
        }

        "must go from RemoveNirmsPage to ProfilePage" in {

          navigator.nextPage(
            RemoveNirmsPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.ProfileController.onPageLoad
        }

        "must go from NirmsNumberUpdatePage to ProfilePage" in {

          navigator.nextPage(NirmsNumberUpdatePage, NormalMode, emptyUserAnswers) mustBe routes.ProfileController
            .onPageLoad()
        }

        "must go from HasNiphlUpdatePage" - {

          "to NiphlNumberUpdatePage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlUpdatePage, true).success.value
            navigator.nextPage(
              HasNiphlUpdatePage,
              NormalMode,
              answers
            ) mustBe routes.NiphlNumberController.onPageLoadUpdate
          }

          "to RemoveNiphlPage when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlUpdatePage, false).success.value
            navigator.nextPage(HasNiphlUpdatePage, NormalMode, answers) mustBe routes.RemoveNiphlController
              .onPageLoad()
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
        }

        "must go from RemoveNiphlPage to ProfilePage" in {

          navigator.nextPage(
            RemoveNiphlPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.ProfileController.onPageLoad
        }

        "must go from NiphlNumberUpdatePage to ProfilePage" in {

          navigator.nextPage(
            NiphlNumberUpdatePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.ProfileController.onPageLoad
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

        "must go from TraderReferencePage to UseTraderReferencePage" in {

          navigator.nextPage(
            TraderReferencePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.UseTraderReferenceController
            .onPageLoad(NormalMode)
        }

        "must go from UseTraderReferencePage" - {

          "to GoodsDescriptionPage when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(UseTraderReferencePage, false).success.value
            navigator.nextPage(UseTraderReferencePage, NormalMode, answers) mustBe routes.GoodsDescriptionController
              .onPageLoadCreate(NormalMode)
          }

          "to CountryOfOriginPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(UseTraderReferencePage, true).success.value
            navigator.nextPage(UseTraderReferencePage, NormalMode, answers) mustBe routes.CountryOfOriginController
              .onPageLoadCreate(NormalMode)
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              UseTraderReferencePage,
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
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
            ) mustBe routes.CyaCreateRecordController.onPageLoad
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

        val recordId              = testRecordId
        val indexAssessment1      = 0
        val indexAssessment2      = 1
        val assessment1           = CategoryAssessment("id1", 1, Seq(Certificate("cert1", "code1", "description1")))
        val assessment2           = CategoryAssessment("id2", 2, Seq(Certificate("cert2", "code2", "description2")))
        val categorisationInfo    =
          CategorisationInfo("1234567890", Seq(assessment1, assessment2), Some("some measure unit"), 0)
        val recordCategorisations = RecordCategorisations(Map(recordId -> categorisationInfo))

        "must go from an assessment" - {

          "to CyaCategorisation if the page is marked to redirect there" in {

            val answers =
              emptyUserAnswers
                .set(RecordCategorisationsQuery, recordCategorisations)
                .success
                .value

            navigator.nextPage(
              AssessmentPage(recordId, indexAssessment1, shouldRedirectToCya = true),
              NormalMode,
              answers
            ) mustEqual routes.CyaCategorisationController.onPageLoad(recordId)
          }

          "to the next assessment when the answer is an exemption and at least one more assessment exists" in {

            val answers =
              emptyUserAnswers
                .set(RecordCategorisationsQuery, recordCategorisations)
                .success
                .value
                .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                .success
                .value

            navigator.nextPage(
              AssessmentPage(recordId, indexAssessment1),
              NormalMode,
              answers
            ) mustEqual routes.AssessmentController
              .onPageLoad(NormalMode, recordId, indexAssessment1 + 1)
          }

          "to the Check Your Answers page" - {

            "when the answer is an exemption and this is the last assessment" in {

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, recordCategorisations)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1 + 1), AssessmentAnswer.Exemption("cert2"))
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment1 + 1),
                NormalMode,
                answers
              ) mustEqual routes.CyaCategorisationController
                .onPageLoad(recordId)
            }

            "when the answer is No Exemption for Category 1" in {

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, recordCategorisations)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment1),
                NormalMode,
                answers
              ) mustEqual routes.CyaCategorisationController
                .onPageLoad(recordId)
            }

            "when the answer is No Exemption for Category 2 and the commodity code is 10 digits and no supplementary unit" in {
              val categorisationInfoNoSuppUnit = categorisationInfo.copy(measurementUnit = None)
              val recordCategorisations        = RecordCategorisations(Map(recordId -> categorisationInfoNoSuppUnit))

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, recordCategorisations)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                NormalMode,
                answers
              ) mustEqual routes.CyaCategorisationController
                .onPageLoad(recordId)
            }

            "when the answer is No Exemption for Category 2 and the commodity code is 8 digits and no supplementary unit" in {
              val categorisationInfoNoSuppUnit =
                categorisationInfo.copy(commodityCode = "12345678", measurementUnit = None)
              val eightDigitsRecordCat         =
                RecordCategorisations(Map(recordId -> categorisationInfoNoSuppUnit))

              val answers                      =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, eightDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                NormalMode,
                answers
              ) mustEqual routes.CyaCategorisationController
                .onPageLoad(recordId)
            }

            "when the answer is No Exemption for Category 2 and the commodity code is 6 digits and no supplementary unit and there are no descendants" in {

              val sixDigitsRecordCat =
                RecordCategorisations(
                  Map(recordId -> categorisationInfo.copy(commodityCode = "123456", measurementUnit = None))
                )

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, sixDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                NormalMode,
                answers
              ) mustEqual routes.CyaCategorisationController
                .onPageLoad(recordId)
            }

          }

          "to the enter longer commodity code page when" - {

            "the answer is No Exemption for Category 2 and the commodity code is 6 digits and there are descendants" in {

              val sixDigitsRecordCat =
                RecordCategorisations(
                  Map(recordId -> categorisationInfo.copy(commodityCode = "123456", descendantCount = 3))
                )

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, sixDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                NormalMode,
                answers
              ) mustEqual routes.LongerCommodityCodeController
                .onPageLoad(NormalMode, recordId)
            }

            "the answer is No Exemption for Category 2 and the commodity code is 6 digits with a zero and there are descendants" in {

              val sixDigitsRecordCat =
                RecordCategorisations(
                  Map(recordId -> categorisationInfo.copy(commodityCode = "123450", descendantCount = 3))
                )

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, sixDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                NormalMode,
                answers
              ) mustEqual routes.LongerCommodityCodeController
                .onPageLoad(NormalMode, recordId)
            }

            "the answer is No Exemption for Category 2 and the commodity code is 6 digits padded to 10 and there are descendants" in {

              val sixDigitsRecordCat =
                RecordCategorisations(
                  Map(recordId -> categorisationInfo.copy(commodityCode = "1234560000", descendantCount = 3))
                )

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, sixDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                NormalMode,
                answers
              ) mustEqual routes.LongerCommodityCodeController
                .onPageLoad(NormalMode, recordId)
            }
          }

          "to the has Supplementary unit page when" - {
            "the answer is No Exemption for Category 2 and the commodity code is 10 digits and there's a supplementary unit" in {

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, recordCategorisations)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                NormalMode,
                answers
              ) mustEqual routes.HasSupplementaryUnitController
                .onPageLoad(NormalMode, recordId)
            }

            "the answer is No Exemption for Category 2 and the commodity code is 8 digits and there's a supplementary unit" in {
              val eightDigitsRecordCat =
                RecordCategorisations(Map(recordId -> categorisationInfo.copy(commodityCode = "12345678")))

              val answers              =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, eightDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                NormalMode,
                answers
              ) mustEqual routes.HasSupplementaryUnitController
                .onPageLoad(NormalMode, recordId)
            }

            "the answer is No Exemption for Category 2 and the commodity code is 6 digits and there's a supplementary unit and there are no descendants" in {

              val sixDigitsRecordCat =
                RecordCategorisations(Map(recordId -> categorisationInfo.copy(commodityCode = "123456")))

              val answers            =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, sixDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                NormalMode,
                answers
              ) mustEqual routes.HasSupplementaryUnitController
                .onPageLoad(NormalMode, recordId)
            }

          }

          "to Journey Recovery when RecordCategorisationsQuery is not present" in {
            navigator.nextPage(
              AssessmentPage(recordId, indexAssessment1),
              NormalMode,
              emptyUserAnswers
            ) mustEqual routes.JourneyRecoveryController.onPageLoad()
          }
        }

        "in Supplementary Unit Journey" - {

          "must go from HasSupplementaryUnitPage" - {

            "to SupplementaryUnitPage when answer is Yes" in {

              val answers = UserAnswers(userAnswersId).set(HasSupplementaryUnitPage(testRecordId), true).success.value
              navigator.nextPage(
                HasSupplementaryUnitPage(testRecordId),
                NormalMode,
                answers
              ) mustBe routes.SupplementaryUnitController
                .onPageLoad(
                  NormalMode,
                  testRecordId
                )
            }

            "to Check Your Answers Page when answer is No" in {

              val answers = UserAnswers(userAnswersId).set(HasSupplementaryUnitPage(testRecordId), false).success.value
              navigator.nextPage(
                HasSupplementaryUnitPage(testRecordId),
                NormalMode,
                answers
              ) mustBe routes.CyaCategorisationController
                .onPageLoad(
                  testRecordId
                )
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

          "must go from SupplementaryUnitPage to Check Your Answers Page" in {

            navigator.nextPage(
              SupplementaryUnitPage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.CyaCategorisationController.onPageLoad(
              testRecordId
            )
          }

        }

        "must go from CategoryGuidancePage to Category Assessment page" in {
          val recordId = testRecordId
          val index    = 0
          navigator.nextPage(
            CategoryGuidancePage(recordId),
            NormalMode,
            emptyUserAnswers
          ) mustEqual routes.AssessmentController.onPageLoad(NormalMode, recordId, index)
        }

        "must go from ReviewReasonPage to Single Record page" in {
          val recordId = testRecordId
          navigator.nextPage(
            ReviewReasonPage(recordId),
            NormalMode,
            emptyUserAnswers
          ) mustEqual routes.SingleRecordController.onPageLoad(recordId)
        }

        "must go from CyaCategorisationPage to CategorisationResult page" in {
          val categoryRecord = CategoryRecord(
            eori = testEori,
            recordId = testRecordId,
            category = 1,
            categoryAssessmentsWithExemptions = 0
          )
          navigator.nextPage(
            CyaCategorisationPage(testRecordId, categoryRecord, Scenario.getScenario(categoryRecord)),
            NormalMode,
            emptyUserAnswers
          ) mustEqual routes.CategorisationResultController.onPageLoad(testRecordId, Category1)
        }

        "must go from LongerCommodityCodePage to HasCorrectGoods page" in {
          navigator.nextPage(
            LongerCommodityCodePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustEqual routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(NormalMode, testRecordId)

        }

        "must go from LongerCommodityCodePage to CyaCategorisation page" in {
          navigator.nextPage(
            LongerCommodityCodePage(testRecordId, shouldRedirectToCya = true),
            NormalMode,
            emptyUserAnswers
          ) mustEqual routes.CyaCategorisationController.onPageLoad(testRecordId)
        }

        "must go from HasCorrectGoodsPage for longer commodity codes" - {

          "to CyaCategorisation when answer is Yes and goods do not need recategorising and no supplementary unit" in {

            val categorisationInfoNoSuppUnit = categorisationInfo.copy(measurementUnit = None)

            val answers = UserAnswers(userAnswersId)
              .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
              .success
              .value
              .set(RecordCategorisationsQuery, RecordCategorisations(Map(testRecordId -> categorisationInfoNoSuppUnit)))
              .success
              .value

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              NormalMode,
              answers
            ) mustBe routes.CyaCategorisationController.onPageLoad(testRecordId)
          }

          "to HasSupplementaryUnit when answer is Yes and does not need recategorising and there is supplementary unit on new commodity code" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
              .success
              .value
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              NormalMode,
              answers
            ) mustBe routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)
          }

          "to first Assessment when answer is Yes and need to recategorise" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
              .success
              .value
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId, needToRecategorise = true),
              NormalMode,
              answers
            ) mustBe routes.AssessmentController.onPageLoad(NormalMode, testRecordId, firstAssessmentIndex)
          }

          "to LongerCommodityCodePage when answer is No" in {

            val answers =
              UserAnswers(userAnswersId)
                .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), false)
                .success
                .value
                .set(RecordCategorisationsQuery, recordCategorisations)
                .success
                .value

            navigator.nextPage(HasCorrectGoodsLongerCommodityCodePage(testRecordId), NormalMode, answers) mustBe
              routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)
          }

          "to CyaCategorisation when answer is Yes and goods do not need recategorising and the supplementary unit is already set" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
              .success
              .value
              .set(RecordCategorisationsQuery, RecordCategorisations(Map(testRecordId -> categorisationInfo)))
              .success
              .value
              .set(HasSupplementaryUnitPage(testRecordId), true)
              .success
              .value
              .set(SupplementaryUnitPage(testRecordId), "1234")
              .success
              .value

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              NormalMode,
              answers
            ) mustBe routes.CyaCategorisationController.onPageLoad(testRecordId)
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              NormalMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }

          "to Journey Recovery when RecordCategorisationsQuery is not present" in {
            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(recordId),
              NormalMode,
              emptyUserAnswers
            ) mustEqual routes.JourneyRecoveryController.onPageLoad()
          }
        }
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
          ) mustBe routes.CyaCreateProfileController.onPageLoad
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
              navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad
            }
          }
          "to CyaCreateProfile when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, false).success.value
            navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad
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
          ) mustBe routes.CyaCreateProfileController.onPageLoad
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
              navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad
            }
          }

          "to CyaCreateProfile when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlPage, false).success.value
            navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.CyaCreateProfileController.onPageLoad
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
          ) mustBe routes.CyaCreateProfileController.onPageLoad
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
          ) mustBe routes.CyaCreateRecordController.onPageLoad
        }

        "must go from UseTraderReferencePage" - {

          "when answer is No" - {

            "to GoodsDescriptionPage when GoodsDescriptionPage is empty" in {

              val answers = UserAnswers(userAnswersId).set(UseTraderReferencePage, false).success.value
              navigator.nextPage(UseTraderReferencePage, CheckMode, answers) mustBe routes.GoodsDescriptionController
                .onPageLoadCreate(CheckMode)
            }

            "to CyaCreateRecord when GoodsDescriptionPage is answered" in {

              val answers =
                UserAnswers(userAnswersId)
                  .set(UseTraderReferencePage, false)
                  .success
                  .value
                  .set(GoodsDescriptionPage, "1234")
                  .success
                  .value
              navigator.nextPage(
                UseTraderReferencePage,
                CheckMode,
                answers
              ) mustBe routes.CyaCreateRecordController.onPageLoad
            }
          }

          "to CyaCreateRecord when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(UseTraderReferencePage, true).success.value
            navigator.nextPage(
              UseTraderReferencePage,
              CheckMode,
              answers
            ) mustBe routes.CyaCreateRecordController.onPageLoad
          }

          "to JourneyRecoveryPage when answer is not present" in {

            navigator.nextPage(
              UseTraderReferencePage,
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }
        }

        "must go from GoodsDescriptionPage to CyaCreateRecord" in {
          navigator.nextPage(
            GoodsDescriptionPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaCreateRecordController.onPageLoad
        }

        "must go from CountryOfOriginPage to CyaCreateRecord" in {
          navigator.nextPage(
            CountryOfOriginPage,
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaCreateRecordController.onPageLoad
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
              ) mustBe routes.CyaCreateRecordController.onPageLoad
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

        val recordId              = testRecordId
        val indexAssessment1      = 0
        val indexAssessment2      = 1
        val assessment1           = CategoryAssessment("id1", 1, Seq(Certificate("cert1", "code1", "description1")))
        val assessment2           = CategoryAssessment("id2", 2, Seq(Certificate("cert2", "code2", "description2")))
        val categorisationInfo    =
          CategorisationInfo("1234567890", Seq(assessment1, assessment2), Some("some measure unit"), 0)
        val recordCategorisations = RecordCategorisations(Map(recordId -> categorisationInfo))

        "must go from an assessment" - {

          "to the next assessment" - {
            "when the answer is an exemption and the next assessment is unanswered" in {

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, recordCategorisations)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment1),
                CheckMode,
                answers
              ) mustEqual routes.AssessmentController
                .onPageLoad(CheckMode, recordId, indexAssessment1 + 1)
            }

            "when the answer is an exemption and the next assessment is answered but future ones still need to be answered" in {

              val newCatInfo = categorisationInfo.copy(categoryAssessments =
                Seq(assessment1, assessment2, assessment1.copy(id = "id3"))
              )

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, RecordCategorisations(Map(recordId -> newCatInfo)))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("true"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.Exemption("true"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, 2), AssessmentAnswer.NotAnsweredYet)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment1),
                CheckMode,
                answers
              ) mustEqual routes.AssessmentController
                .onPageLoad(CheckMode, recordId, indexAssessment1 + 1)
            }

          }

          "to the Check Your Answers page" - {

            "when the answer is an exemption and the next assessment has been answered and no unanswered questions" in {

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, recordCategorisations)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1 + 1), AssessmentAnswer.Exemption("cert2"))
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment1),
                CheckMode,
                answers
              ) mustEqual routes.CyaCategorisationController
                .onPageLoad(recordId)
            }

            "when the answer is an exemption and this is the last assessment" in {

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, recordCategorisations)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1 + 1), AssessmentAnswer.Exemption("cert2"))
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment1 + 1),
                CheckMode,
                answers
              ) mustEqual routes.CyaCategorisationController
                .onPageLoad(recordId)
            }

            "when the answer is No Exemption for Category 1 " in {

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, recordCategorisations)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment1),
                CheckMode,
                answers
              ) mustEqual routes.CyaCategorisationController
                .onPageLoad(recordId)
            }

            "when the answer is No Exemption for Category 2 and the commodity code is length 10 and no supplementary unit" in {
              val categorisationInfoNoSuppUnit = categorisationInfo.copy(measurementUnit = None)
              val answers                      =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, RecordCategorisations(Map(recordId -> categorisationInfoNoSuppUnit)))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                CheckMode,
                answers
              ) mustEqual routes.CyaCategorisationController
                .onPageLoad(recordId)
            }

            "when the answer is No Exemption for Category 2 and the commodity code is length 8 and no supplementary unit" in {

              val eightDigitsRecordCat =
                RecordCategorisations(
                  Map(recordId -> categorisationInfo.copy(measurementUnit = None, commodityCode = "12345678"))
                )

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, eightDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                CheckMode,
                answers
              ) mustEqual routes.CyaCategorisationController
                .onPageLoad(recordId)
            }

            "when the answer is No Exemption for Category 2 and the commodity code is 6 digits and no supplementary unit and there are no descendants" in {

              val sixDigitsRecordCat =
                RecordCategorisations(
                  Map(recordId -> categorisationInfo.copy(commodityCode = "123456", measurementUnit = None))
                )

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, sixDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                CheckMode,
                answers
              ) mustEqual routes.CyaCategorisationController
                .onPageLoad(recordId)
            }

          }

          "to the has Supplementary unit page when" - {
            "the answer is No Exemption for Category 2 and the commodity code is 10 digits and there's a supplementary unit" in {

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, recordCategorisations)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                CheckMode,
                answers
              ) mustEqual routes.HasSupplementaryUnitController
                .onPageLoad(CheckMode, recordId)
            }

            "the answer is No Exemption for Category 2 and the commodity code is 8 digits and there's a supplementary unit" in {
              val eightDigitsRecordCat =
                RecordCategorisations(Map(recordId -> categorisationInfo.copy(commodityCode = "12345678")))

              val answers              =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, eightDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                CheckMode,
                answers
              ) mustEqual routes.HasSupplementaryUnitController
                .onPageLoad(CheckMode, recordId)
            }

            "when the answer is No Exemption for Category 2 and the commodity code is 6 digits and there's a supplementary unit and there are no descendants" in {

              val sixDigitsRecordCat =
                RecordCategorisations(Map(recordId -> categorisationInfo.copy(commodityCode = "123456")))

              val answers            =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, sixDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                CheckMode,
                answers
              ) mustEqual routes.HasSupplementaryUnitController
                .onPageLoad(CheckMode, recordId)
            }

          }

          "to the enter longer commodity code page" - {

            "when the answer is No Exemption for Category 2 and the commodity code is 6 digits and there are descendants" in {

              val sixDigitsRecordCat =
                RecordCategorisations(
                  Map(recordId -> categorisationInfo.copy(commodityCode = "123456", descendantCount = 3))
                )

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, sixDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                CheckMode,
                answers
              ) mustEqual routes.LongerCommodityCodeController
                .onPageLoad(CheckMode, recordId)
            }

            "when the answer is No Exemption for Category 2 and the commodity code is 6 digits ending in 0 and there are descendants" in {

              val sixDigitsRecordCat =
                RecordCategorisations(
                  Map(recordId -> categorisationInfo.copy(commodityCode = "123450", descendantCount = 3))
                )

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, sixDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                CheckMode,
                answers
              ) mustEqual routes.LongerCommodityCodeController
                .onPageLoad(CheckMode, recordId)
            }

            "when the answer is No Exemption for Category 2 and the commodity code is 6 digits padded to 10 and there are descendants" in {

              val sixDigitsRecordCat =
                RecordCategorisations(
                  Map(recordId -> categorisationInfo.copy(commodityCode = "1234560000", descendantCount = 3))
                )

              val answers =
                emptyUserAnswers
                  .set(RecordCategorisationsQuery, sixDigitsRecordCat)
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment1), AssessmentAnswer.Exemption("cert1"))
                  .success
                  .value
                  .set(AssessmentPage(recordId, indexAssessment2), AssessmentAnswer.NoExemption)
                  .success
                  .value

              navigator.nextPage(
                AssessmentPage(recordId, indexAssessment2),
                CheckMode,
                answers
              ) mustEqual routes.LongerCommodityCodeController
                .onPageLoad(CheckMode, recordId)
            }

          }
        }

        "must go from LongerCommodityCodePage to HasCorrectGoods page" in {
          navigator.nextPage(
            LongerCommodityCodePage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustEqual routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(CheckMode, testRecordId)

        }

        "must go from HasCorrectGoodsPage for longer commodity codes" - {

          "to CyaCategorisation when answer is Yes and does not need recategorising and no supplementary unit" in {
            val categorisationInfoNoSuppUnit = categorisationInfo.copy(measurementUnit = None)

            val answers = UserAnswers(userAnswersId)
              .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
              .success
              .value
              .set(RecordCategorisationsQuery, RecordCategorisations(Map(testRecordId -> categorisationInfoNoSuppUnit)))
              .success
              .value

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              CheckMode,
              answers
            ) mustBe routes.CyaCategorisationController.onPageLoad(testRecordId)
          }

          "to first Assessment when answer is Yes and need to recategorise" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
              .success
              .value
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId, needToRecategorise = true),
              CheckMode,
              answers
            ) mustBe routes.AssessmentController.onPageLoad(CheckMode, testRecordId, firstAssessmentIndex)
          }

          "to HasSupplementaryUnit when answer is Yes and does not need recategorising and there is supplementary unit on new commodity code" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
              .success
              .value
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              CheckMode,
              answers
            ) mustBe routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)
          }

          "to LongerCommodityCodePage when answer is No" in {

            val answers =
              UserAnswers(userAnswersId)
                .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), false)
                .success
                .value
                .set(RecordCategorisationsQuery, recordCategorisations)
                .success
                .value
            navigator.nextPage(HasCorrectGoodsLongerCommodityCodePage(testRecordId), CheckMode, answers) mustBe
              routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)
          }

          "to CyaCategorisation when answer is Yes and goods do not need recategorising and the supplementary unit is already set" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
              .success
              .value
              .set(RecordCategorisationsQuery, RecordCategorisations(Map(testRecordId -> categorisationInfo)))
              .success
              .value
              .set(HasSupplementaryUnitPage(testRecordId), true)
              .success
              .value
              .set(SupplementaryUnitPage(testRecordId), "1234")
              .success
              .value

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              CheckMode,
              answers
            ) mustBe routes.CyaCategorisationController.onPageLoad(testRecordId)
          }

          "to JourneyRecoveryPage when answer is not present" in {
            val answers =
              UserAnswers(userAnswersId).set(RecordCategorisationsQuery, recordCategorisations).success.value

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              CheckMode,
              answers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }

          "to JourneyRecoveryPage when record categorisation is not present" in {

            navigator.nextPage(
              HasCorrectGoodsLongerCommodityCodePage(testRecordId),
              CheckMode,
              emptyUserAnswers
            ) mustBe routes.JourneyRecoveryController
              .onPageLoad()
          }

        }

      }

    }
  }
}
