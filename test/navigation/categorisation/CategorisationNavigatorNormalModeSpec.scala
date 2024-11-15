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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages._
import pages.categorisation._
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants
import utils.Constants.firstAssessmentNumber

class CategorisationNavigatorNormalModeSpec extends SpecBase with BeforeAndAfterEach {

  private val categorisationService = mock[CategorisationService]

  private val navigator = new CategorisationNavigator(categorisationService)

  private val recordId    = "dummyRecordId"
  private val userAnswers = UserAnswers(recordId)

  "CategorisationNavigator" - {

    "return AssessmentController.onPageLoad for CategoryGuidancePage in normalRoutes" in {

      navigator.normalRoutes(CategoryGuidancePage(recordId))(
        userAnswers
      ) mustBe controllers.categorisation.routes.AssessmentController
        .onPageLoad(NormalMode, recordId, Constants.firstAssessmentNumber)
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
          ) mustBe controllers.categorisation.routes.SupplementaryUnitController
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
          ) mustBe controllers.categorisation.routes.CyaSupplementaryUnitController
            .onPageLoad(
              testRecordId
            )
        }

        "to JourneyRecoveryPage when answer is not present" in {
          val continueUrl =
            RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url)
          navigator.nextPage(
            HasSupplementaryUnitUpdatePage(testRecordId),
            NormalMode,
            emptyUserAnswers
          ) mustBe controllers.problem.routes.JourneyRecoveryController
            .onPageLoad(Some(continueUrl))
        }
      }

      "must go from SupplementaryUnitUpdatePage to CyaSupplementaryUnitController" in {

        navigator.nextPage(
          SupplementaryUnitUpdatePage(testRecordId),
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.categorisation.routes.CyaSupplementaryUnitController.onPageLoad(
          testRecordId
        )
      }

      "must go from CyaSupplementaryUnitController to SingleRecordController" in {

        navigator.nextPage(
          CyaSupplementaryUnitPage(testRecordId),
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.goodsRecord.routes.SingleRecordController
          .onPageLoad(testRecordId)
      }

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
          controllers.categorisation.routes.AssessmentController.onPageLoad(NormalMode, testRecordId, 2)

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

              navigator.nextPage(AssessmentPage(testRecordId, 1), NormalMode, userAnswers) mustEqual
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

              navigator.nextPage(AssessmentPage(testRecordId, 1), NormalMode, userAnswers) mustEqual
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

              navigator.nextPage(AssessmentPage(testRecordId, 1), NormalMode, userAnswers) mustEqual
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

              navigator.nextPage(AssessmentPage(testRecordId, 1), NormalMode, userAnswers) mustEqual
                controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)
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

          navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
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

          navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
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

          navigator.nextPage(AssessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)
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
            controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

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
            controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

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
            controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

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
              controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

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
              controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

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
              controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

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
                controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

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
                controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

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
                controllers.categorisation.routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId)

            }
          }
        }

      }

      "to journey recovery" - {

        "if categorisation details are not defined" in {
          navigator.nextPage(AssessmentPage(testRecordId, 0), NormalMode, emptyUserAnswers) mustEqual
            controllers.problem.routes.JourneyRecoveryController.onPageLoad()
        }

        "if assessment answer is not defined" in {
          val userAnswers =
            emptyUserAnswers
              .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
              .success
              .value

          navigator.nextPage(AssessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
            controllers.problem.routes.JourneyRecoveryController.onPageLoad()
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
            controllers.problem.routes.JourneyRecoveryController.onPageLoad()
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
          controllers.categorisation.routes.SupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

      }

      "to the cya when answer is no" in {

        val userAnswers = emptyUserAnswers
          .set(HasSupplementaryUnitPage(testRecordId), false)
          .success
          .value

        navigator.nextPage(HasSupplementaryUnitPage(testRecordId), NormalMode, userAnswers) mustBe
          controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

      }

      "to JourneyRecoveryPage when answer is not present" in {

        navigator.nextPage(
          HasSupplementaryUnitPage(testRecordId),
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.problem.routes.JourneyRecoveryController
          .onPageLoad()
      }

    }

    "must go from the supplementary unit page to the check your answers" in {

      navigator.nextPage(SupplementaryUnitPage(testRecordId), NormalMode, emptyUserAnswers) mustBe
        controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

    }

    "must go from longer commodity code to has correct longer commodity page" in {
      navigator.nextPage(LongerCommodityCodePage(testRecordId), NormalMode, emptyUserAnswers) mustEqual
        routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(NormalMode, testRecordId)
    }

    "must go from check your answers page" - {

      "to category 1 result when categorisation result is so" in {
        val userAnswers =
          emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

        when(categorisationService.calculateResult(any(), any(), any())).thenReturn(Category1Scenario)

        navigator.nextPage(CyaCategorisationPage(testRecordId), NormalMode, userAnswers) mustBe
          controllers.categorisation.routes.CategorisationResultController
            .onPageLoad(testRecordId, Category1Scenario)
      }

      "to category 2 result when categorisation result is so" in {
        val userAnswers =
          emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

        when(categorisationService.calculateResult(any(), any(), any())).thenReturn(Category2Scenario)

        navigator.nextPage(CyaCategorisationPage(testRecordId), NormalMode, userAnswers) mustBe
          controllers.categorisation.routes.CategorisationResultController
            .onPageLoad(testRecordId, Category2Scenario)
      }

      "to standard goods result when categorisation result is so" in {
        val userAnswers =
          emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

        when(categorisationService.calculateResult(any(), any(), any())).thenReturn(StandardGoodsScenario)

        navigator.nextPage(CyaCategorisationPage(testRecordId), NormalMode, userAnswers) mustBe
          controllers.categorisation.routes.CategorisationResultController
            .onPageLoad(testRecordId, StandardGoodsScenario)
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

        when(categorisationService.calculateResult(eqTo(categorisationInfo), any(), any()))
          .thenReturn(Category1Scenario)
        when(categorisationService.calculateResult(eqTo(longerCommodity), any(), any()))
          .thenReturn(Category2Scenario)

        navigator.nextPage(CyaCategorisationPage(testRecordId), NormalMode, userAnswers) mustBe
          controllers.categorisation.routes.CategorisationResultController
            .onPageLoad(testRecordId, Category2Scenario)
      }

      "to journey recovery when no categorisation info is found" in {
        navigator.nextPage(CyaCategorisationPage(testRecordId), NormalMode, emptyUserAnswers) mustBe
          controllers.problem.routes.JourneyRecoveryController.onPageLoad(
            Some(
              RedirectUrl(
                controllers.categorisation.routes.CategorisationPreparationController
                  .startCategorisation(testRecordId)
                  .url
              )
            )
          )
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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(NormalMode, testRecordId, firstAssessmentNumber)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(NormalMode, testRecordId, firstAssessmentNumber)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(NormalMode, testRecordId, firstAssessmentNumber)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(NormalMode, testRecordId, 3)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(NormalMode, testRecordId, 3)

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

          navigator.nextPage(RecategorisationPreparationPage(testRecordId), NormalMode, userAnswers) mustBe
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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(StandardGoodsNoAssessmentsScenario)

          navigator.nextPage(
            RecategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.CategorisationResultController
            .onPageLoad(testRecordId, StandardGoodsNoAssessmentsScenario)

        }

        "for category 1 no exemptions when there is a category 1 assessment with no exemptions" in {

          val categoryInfoNoAssessments = CategorisationInfo(
            "1234567890",
            "BV",
            Some(validityEndDate),
            Seq(CategoryAssessment("assessmentId", 1, Seq.empty, "measure description", Some("regulationUrl"))),
            Seq.empty,
            None,
            1
          )

          val userAnswers = emptyUserAnswers
            .set(LongerCategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
            .success
            .value

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category1NoExemptionsScenario)

          navigator.nextPage(
            RecategorisationPreparationPage(testRecordId),
            NormalMode,
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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            RecategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            RecategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            RecategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            RecategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            RecategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            RecategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(NormalMode, testRecordId)

        }

      }

      "to journey recovery page when there's no categorisation info" in {
        navigator.nextPage(
          RecategorisationPreparationPage(testRecordId),
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.problem.routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "to journey recovery" - {

      "if categorisation details are not defined" in {
        navigator.nextPage(ReassessmentPage(testRecordId, 0), NormalMode, emptyUserAnswers) mustEqual
          controllers.problem.routes.JourneyRecoveryController.onPageLoad()
      }

      "if assessment answer is not defined" in {
        val userAnswers =
          emptyUserAnswers
            .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

        navigator.nextPage(ReassessmentPage(testRecordId, 0), NormalMode, userAnswers) mustEqual
          controllers.problem.routes.JourneyRecoveryController.onPageLoad()
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
          controllers.problem.routes.JourneyRecoveryController.onPageLoad()
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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
            controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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

        navigator.nextPage(ReassessmentPage(testRecordId, 2), NormalMode, userAnswers) mustEqual
          controllers.categorisation.routes.CyaCategorisationController.onPageLoad(testRecordId)

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
          controllers.categorisation.routes.AssessmentController
            .onPageLoadReassessment(NormalMode, testRecordId, 3)

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
          controllers.categorisation.routes.AssessmentController
            .onPageLoadReassessment(NormalMode, testRecordId, 3)

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
        controllers.categorisation.routes.HasSupplementaryUnitController.onPageLoad(NormalMode, testRecordId)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(NormalMode, testRecordId, 2)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(NormalMode, testRecordId, 2)

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
            controllers.categorisation.routes.AssessmentController
              .onPageLoadReassessment(NormalMode, testRecordId, 2)

        }

      }

    }

    "return IndexController.onPageLoad for other pages in normalRoutes" in {
      val page = new Page {}

      navigator.normalRoutes(page)(userAnswers) mustBe routes.IndexController.onPageLoad()
    }

    "must go from categorisation preparation" - {

      "to category guidance page" - {
        "if assessments need answering" in {
          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
            .success
            .value

          navigator.nextPage(CategorisationPreparationPage(testRecordId), NormalMode, userAnswers) mustEqual
            controllers.categorisation.routes.CategoryGuidanceController.onPageLoad(testRecordId)

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
          ) mustBe controllers.categorisation.routes.CategoryGuidanceController.onPageLoad(testRecordId)

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
          ) mustBe controllers.categorisation.routes.CategoryGuidanceController.onPageLoad(testRecordId)

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
          ) mustBe controllers.categorisation.routes.CategoryGuidanceController.onPageLoad(testRecordId)

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
          ) mustBe controllers.categorisation.routes.CategoryGuidanceController.onPageLoad(testRecordId)

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
          ) mustBe controllers.categorisation.routes.CategoryGuidanceController.onPageLoad(testRecordId)

        }
      }

      "to expired commodity code controller page when commodity code is expired on the same day" in {
        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationInfoWithExpiredCommodityCode)
          .success
          .value

        navigator.nextPage(CategorisationPreparationPage(testRecordId), NormalMode, userAnswers) mustEqual
          controllers.problem.routes.ExpiredCommodityCodeController.onPageLoad(testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(StandardGoodsNoAssessmentsScenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.CategorisationResultController
            .onPageLoad(testRecordId, StandardGoodsNoAssessmentsScenario)

        }

        "for category 1 no exemptions when there is a category 1 assessment with no exemptions" in {

          val categoryInfoNoAssessments = CategorisationInfo(
            "1234567890",
            "BV",
            Some(validityEndDate),
            Seq(CategoryAssessment("assessmentId", 1, Seq.empty, "measure description", Some("regulationUrl"))),
            Seq.empty,
            None,
            1
          )

          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categoryInfoNoAssessments)
            .success
            .value

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category1NoExemptionsScenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.CategorisationResultController
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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category1Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.CategorisationResultController
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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.CategorisationResultController
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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category1Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.CategorisationResultController
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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(StandardGoodsScenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.CategorisationResultController
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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.CategorisationResultController
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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.CategorisationResultController
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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.LongerCommodityCodeController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.LongerCommodityCodeController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.LongerCommodityCodeController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(NormalMode, testRecordId)

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

          when(categorisationService.calculateResult(any(), any(), any()))
            .thenReturn(Category2Scenario)

          navigator.nextPage(
            CategorisationPreparationPage(testRecordId),
            NormalMode,
            userAnswers
          ) mustBe controllers.categorisation.routes.HasSupplementaryUnitController
            .onPageLoad(NormalMode, testRecordId)

        }

      }

      "to journey recovery page when there's no categorisation info" in {
        navigator.nextPage(
          CategorisationPreparationPage(testRecordId),
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.problem.routes.JourneyRecoveryController.onPageLoad()
      }

    }

  }
}
