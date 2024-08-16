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

package controllers

import base.SpecBase
import base.TestConstants.testRecordId
import forms.AssessmentFormProvider2
import models.ott.{CategorisationInfo2, CategoryAssessment}
import models.{AssessmentAnswer2, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{AssessmentPage2, ReassessmentPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CategorisationDetailsQuery2, LongerCategorisationDetailsQuery}
import repositories.SessionRepository
import views.html.AssessmentView2

import scala.concurrent.Future

class AssessmentControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private def onwardRoute      = Call("GET", "/foo")
  private val formProvider2    = new AssessmentFormProvider2()
  private def assessmentRoute2 = routes.AssessmentController.onPageLoad2(NormalMode, testRecordId, 0).url

  private def reassessmentRoute = routes.AssessmentController.onPageLoadReassessment(NormalMode, testRecordId, 0).url

  "AssessmentController 2" - {

    "for initial categorisation" - {
      "onPageLoad" - {

        "must render the view when an assessment can be found for this id" - {

          "and has not previously been answered" in {

            val answers =
              emptyUserAnswers.set(CategorisationDetailsQuery2(testRecordId), categorisationInfo2).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute2)

              val result = route(application, request).value

              val onSubmitAction    = routes.AssessmentController.onSubmit2(NormalMode, testRecordId, 0)
              val view              = application.injector.instanceOf[AssessmentView2]
              val form              = formProvider2(1)
              val expectedListItems = categorisationInfo2.categoryAssessmentsThatNeedAnswers.head.exemptions.map {
                exemption =>
                  exemption.code + " - " + exemption.description
              }

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                0,
                expectedListItems,
                categorisationInfo2.commodityCode,
                onSubmitAction
              )(
                request,
                messages(application)
              ).toString
            }
          }

          "and has previously been answered" in {

            val answers =
              emptyUserAnswers
                .set(CategorisationDetailsQuery2(testRecordId), categorisationInfo2)
                .success
                .value
                .set(AssessmentPage2(testRecordId, 0), AssessmentAnswer2.NoExemption)
                .success
                .value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute2)

              val result = route(application, request).value

              val onSubmitAction    = routes.AssessmentController.onSubmit2(NormalMode, testRecordId, 0)
              val view              = application.injector.instanceOf[AssessmentView2]
              val form              = formProvider2(1).fill(AssessmentAnswer2.NoExemption)
              val expectedListItems = categorisationInfo2.categoryAssessmentsThatNeedAnswers.head.exemptions.map {
                exemption =>
                  exemption.code + " - " + exemption.description
              }

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                0,
                expectedListItems,
                categorisationInfo2.commodityCode,
                onSubmitAction
              )(
                request,
                messages(application)
              ).toString
            }
          }
        }

        "must redirect to Journey Recovery" - {

          "when categorisation information does not exist" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute2)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "when this assessment index cannot be found" in {

            val categorisationInfo =
              CategorisationInfo2("1234567890", Seq.empty[CategoryAssessment], Seq.empty[CategoryAssessment], None, 1)
            val answers            =
              emptyUserAnswers.set(CategorisationDetailsQuery2(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(GET, assessmentRoute2)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }
        }
      }

      "onSubmit" - {

        "must save the answer and redirect to the next page when a valid value is submitted" in {

          val mockRepository = mock[SessionRepository]
          when(mockRepository.set(any())).thenReturn(Future.successful(true))

          val answers =
            emptyUserAnswers.set(CategorisationDetailsQuery2(testRecordId), categorisationInfo2).success.value

          val application =
            applicationBuilder(userAnswers = Some(answers))
              .overrides(
                bind[SessionRepository].toInstance(mockRepository),
                bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, assessmentRoute2).withFormUrlEncodedBody(("value", "false"))

            val result = route(application, request).value

            val expectedAnswers =
              answers.set(AssessmentPage2(testRecordId, 0), AssessmentAnswer2.NoExemption).success.value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
            verify(mockRepository).set(eqTo(expectedAnswers))
          }
        }

        "must return a Bad Request and errors when invalid data is submitted" in {

          val answers =
            emptyUserAnswers.set(CategorisationDetailsQuery2(testRecordId), categorisationInfo2).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(POST, assessmentRoute2).withFormUrlEncodedBody(("value", ""))

            val result = route(application, request).value

            val onSubmitAction    = routes.AssessmentController.onSubmit2(NormalMode, testRecordId, 0)
            val view              = application.injector.instanceOf[AssessmentView2]
            val form              = formProvider2(1).fill(AssessmentAnswer2.NoExemption)
            val boundForm         = form.bind(Map("value" -> ""))
            val expectedListItems = categorisationInfo2.categoryAssessmentsThatNeedAnswers.head.exemptions.map {
              exemption =>
                exemption.code + " - " + exemption.description
            }

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(
              boundForm,
              NormalMode,
              testRecordId,
              0,
              expectedListItems,
              categorisationInfo2.commodityCode,
              onSubmitAction
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery" - {

          "when categorisation information does not exist" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

            running(application) {
              val request = FakeRequest(POST, assessmentRoute2)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "when this assessment cannot be found" in {

            val categorisationInfo =
              CategorisationInfo2("1234567890", Seq.empty[CategoryAssessment], Seq.empty[CategoryAssessment], None, 1)
            val answers            =
              emptyUserAnswers.set(CategorisationDetailsQuery2(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(POST, assessmentRoute2)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "when session repository fails" in {

            val mockSessionRepo = mock[SessionRepository]
            when(mockSessionRepo.set(any())).thenReturn(Future.failed(new Exception(":(")))

            val answers =
              emptyUserAnswers.set(CategorisationDetailsQuery2(testRecordId), categorisationInfo2).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(
                bind[SessionRepository].toInstance(mockSessionRepo)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, assessmentRoute2).withFormUrlEncodedBody(("value", "false"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
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
              emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo2).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(GET, reassessmentRoute)

              val result = route(application, request).value

              val onSubmitAction    = routes.AssessmentController.onSubmitReassessment(NormalMode, testRecordId, 0)
              val view              = application.injector.instanceOf[AssessmentView2]
              val form              = formProvider2(1)
              val expectedListItems = categorisationInfo2.categoryAssessmentsThatNeedAnswers.head.exemptions.map {
                exemption =>
                  exemption.code + " - " + exemption.description
              }

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                0,
                expectedListItems,
                categorisationInfo2.commodityCode,
                onSubmitAction
              )(
                request,
                messages(application)
              ).toString
            }
          }

          "and has previously been answered" in {

            val answers =
              emptyUserAnswers
                .set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo2)
                .success
                .value
                .set(ReassessmentPage(testRecordId, 0), AssessmentAnswer2.NoExemption)
                .success
                .value

            val application    = applicationBuilder(userAnswers = Some(answers)).build()
            val onSubmitAction = routes.AssessmentController.onSubmitReassessment(NormalMode, testRecordId, 0)

            running(application) {
              val request = FakeRequest(GET, reassessmentRoute)

              val result = route(application, request).value

              val view              = application.injector.instanceOf[AssessmentView2]
              val form              = formProvider2(1).fill(AssessmentAnswer2.NoExemption)
              val expectedListItems = categorisationInfo2.categoryAssessmentsThatNeedAnswers.head.exemptions.map {
                exemption =>
                  exemption.code + " - " + exemption.description
              }

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                form,
                NormalMode,
                testRecordId,
                0,
                expectedListItems,
                categorisationInfo2.commodityCode,
                onSubmitAction
              )(
                request,
                messages(application)
              ).toString
            }
          }
        }

        "must redirect to Journey Recovery" - {

          "when categorisation information does not exist" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

            running(application) {
              val request = FakeRequest(GET, reassessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "when this assessment index cannot be found" in {

            val categorisationInfo =
              CategorisationInfo2("1234567890", Seq.empty[CategoryAssessment], Seq.empty[CategoryAssessment], None, 1)
            val answers            =
              emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(GET, reassessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }
        }
      }

      "onSubmit" - {

        "must save the answer and redirect to the next page when a valid value is submitted" in {

          val mockRepository = mock[SessionRepository]
          when(mockRepository.set(any())).thenReturn(Future.successful(true))

          val answers =
            emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo2).success.value

          val application =
            applicationBuilder(userAnswers = Some(answers))
              .overrides(
                bind[SessionRepository].toInstance(mockRepository),
                bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
              )
              .build()

          running(application) {
            val request = FakeRequest(POST, reassessmentRoute).withFormUrlEncodedBody(("value", "false"))

            val result = route(application, request).value

            val expectedAnswers =
              answers.set(ReassessmentPage(testRecordId, 0), AssessmentAnswer2.NoExemption).success.value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
            verify(mockRepository).set(eqTo(expectedAnswers))
          }
        }

        "must return a Bad Request and errors when invalid data is submitted" in {

          val answers =
            emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo2).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(POST, reassessmentRoute).withFormUrlEncodedBody(("value", ""))

            val result = route(application, request).value

            val onSubmitAction    = routes.AssessmentController.onSubmitReassessment(NormalMode, testRecordId, 0)
            val view              = application.injector.instanceOf[AssessmentView2]
            val form              = formProvider2(1).fill(AssessmentAnswer2.NoExemption)
            val boundForm         = form.bind(Map("value" -> ""))
            val expectedListItems = categorisationInfo2.categoryAssessmentsThatNeedAnswers.head.exemptions.map {
              exemption =>
                exemption.code + " - " + exemption.description
            }

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(
              boundForm,
              NormalMode,
              testRecordId,
              0,
              expectedListItems,
              categorisationInfo2.commodityCode,
              onSubmitAction
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery" - {

          "when categorisation information does not exist" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

            running(application) {
              val request = FakeRequest(POST, reassessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "when this assessment cannot be found" in {

            val categorisationInfo =
              CategorisationInfo2("1234567890", Seq.empty[CategoryAssessment], Seq.empty[CategoryAssessment], None, 1)
            val answers            =
              emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(POST, reassessmentRoute)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

          "when session repository fails" in {

            val mockSessionRepo = mock[SessionRepository]
            when(mockSessionRepo.set(any())).thenReturn(Future.failed(new Exception(":(")))

            val answers =
              emptyUserAnswers.set(LongerCategorisationDetailsQuery(testRecordId), categorisationInfo2).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(
                bind[SessionRepository].toInstance(mockSessionRepo)
              )
              .build()

            running(application) {
              val request = FakeRequest(POST, reassessmentRoute).withFormUrlEncodedBody(("value", "false"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

        }

      }

    }
  }

}
