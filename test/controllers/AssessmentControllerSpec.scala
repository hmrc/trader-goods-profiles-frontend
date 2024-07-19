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
import forms.AssessmentFormProvider
import models.ott.{CategorisationInfo, CategoryAssessment, Certificate}
import models.{AssessmentAnswer, NormalMode, RecordCategorisations}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.AssessmentPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{RecategorisingQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import viewmodels.AssessmentViewModel
import views.html.AssessmentView

import scala.concurrent.Future

class AssessmentControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute     = Call("GET", "/foo")
  private val formProvider    = new AssessmentFormProvider()
  private def assessmentId    = "321"
  private def recordId        = testRecordId
  private def index           = 0
  private def assessmentRoute = routes.AssessmentController.onPageLoad(NormalMode, recordId, index).url

  "AssessmentController" - {

    "for a GET" - {

      "must redirect" - {

        "to CyaCategorisation if there's a category 2 assessment without possible exemptions" in {

          val assessmentCat2NoExemptions = CategoryAssessment(assessmentId, 2, Seq())
          val categorisationInfo         =
            CategorisationInfo("123", Seq(assessmentCat2NoExemptions), Some("Weight, in kilograms"), 0)
          val recordCategorisations      = RecordCategorisations(records = Map(recordId -> categorisationInfo))

          val answers =
            emptyUserAnswers
              .set(RecordCategorisationsQuery, recordCategorisations)
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.CyaCategorisationController.onPageLoad(recordId).url
          }
        }

        "when recategorising and has previously been answered" in {

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
              .set(RecategorisingQuery(recordId), true)
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
            .build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

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

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            val view         = application.injector.instanceOf[AssessmentView]
            val form         = formProvider(Seq(assessmentId))
            val radioOptions = AssessmentAnswer.radioOptions(assessment.exemptions)(messages(application))
            val viewModel    = AssessmentViewModel(
              commodityCode = categorisationInfo.commodityCode,
              numberOfThisAssessment = 1,
              numberOfAssessments = categorisationInfo.categoryAssessments.size,
              radioOptions = radioOptions
            )

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, recordId, index, viewModel)(
              request,
              messages(application)
            ).toString
          }
        }

        "when recategorising and has not previously been answered" in {

          val assessment            = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
          val categorisationInfo    = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
          val recordCategorisations = RecordCategorisations(records = Map(recordId -> categorisationInfo))
          val answers               = emptyUserAnswers
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value
            .set(RecategorisingQuery(testRecordId), true)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            val view         = application.injector.instanceOf[AssessmentView]
            val form         = formProvider(Seq(assessmentId))
            val radioOptions = AssessmentAnswer.radioOptions(assessment.exemptions)(messages(application))
            val viewModel    = AssessmentViewModel(
              commodityCode = categorisationInfo.commodityCode,
              numberOfThisAssessment = 1,
              numberOfAssessments = categorisationInfo.categoryAssessments.size,
              radioOptions = radioOptions
            )

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, recordId, index, viewModel)(
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

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            val view         = application.injector.instanceOf[AssessmentView]
            val form         = formProvider(Seq(assessmentId)).fill(AssessmentAnswer.NoExemption)
            val radioOptions = AssessmentAnswer.radioOptions(assessment.exemptions)(messages(application))
            val viewModel    = AssessmentViewModel(
              commodityCode = categorisationInfo.commodityCode,
              numberOfThisAssessment = 1,
              numberOfAssessments = categorisationInfo.categoryAssessments.size,
              radioOptions = radioOptions
            )

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, recordId, index, viewModel)(
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
          val request = FakeRequest(POST, assessmentRoute).withFormUrlEncodedBody(("value", "none"))

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
          val request = FakeRequest(POST, assessmentRoute).withFormUrlEncodedBody(("value", "invalid value"))

          val result = route(application, request).value

          val view         = application.injector.instanceOf[AssessmentView]
          val form         = formProvider(Seq(assessmentId)).fill(AssessmentAnswer.NoExemption)
          val boundForm    = form.bind(Map("value" -> "invalid value"))
          val radioOptions = AssessmentAnswer.radioOptions(assessment.exemptions)(messages(application))
          val viewModel    = AssessmentViewModel(
            commodityCode = categorisationInfo.commodityCode,
            numberOfThisAssessment = 1,
            numberOfAssessments = categorisationInfo.categoryAssessments.size,
            radioOptions = radioOptions
          )

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, recordId, index, viewModel)(
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

          val application     = applicationBuilder(userAnswers = Some(answers)).build()
          val assessmentRoute = routes.AssessmentController.onPageLoad(NormalMode, "differentRecordId", index).url

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
