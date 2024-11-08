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

package navigation.categorisation

import base.SpecBase
import base.TestConstants.{testRecordId, userAnswersId}
import controllers.routes
import models._
import models.ott.{CategorisationInfo, CategoryAssessment}
import navigation.CategorisationNavigator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages._
import pages.categorisation._
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import services.CategorisationService
import utils.Constants.firstAssessmentNumber

class CategorisationNavigatorCheckModeSpec extends SpecBase with BeforeAndAfterEach {

  private val mockCategorisationService = mock[CategorisationService]

  private val navigator = new CategorisationNavigator(mockCategorisationService)

  "CategorisationNavigator" - {

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
          controllers.categorisation.routes.AssessmentController.onPageLoad(CheckMode, testRecordId, 2)

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
              controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
                controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
                controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)
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
                controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)
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
                controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)
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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
              controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

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
              controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

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
              controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

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
              controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

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
              controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

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
              controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

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
                controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

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
                controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

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
                controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(CheckMode, testRecordId)

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

    "must go from reassessment preparation" - {

      "to first assessment page when" - {
        "first reassessment is unanswered" in {
          val userAnswers = emptyUserAnswers
            .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

          navigator.nextPage(RecategorisationPreparationPage(testRecordId), CheckMode, userAnswers) mustEqual
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(CheckMode, testRecordId, firstAssessmentNumber)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(CheckMode, testRecordId, firstAssessmentNumber)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(CheckMode, testRecordId, firstAssessmentNumber)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(CheckMode, testRecordId, 3)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(CheckMode, testRecordId, 3)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
          ) mustBe controllers.categorisation.routes.CategorisationResultController
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
          ) mustBe controllers.categorisation.routes.CategorisationResultController
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
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(CheckMode, testRecordId)

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
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(CheckMode, testRecordId)

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
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(CheckMode, testRecordId)

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
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(CheckMode, testRecordId)

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
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(CheckMode, testRecordId)

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
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(CheckMode, testRecordId)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(CheckMode, testRecordId, 2)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(CheckMode, testRecordId, 2)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(CheckMode, testRecordId, 3)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(CheckMode, testRecordId, 3)

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
                ReassessmentAnswer(
                  AssessmentAnswer.Exemption(Seq("TEST_CODE")),
                  isAnswerCopiedFromPreviousAssessment = true
                )
              )
              .success
              .value

          navigator.nextPage(ReassessmentPage(testRecordId, 0), CheckMode, userAnswers) mustEqual
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(CheckMode, testRecordId, 3)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
          controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

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
          controllers.categorisation.routes.SupplementaryUnitController.onPageLoad(CheckMode, testRecordId)

      }

      "to the cya when answer is no" in {

        val userAnswers = emptyUserAnswers
          .set(HasSupplementaryUnitPage(testRecordId), false)
          .success
          .value

        navigator.nextPage(HasSupplementaryUnitPage(testRecordId), CheckMode, userAnswers) mustBe
          controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
          controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
        controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
          ) mustBe controllers.categorisation.routes.SupplementaryUnitController
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
          ) mustBe controllers.categorisation.routes.CyaSupplementaryUnitController
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
          ) mustBe controllers.categorisation.routes.CyaSupplementaryUnitController
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
        ) mustBe controllers.categorisation.routes.CyaSupplementaryUnitController
          .onPageLoad(testRecordId)
      }
    }

  }

}
