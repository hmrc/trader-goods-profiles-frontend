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

package controllers.categorisation

import base.SpecBase
import base.TestConstants.testRecordId
import config.FrontendAppConfig
import forms.AssessmentFormProvider
import models.helper.CategorisationJourney
import models.ott.{CategorisationInfo, CategoryAssessment}
import models.{AssessmentAnswer, NormalMode, ReassessmentAnswer}
import navigation.{CategorisationNavigator, FakeCategorisationNavigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.categorisation.{AssessmentPage, ReassessmentPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants
import views.html.categorisation.AssessmentView

import scala.concurrent.Future

class AssessmentControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private def onwardRoute     = Call("GET", "/foo")
  private val formProvider    = new AssessmentFormProvider()
  private def assessmentRoute =
    controllers.categorisation.routes.AssessmentController
      .onPageLoad(NormalMode, testRecordId, Constants.firstAssessmentNumber)
      .url

  private def reassessmentRoute =
    controllers.categorisation.routes.AssessmentController
      .onPageLoadReassessment(NormalMode, testRecordId, Constants.firstAssessmentNumber)
      .url

  "AssessmentController" - {

    "for initial categorisation" - {
      "onPageLoad" - {

        "must render the view when an assessment can be found for this id" - {

          "and has not previously been answered" in {

            val answers =
              emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute)

              val result = route(application, request).value

              val onSubmitAction               =
                controllers.categorisation.routes.AssessmentController
                  .onSubmit(NormalMode, testRecordId, Constants.firstAssessmentNumber)
              val view                         = application.injector.instanceOf[AssessmentView]
              val appConfig: FrontendAppConfig = application.injector.instanceOf[FrontendAppConfig]
              val form                         = formProvider()
              val expectedCodesAndDescriptions =
                categorisationInfo.categoryAssessmentsThatNeedAnswers.head.exemptions.map { exemption =>
                  (exemption.code, exemption.description)
                }

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                Constants.firstAssessmentNumber,
                expectedCodesAndDescriptions,
                categorisationInfo.commodityCode,
                onSubmitAction,
                categorisationInfo.categoryAssessments.head.themeDescription,
                categorisationInfo.categoryAssessments.head.regulationUrl,
                isReassessment = false
              )(
                request,
                messages(application),
                appConfig
              ).toString
            }
          }

          "and has previously been answered" in {

            val answers =
              emptyUserAnswers
                .set(CategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(AssessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption)
                .success
                .value

            val application = applicationBuilder(userAnswers = Some(answers))
              .build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute)

              val result = route(application, request).value

              val onSubmitAction               =
                controllers.categorisation.routes.AssessmentController
                  .onSubmit(NormalMode, testRecordId, Constants.firstAssessmentNumber)
              val view                         = application.injector.instanceOf[AssessmentView]
              val form                         = formProvider().fill(AssessmentAnswer.NoExemption)
              val appConfig: FrontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

              val expectedCodesAndDescriptions =
                categorisationInfo.categoryAssessmentsThatNeedAnswers.head.exemptions.map { exemption =>
                  (exemption.code, exemption.description)
                }

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                Constants.firstAssessmentNumber,
                expectedCodesAndDescriptions,
                categorisationInfo.commodityCode,
                onSubmitAction,
                categorisationInfo.categoryAssessments.head.themeDescription,
                categorisationInfo.categoryAssessments.head.regulationUrl,
                isReassessment = false
              )(
                request,
                messages(application),
                appConfig
              ).toString
            }
          }
        }

        "must redirect to Journey Recovery" - {

          "when categorisation information does not exist" in {

            val mockSessionRepository = mock[SessionRepository]
            when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
              .build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(
                  Some(
                    RedirectUrl(
                      controllers.categorisation.routes.CategorisationPreparationController
                        .startCategorisation(testRecordId)
                        .url
                    )
                  )
                )
                .url

              withClue("must cleanse the user answers data") {
                verify(mockSessionRepository)
                  .clearData(eqTo(emptyUserAnswers.id), eqTo(CategorisationJourney))
              }
            }
          }

          "when this assessment index cannot be found" in {

            val mockSessionRepository = mock[SessionRepository]
            when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

            val categorisationInfo =
              CategorisationInfo(
                "1234567890",
                "BV",
                Some(validityEndDate),
                Seq.empty[CategoryAssessment],
                Seq.empty[CategoryAssessment],
                None,
                1
              )
            val answers            =
              emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
              .build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(
                  Some(
                    RedirectUrl(
                      controllers.categorisation.routes.CategorisationPreparationController
                        .startCategorisation(testRecordId)
                        .url
                    )
                  )
                )
                .url

              withClue("must cleanse the user answers data") {
                verify(mockSessionRepository)
                  .clearData(eqTo(emptyUserAnswers.id), eqTo(CategorisationJourney))
              }
            }
          }
        }
      }

      "onSubmit" - {

        "must save the answer and redirect to the next page when a valid value is submitted" in {

          val mockRepository = mock[SessionRepository]
          when(mockRepository.set(any())).thenReturn(Future.successful(true))

          val answers =
            emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

          val application =
            applicationBuilder(userAnswers = Some(answers))
              .overrides(
                bind[SessionRepository].toInstance(mockRepository),
                bind[CategorisationNavigator].toInstance(new FakeCategorisationNavigator(onwardRoute))
              )
              .build()

          running(application) {
            val checkedValues = List("none")

            val request = FakeRequest(POST, assessmentRoute).withFormUrlEncodedBody(
              checkedValues.flatMap(value => Seq("value[]" -> value)): _*
            )

            val result = route(application, request).value

            val expectedAnswers =
              answers.set(AssessmentPage(testRecordId, 0), AssessmentAnswer.NoExemption).success.value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
            verify(mockRepository).set(eqTo(expectedAnswers))
          }
        }

        "must return a Bad Request and errors when invalid data is submitted" in {

          val answers =
            emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(POST, assessmentRoute).withFormUrlEncodedBody(("value", ""))

            val result = route(application, request).value

            val onSubmitAction               =
              controllers.categorisation.routes.AssessmentController
                .onSubmit(NormalMode, testRecordId, Constants.firstAssessmentNumber)
            val view                         = application.injector.instanceOf[AssessmentView]
            val form                         = formProvider().fill(AssessmentAnswer.NoExemption)
            val appConfig: FrontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

            val boundForm                    = form.bind(Map("value" -> ""))
            val expectedCodesAndDescriptions =
              categorisationInfo.categoryAssessmentsThatNeedAnswers.head.exemptions.map { exemption =>
                (exemption.code, exemption.description)
              }

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(
              boundForm,
              NormalMode,
              testRecordId,
              Constants.firstAssessmentNumber,
              expectedCodesAndDescriptions,
              categorisationInfo.commodityCode,
              onSubmitAction,
              categorisationInfo.categoryAssessments.head.themeDescription,
              categorisationInfo.categoryAssessments.head.regulationUrl,
              isReassessment = false
            )(
              request,
              messages(application),
              appConfig
            ).toString
          }
        }

        "must redirect to Journey Recovery" - {

          "when categorisation information does not exist" in {

            val mockSessionRepository = mock[SessionRepository]
            when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
              .build()

            running(application) {
              val request = FakeRequest(POST, assessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(
                  Some(
                    RedirectUrl(
                      controllers.categorisation.routes.CategorisationPreparationController
                        .startCategorisation(testRecordId)
                        .url
                    )
                  )
                )
                .url

              withClue("must cleanse the user answers data") {
                verify(mockSessionRepository)
                  .clearData(eqTo(emptyUserAnswers.id), eqTo(CategorisationJourney))
              }
            }
          }

          "when this assessment cannot be found" in {

            val mockSessionRepository = mock[SessionRepository]
            when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

            val categorisationInfo =
              CategorisationInfo(
                "1234567890",
                "BV",
                Some(validityEndDate),
                Seq.empty[CategoryAssessment],
                Seq.empty[CategoryAssessment],
                None,
                1
              )
            val answers            =
              emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
              .build()

            running(application) {
              val request = FakeRequest(POST, assessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(
                  Some(
                    RedirectUrl(
                      controllers.categorisation.routes.CategorisationPreparationController
                        .startCategorisation(testRecordId)
                        .url
                    )
                  )
                )
                .url

              withClue("must cleanse the user answers data") {
                verify(mockSessionRepository)
                  .clearData(eqTo(emptyUserAnswers.id), eqTo(CategorisationJourney))
              }
            }
          }

          "when session repository fails" in {

            val mockSessionRepo = mock[SessionRepository]
            when(mockSessionRepo.set(any())).thenReturn(Future.failed(new Exception(":(")))
            when(mockSessionRepo.clearData(any(), any())).thenReturn(Future.successful(true))

            val answers =
              emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(
                bind[SessionRepository].toInstance(mockSessionRepo)
              )
              .build()

            running(application) {
              val checkedValues = List("none")

              val request = FakeRequest(POST, assessmentRoute).withFormUrlEncodedBody(
                checkedValues.flatMap(value => Seq("value[]" -> value)): _*
              )

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(
                  Some(
                    RedirectUrl(
                      controllers.categorisation.routes.CategorisationPreparationController
                        .startCategorisation(testRecordId)
                        .url
                    )
                  )
                )
                .url

              withClue("must cleanse the user answers data") {
                verify(mockSessionRepo).clearData(eqTo(emptyUserAnswers.id), eqTo(CategorisationJourney))
              }
            }
          }

        }

      }

    }

    "for longer commodity code reassessment" - {
      "onPageLoad" - {

        "must render the view when an assessment can be found for this id" - {

          "and has not previously been answered" in {

            val answers =
              emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(GET, reassessmentRoute)

              val result = route(application, request).value

              val onSubmitAction               = controllers.categorisation.routes.AssessmentController
                .onSubmitReassessment(NormalMode, testRecordId, Constants.firstAssessmentNumber)
              val view                         = application.injector.instanceOf[AssessmentView]
              val appConfig: FrontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

              val form                         = formProvider()
              val expectedCodesAndDescriptions =
                categorisationInfo.categoryAssessmentsThatNeedAnswers.head.exemptions.map { exemption =>
                  (exemption.code, exemption.description)
                }

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                Constants.firstAssessmentNumber,
                expectedCodesAndDescriptions,
                categorisationInfo.commodityCode,
                onSubmitAction,
                categorisationInfo.categoryAssessments.head.themeDescription,
                categorisationInfo.categoryAssessments.head.regulationUrl,
                isReassessment = true
              )(
                request,
                messages(application),
                appConfig
              ).toString
            }
          }

          "and has previously been answered" in {

            val answers =
              emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo)
                .success
                .value
                .set(ReassessmentPage(testRecordId, 0), ReassessmentAnswer(AssessmentAnswer.NoExemption))
                .success
                .value

            val application    = applicationBuilder(userAnswers = Some(answers)).build()
            val onSubmitAction =
              controllers.categorisation.routes.AssessmentController
                .onSubmitReassessment(NormalMode, testRecordId, Constants.firstAssessmentNumber)

            running(application) {
              val request = FakeRequest(GET, reassessmentRoute)

              val result = route(application, request).value

              val view                         = application.injector.instanceOf[AssessmentView]
              val appConfig: FrontendAppConfig = application.injector.instanceOf[FrontendAppConfig]
              val form                         = formProvider().fill(AssessmentAnswer.NoExemption)
              val expectedCodesAndDescriptions =
                categorisationInfo.categoryAssessmentsThatNeedAnswers.head.exemptions.map { exemption =>
                  (exemption.code, exemption.description)
                }

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                Constants.firstAssessmentNumber,
                expectedCodesAndDescriptions,
                categorisationInfo.commodityCode,
                onSubmitAction,
                categorisationInfo.categoryAssessments.head.themeDescription,
                categorisationInfo.categoryAssessments.head.regulationUrl,
                isReassessment = true
              )(
                request,
                messages(application),
                appConfig
              ).toString
            }
          }
        }

        "must redirect to Journey Recovery" - {

          "when categorisation information does not exist" in {

            val mockSessionRepository = mock[SessionRepository]
            when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
              .build()

            running(application) {
              val request = FakeRequest(GET, reassessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(
                  Some(
                    RedirectUrl(
                      controllers.categorisation.routes.CategorisationPreparationController
                        .startCategorisation(testRecordId)
                        .url
                    )
                  )
                )
                .url

              withClue("must cleanse the user answers data") {
                verify(mockSessionRepository)
                  .clearData(eqTo(emptyUserAnswers.id), eqTo(CategorisationJourney))
              }
            }
          }

          "when this assessment index cannot be found" in {

            val mockSessionRepository = mock[SessionRepository]
            when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

            val categorisationInfo =
              CategorisationInfo(
                "1234567890",
                "BV",
                Some(validityEndDate),
                Seq.empty[CategoryAssessment],
                Seq.empty[CategoryAssessment],
                None,
                1
              )
            val answers            =
              emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
              .build()

            running(application) {
              val request = FakeRequest(GET, reassessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(
                  Some(
                    RedirectUrl(
                      controllers.categorisation.routes.CategorisationPreparationController
                        .startCategorisation(testRecordId)
                        .url
                    )
                  )
                )
                .url

              withClue("must cleanse the user answers data") {
                verify(mockSessionRepository)
                  .clearData(eqTo(emptyUserAnswers.id), eqTo(CategorisationJourney))
              }

            }
          }
        }
      }

      "onSubmit" - {

        "must save the answer and redirect to the next page when a valid value is submitted" in {

          val mockRepository = mock[SessionRepository]
          when(mockRepository.set(any())).thenReturn(Future.successful(true))

          val answers =
            emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

          val application =
            applicationBuilder(userAnswers = Some(answers))
              .overrides(
                bind[SessionRepository].toInstance(mockRepository),
                bind[CategorisationNavigator].toInstance(new FakeCategorisationNavigator(onwardRoute))
              )
              .build()

          running(application) {
            val checkedValues = List("Y903", "Y256")

            val request = FakeRequest(POST, reassessmentRoute).withFormUrlEncodedBody(
              checkedValues.flatMap(value => Seq("value[]" -> value)): _*
            )

            val result = route(application, request).value

            val expectedAnswers =
              answers
                .set(
                  ReassessmentPage(testRecordId, 0),
                  ReassessmentAnswer(AssessmentAnswer.Exemption(Seq("Y903", "Y256")))
                )
                .success
                .value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
            verify(mockRepository).set(eqTo(expectedAnswers))
          }
        }

        "must return a Bad Request and errors when invalid data is submitted" in {

          val answers =
            emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(POST, reassessmentRoute).withFormUrlEncodedBody(("value", ""))

            val result = route(application, request).value

            val onSubmitAction =
              controllers.categorisation.routes.AssessmentController
                .onSubmitReassessment(NormalMode, testRecordId, Constants.firstAssessmentNumber)
            val view           = application.injector.instanceOf[AssessmentView]
            val form           = formProvider().fill(AssessmentAnswer.NoExemption)

            val boundForm                    = form.bind(Map("value" -> ""))
            val appConfig: FrontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

            val expectedCodesAndDescriptions =
              categorisationInfo.categoryAssessmentsThatNeedAnswers.head.exemptions.map { exemption =>
                (exemption.code, exemption.description)
              }

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(
              boundForm,
              NormalMode,
              testRecordId,
              Constants.firstAssessmentNumber,
              expectedCodesAndDescriptions,
              categorisationInfo.commodityCode,
              onSubmitAction,
              categorisationInfo.categoryAssessments.head.themeDescription,
              categorisationInfo.categoryAssessments.head.regulationUrl,
              isReassessment = true
            )(
              request,
              messages(application),
              appConfig
            ).toString
          }
        }

        "must redirect to Journey Recovery" - {

          "when categorisation information does not exist" in {

            val mockSessionRepository = mock[SessionRepository]
            when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
              .build()

            running(application) {
              val request = FakeRequest(POST, reassessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(
                  Some(
                    RedirectUrl(
                      controllers.categorisation.routes.CategorisationPreparationController
                        .startCategorisation(testRecordId)
                        .url
                    )
                  )
                )
                .url

              withClue("must cleanse the user answers data") {
                verify(mockSessionRepository)
                  .clearData(eqTo(emptyUserAnswers.id), eqTo(CategorisationJourney))
              }
            }
          }

          "when this assessment cannot be found" in {

            val mockSessionRepository = mock[SessionRepository]
            when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

            val categorisationInfo =
              CategorisationInfo(
                "1234567890",
                "BV",
                Some(validityEndDate),
                Seq.empty[CategoryAssessment],
                Seq.empty[CategoryAssessment],
                None,
                1
              )
            val answers            =
              emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
              .build()

            running(application) {
              val request = FakeRequest(POST, reassessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(
                  Some(
                    RedirectUrl(
                      controllers.categorisation.routes.CategorisationPreparationController
                        .startCategorisation(testRecordId)
                        .url
                    )
                  )
                )
                .url

              withClue("must cleanse the user answers data") {
                verify(mockSessionRepository)
                  .clearData(eqTo(emptyUserAnswers.id), eqTo(CategorisationJourney))
              }
            }
          }

          "when session repository fails" in {

            val mockSessionRepo = mock[SessionRepository]
            when(mockSessionRepo.set(any())).thenReturn(Future.failed(new Exception(":(")))
            when(mockSessionRepo.clearData(any(), any())).thenReturn(Future.successful(true))

            val answers =
              emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(
                bind[SessionRepository].toInstance(mockSessionRepo)
              )
              .build()

            running(application) {
              val checkedValues = List("none")

              val request = FakeRequest(POST, assessmentRoute).withFormUrlEncodedBody(
                checkedValues.flatMap(value => Seq("value[]" -> value)): _*
              )

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
                .onPageLoad(
                  Some(
                    RedirectUrl(
                      controllers.categorisation.routes.CategorisationPreparationController
                        .startCategorisation(testRecordId)
                        .url
                    )
                  )
                )
                .url

              withClue("must cleanse the user answers data") {
                verify(mockSessionRepo).clearData(eqTo(emptyUserAnswers.id), eqTo(CategorisationJourney))
              }

            }
          }

        }

      }

    }
  }

}
