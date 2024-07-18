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
import connectors.TraderProfileConnector
import forms.AssessmentFormProvider
import models.ott.{CategorisationInfo, CategoryAssessment, Certificate, OtherExemption}
import models.{AssessmentAnswer, NormalMode, RecordCategorisations, TraderProfile, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.AssessmentPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.RecordCategorisationsQuery
import repositories.SessionRepository
import services.CategorisationService
import utils.Constants.niphlsAssessment
import viewmodels.AssessmentViewModel
import views.html.AssessmentView

import scala.concurrent.Future

class AssessmentControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private def onwardRoute     = Call("GET", "/foo")
  private val formProvider    = new AssessmentFormProvider()
  private def assessmentId    = "321"
  private def recordId        = "1"
  private def index           = 0
  private def assessmentRoute = routes.AssessmentController.onPageLoad(NormalMode, recordId, index).url

  private val mockTraderProfileConnector = mock[TraderProfileConnector]
  private val fakeNavigator              = new FakeNavigator(onwardRoute)

  override def beforeEach(): Unit =
    when(mockTraderProfileConnector.getTraderProfile(any())(any())).thenReturn(
      Future.successful(
        TraderProfile("actorId", "ukimsNumber", None, None)
      )
    )

  "AssessmentController" - {

    "for a GET" - {

      "must redirect" - {

        "if there's a category 2 assessment without possible exemptions" in {

          val assessmentCat2NoExemptions = CategoryAssessment(assessmentId, 2, Seq())
          val categorisationInfo         =
            CategorisationInfo("123", Seq(assessmentCat2NoExemptions), Some("Weight, in kilograms"), 0)
          val recordCategorisations      = RecordCategorisations(records = Map(recordId -> categorisationInfo))

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value

          val mockCategorisationService = mock[CategorisationService]
          when(mockCategorisationService.requireCategorisation(any(), any())(any()))
            .thenReturn(Future.successful(answers))
          val application               = applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[CategorisationService].to(mockCategorisationService),
              bind[TraderProfileConnector].to(mockTraderProfileConnector),
              bind[Navigator].to(fakeNavigator)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
          }
        }

        "if user has niphls and the assessment question has niphls as an answer" in {
          val assessmentNiphls      =
            CategoryAssessment(assessmentId, 1, Seq(OtherExemption(niphlsAssessment, niphlsAssessment, "niphls")))
          val categorisationInfo    =
            CategorisationInfo("123", Seq(assessmentNiphls), Some("Weight, in kilograms"), 0)
          val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value

          val mockCategorisationService = mock[CategorisationService]
          when(mockCategorisationService.requireCategorisation(any(), any())(any()))
            .thenReturn(Future.successful(answers))

          when(mockTraderProfileConnector.getTraderProfile(any())(any())).thenReturn(
            Future.successful(TraderProfile("actorId", "ukimsNumber", None, Some("niphlsNo")))
          )

          val mockSessionRepository                  = mock[SessionRepository]
          val argCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
          when(mockSessionRepository.set(argCaptor.capture())).thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[CategorisationService].to(mockCategorisationService),
              bind[TraderProfileConnector].to(mockTraderProfileConnector),
              bind[SessionRepository].to(mockSessionRepository),
              bind[Navigator].to(fakeNavigator)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            withClue("Should have set the value of the assessment to Yes since they have NIPHLS") {
              val resultingUA = argCaptor.getValue
              resultingUA.get(AssessmentPage(recordId, index)) mustBe Some(AssessmentAnswer.Exemption("true"))
            }
          }

        }

        "if user has niphls and the assessment question is a category 2 niphls empty question" in {
          val assessmentNiphls      =
            CategoryAssessment(assessmentId, 1, Seq(OtherExemption(niphlsAssessment, niphlsAssessment, "niphls")))
          val assessmentEmptyNiphls =
            CategoryAssessment("assId2", 2, Seq.empty)
          val categorisationInfo    =
            CategorisationInfo("123", Seq(assessmentNiphls, assessmentEmptyNiphls), Some("Weight, in kilograms"), 0)
          val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
              .set(AssessmentPage(recordId, index), AssessmentAnswer.Exemption("true"))
              .success
              .value

          val mockCategorisationService = mock[CategorisationService]
          when(mockCategorisationService.requireCategorisation(any(), any())(any()))
            .thenReturn(Future.successful(answers))

          when(mockTraderProfileConnector.getTraderProfile(any())(any())).thenReturn(
            Future.successful(TraderProfile("actorId", "ukimsNumber", None, Some("niphlsNo")))
          )

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[CategorisationService].to(mockCategorisationService),
              bind[TraderProfileConnector].to(mockTraderProfileConnector),
              bind[Navigator].to(fakeNavigator)
            )
            .build()

          running(application) {
            val assessmentRoute = routes.AssessmentController.onPageLoad(NormalMode, recordId, index + 1).url
            val request         = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

          }

        }

      }

      "must render the view when an assessment can be found for this id" - {

        "and has not previously been answered" in {

          val assessment            = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
          val categorisationInfo    = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
          val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))
          val answers               = emptyUserAnswers.set(RecordCategorisationsQuery, recordCategorisations).success.value

          val mockCategorisationService = mock[CategorisationService]
          when(mockCategorisationService.requireCategorisation(any(), any())(any()))
            .thenReturn(Future.successful(answers))
          val application               = applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[CategorisationService].to(mockCategorisationService),
              bind[TraderProfileConnector].to(mockTraderProfileConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            val view      = application.injector.instanceOf[AssessmentView]
            val form      = formProvider(1)
            val listItems = assessment.exemptions.map { exemption =>
              exemption.code + " - " + exemption.description
            }

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, recordId, index, listItems, "123")(
              request,
              messages(application)
            ).toString
          }
        }

        "and has previously been answered" in {

          val assessment            = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
          val categorisationInfo    = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
          val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value
              .set(AssessmentPage(recordId, index), AssessmentAnswer.NoExemption)
              .success
              .value

          val mockCategorisationService = mock[CategorisationService]
          when(mockCategorisationService.requireCategorisation(any(), any())(any()))
            .thenReturn(Future.successful(answers))
          val application               = applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[CategorisationService].to(mockCategorisationService),
              bind[TraderProfileConnector].to(mockTraderProfileConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            val view      = application.injector.instanceOf[AssessmentView]
            val form      = formProvider(1).fill(AssessmentAnswer.NoExemption)
            val listItems = assessment.exemptions.map { exemption =>
              exemption.code + " - " + exemption.description
            }

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, recordId, index, listItems, "123")(
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
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when this assessment index cannot be found" in {

          val categorisationInfo    = CategorisationInfo("123", Nil, Some("Weight, in kilograms"), 0)
          val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))
          val answers               = emptyUserAnswers.set(RecordCategorisationsQuery, recordCategorisations).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when trader profile is not found" in {

          when(mockTraderProfileConnector.getTraderProfile(any())(any()))
            .thenReturn(Future.failed(new RuntimeException("")))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when it is a niphls question and they don't have niphls, because they should not get this far in the journey" in {

          val assessmentNiphls      =
            CategoryAssessment(assessmentId, 1, Seq(OtherExemption(niphlsAssessment, niphlsAssessment, "niphls")))
          val categorisationInfo    =
            CategorisationInfo("123", Seq(assessmentNiphls), Some("Weight, in kilograms"), 0)
          val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value

          val mockCategorisationService = mock[CategorisationService]
          when(mockCategorisationService.requireCategorisation(any(), any())(any()))
            .thenReturn(Future.successful(answers))

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[CategorisationService].to(mockCategorisationService),
              bind[TraderProfileConnector].to(mockTraderProfileConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

      }
    }

    "for a POST" - {

      "must save the answer and redirect to the next page when a valid value is submitted" in {

        val assessment            = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
        val categorisationInfo    = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
        val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))

        val mockRepository = mock[SessionRepository]
        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        val answers = emptyUserAnswers.set(RecordCategorisationsQuery, recordCategorisations).success.value

        val application =
          applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[SessionRepository].toInstance(mockRepository),
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
            )
            .build()

        running(application) {
          val request = FakeRequest(POST, assessmentRoute).withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          val expectedAnswers = answers.set(AssessmentPage(recordId, index), AssessmentAnswer.NoExemption).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          verify(mockRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val assessment            = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
        val categorisationInfo    = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
        val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))

        val answers = emptyUserAnswers.set(RecordCategorisationsQuery, recordCategorisations).success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {
          val request = FakeRequest(POST, assessmentRoute).withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          val view      = application.injector.instanceOf[AssessmentView]
          val form      = formProvider(1).fill(AssessmentAnswer.NoExemption)
          val boundForm = form.bind(Map("value" -> ""))
          val listItems = assessment.exemptions.map { exemption =>
            exemption.code + " - " + exemption.description
          }

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, recordId, index, listItems, "123")(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery" - {

        "when categorisation information does not exist" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(POST, assessmentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "when this assessment cannot be found" in {

          val answers = emptyUserAnswers.set(RecordCategorisationsQuery, recordCategorisations).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(POST, assessmentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }
    }
  }
}
