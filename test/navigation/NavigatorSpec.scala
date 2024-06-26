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
import pages._
import models._
import models.ott.{CategorisationInfo, CategoryAssessment, Certificate}
import queries.RecordCategorisationsQuery

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
      }

      "in Create Profile Journey" - {

        "must go from ProfileSetupPage to UkimsNumberPage" in {

          navigator.nextPage(ProfileSetupPage, NormalMode, emptyUserAnswers) mustBe routes.UkimsNumberController
            .onPageLoad(NormalMode)
        }

        "must go from UkimsNumberPage to HasNirmsPage" in {

          navigator.nextPage(UkimsNumberPage, NormalMode, emptyUserAnswers) mustBe routes.HasNirmsController.onPageLoad(
            NormalMode
          )
        }

        "must go from HasNirmsPage" - {

          "to NirmsNumberPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, true).success.value
            navigator.nextPage(HasNirmsPage, NormalMode, answers) mustBe routes.NirmsNumberController.onPageLoad(
              NormalMode
            )
          }

          "to HasNiphlPage when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasNirmsPage, false).success.value
            navigator.nextPage(HasNirmsPage, NormalMode, answers) mustBe routes.HasNiphlController.onPageLoad(
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

          navigator.nextPage(NirmsNumberPage, NormalMode, emptyUserAnswers) mustBe routes.HasNiphlController.onPageLoad(
            NormalMode
          )
        }

        "must go from HasNiphlPage" - {

          "to NiphlNumberPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(HasNiphlPage, true).success.value
            navigator.nextPage(HasNiphlPage, NormalMode, answers) mustBe routes.NiphlNumberController.onPageLoad(
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
      }

      "in Create Record Journey" - {

        "must go from CreateRecordStartPage to TraderReferencePage" in {

          navigator.nextPage(
            CreateRecordStartPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.TraderReferenceController
            .onPageLoad(NormalMode)
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
              .onPageLoad(
                NormalMode
              )
          }

          "to CountryOfOriginPage when answer is Yes" in {

            val answers = UserAnswers(userAnswersId).set(UseTraderReferencePage, true).success.value
            navigator.nextPage(UseTraderReferencePage, NormalMode, answers) mustBe routes.CountryOfOriginController
              .onPageLoad(NormalMode)
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
          ) mustBe routes.CountryOfOriginController.onPageLoad(
            NormalMode
          )
        }

        "must go from CountryOfOriginPage to CommodityCodePage" in {
          navigator.nextPage(
            CountryOfOriginPage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.CommodityCodeController.onPageLoad(
            NormalMode
          )
        }

        "must go from CommodityCodePage to HasCorrectGoodsPage" in {

          navigator.nextPage(
            CommodityCodePage,
            NormalMode,
            emptyUserAnswers
          ) mustBe routes.HasCorrectGoodsController.onPageLoad(NormalMode)
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
            navigator.nextPage(HasCorrectGoodsPage, NormalMode, answers) mustBe routes.CommodityCodeController
              .onPageLoad(NormalMode)
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
      }

      "must go from an assessment" - {
        val recordId              = testRecordId
        val index                 = 0
        val assessment1           = CategoryAssessment("id1", 1, Seq(Certificate("cert1", "code1", "description1")))
        val assessment2           = CategoryAssessment("id2", 2, Seq(Certificate("cert2", "code2", "description2")))
        val categorisationInfo    = CategorisationInfo("123", Seq(assessment1, assessment2), Some("some measure unit"))
        val recordCategorisations = RecordCategorisations(Map(recordId -> categorisationInfo))

        "to the next assessment when the answer is an exemption and at least one more assessment exists" in {

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
              .set(AssessmentPage(recordId, index), AssessmentAnswer.Exemption("cert1"))
              .success
              .value

          navigator.nextPage(AssessmentPage(recordId, index), NormalMode, answers) mustEqual routes.AssessmentController
            .onPageLoad(NormalMode, recordId, index + 1)
        }

        "to the Check Your Answers page when the answer is an exemption and this is the last assessment" in {

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
              .set(AssessmentPage(recordId, index), AssessmentAnswer.Exemption("cert1"))
              .success
              .value
              .set(AssessmentPage(recordId, index + 1), AssessmentAnswer.Exemption("cert2"))
              .success
              .value

          navigator.nextPage(
            AssessmentPage(recordId, index + 1),
            NormalMode,
            answers
          ) mustEqual routes.CyaCategorisationController
            .onPageLoad(recordId)
        }

        "to the Check Your Answers page when the answer is No Exemption" in {

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
              .set(AssessmentPage(recordId, index), AssessmentAnswer.NoExemption)
              .success
              .value

          navigator.nextPage(
            AssessmentPage(recordId, index),
            NormalMode,
            answers
          ) mustEqual routes.CyaCategorisationController
            .onPageLoad(recordId)
        }

        "to Journey Recovery when RecordCategorisationsQuery is not present" in {
          navigator.nextPage(
            AssessmentPage(recordId, index),
            NormalMode,
            emptyUserAnswers
          ) mustEqual routes.JourneyRecoveryController.onPageLoad()
        }
      }

      "must go from RemoveGoodsRecordPage to page 1 of GoodsRecordsController" in {
        navigator.nextPage(RemoveGoodsRecordPage, NormalMode, emptyUserAnswers) mustEqual routes.GoodsRecordsController
          .onPageLoad(1)
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
        )
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
              navigator.nextPage(HasNirmsPage, CheckMode, answers) mustBe routes.NirmsNumberController.onPageLoad(
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
              navigator.nextPage(HasNiphlPage, CheckMode, answers) mustBe routes.NiphlNumberController.onPageLoad(
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
                .onPageLoad(
                  CheckMode
                )
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
          ) mustBe routes.HasCorrectGoodsController.onPageLoad(CheckMode)
        }

        "must go from HasCorrectGoodsPage" - {

          "when answer is Yes" - {

            "to CommodityCodePage when CommodityCodePage is empty" in {

              val answers = UserAnswers(userAnswersId).set(HasCorrectGoodsPage, true).success.value
              navigator.nextPage(HasCorrectGoodsPage, CheckMode, answers) mustBe routes.CommodityCodeController
                .onPageLoad(
                  CheckMode
                )
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
            ) mustBe routes.CommodityCodeController.onPageLoad(CheckMode)
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

      "must go from an assessment" - {

        val recordId              = testRecordId
        val index                 = 0
        val assessment1           = CategoryAssessment("id1", 1, Seq(Certificate("cert1", "code1", "description1")))
        val assessment2           = CategoryAssessment("id2", 2, Seq(Certificate("cert2", "code2", "description2")))
        val categorisationInfo    = CategorisationInfo("123", Seq(assessment1, assessment2), Some("some measure unit"))
        val recordCategorisations = RecordCategorisations(Map(recordId -> categorisationInfo))

        "to the Check Your Answers Page when the answer is an exemption and the next assessment has been answered" in {

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
              .set(AssessmentPage(recordId, index), AssessmentAnswer.Exemption("cert1"))
              .success
              .value
              .set(AssessmentPage(recordId, index + 1), AssessmentAnswer.Exemption("cert2"))
              .success
              .value

          navigator.nextPage(
            AssessmentPage(recordId, index),
            CheckMode,
            answers
          ) mustEqual routes.CyaCategorisationController
            .onPageLoad(recordId)
        }

        "to the next assessment when the answer is an exemption and the next assessment is unanswered" in {

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
              .set(AssessmentPage(recordId, index), AssessmentAnswer.Exemption("cert1"))
              .success
              .value

          navigator.nextPage(AssessmentPage(recordId, index), CheckMode, answers) mustEqual routes.AssessmentController
            .onPageLoad(CheckMode, recordId, index + 1)
        }

        "to the Check Your Answers page when the answer is an exemption and this is the last assessment" in {

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
              .set(AssessmentPage(recordId, index), AssessmentAnswer.Exemption("cert1"))
              .success
              .value
              .set(AssessmentPage(recordId, index + 1), AssessmentAnswer.Exemption("cert2"))
              .success
              .value

          navigator.nextPage(
            AssessmentPage(recordId, index + 1),
            CheckMode,
            answers
          ) mustEqual routes.CyaCategorisationController
            .onPageLoad(recordId)
        }

        "to the Check Your Answers page when the answer is No Exemption" in {

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
              .set(AssessmentPage(recordId, index), AssessmentAnswer.NoExemption)
              .success
              .value

          navigator.nextPage(
            AssessmentPage(recordId, index),
            CheckMode,
            answers
          ) mustEqual routes.CyaCategorisationController
            .onPageLoad(recordId)
        }
      }

      "in Supplementary Unit Journey" - {

        "must go from HasSupplementaryUnitPage" - {

          "to SupplementaryUnitPage when answer is Yes and answer is undefined" in {

            val answers = UserAnswers(userAnswersId).set(HasSupplementaryUnitPage(testRecordId), true).success.value
            navigator.nextPage(
              HasSupplementaryUnitPage(testRecordId),
              CheckMode,
              answers
            ) mustBe routes.SupplementaryUnitController
              .onPageLoad(
                CheckMode,
                testRecordId
              )
          }

          "to Check Your Answers when answer is Yes and unit is already defined" in {

            val answers = UserAnswers(userAnswersId)
              .set(HasSupplementaryUnitPage(testRecordId), true)
              .success
              .value
              .set(SupplementaryUnitPage(testRecordId), "974.0")
              .success
              .value

            navigator.nextPage(
              HasSupplementaryUnitPage(testRecordId),
              CheckMode,
              answers
            ) mustBe routes.CyaCategorisationController
              .onPageLoad(testRecordId)
          }

          "to Check Your Answers Page when answer is No" in {

            val answers = UserAnswers(userAnswersId).set(HasSupplementaryUnitPage(testRecordId), false).success.value
            navigator.nextPage(
              HasSupplementaryUnitPage(testRecordId),
              CheckMode,
              answers
            ) mustBe routes.CyaCategorisationController
              .onPageLoad(
                testRecordId
              )
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

        "must go from SupplementaryUnitPage to Check Your Answers Page" in {

          navigator.nextPage(
            SupplementaryUnitPage(testRecordId),
            CheckMode,
            emptyUserAnswers
          ) mustBe routes.CyaCategorisationController.onPageLoad(
            testRecordId
          )
        }

      }

    }
  }
}
