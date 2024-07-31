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
import models.{AssessmentAnswer, NormalMode}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.AssessmentPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CategorisationDetailsQuery, RecategorisingQuery}
import repositories.SessionRepository
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

        "to LongerCommodityCode if there's a cat 2 assessment without exemptions, the commodity code's short, and there are descendants" in {

          val commodityCode              = "1234560"
          val descendantCount            = 1
          val assessmentCat2NoExemptions = CategoryAssessment(assessmentId, 2, Seq())
          val categorisationInfo         =
            CategorisationInfo(
              commodityCode,
              Seq(assessmentCat2NoExemptions),
              Some("Weight, in kilograms"),
              descendantCount
            )

          val answers =
            emptyUserAnswers
              .set(CategorisationDetailsQuery(recordId), categorisationInfo)
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.LongerCommodityCodeController
              .onPageLoad(NormalMode, recordId)
              .url
          }
        }

        "to CyaCategorisation if there's a cat 2 assessment without exemptions and the user's not being redirected to LongerCommodityCode" in {

          val scenarios = Seq(
            ("12345600", 0), // short comcode, no descendants
            ("1234567800", 0), // long comcode, no descendants
            ("1234567800", 1) // long comcode, with descendants
          )

          scenarios.map { scenario =>
            val commodityCode              = scenario._1
            val descendantCount            = scenario._2
            val assessmentCat2NoExemptions = CategoryAssessment(assessmentId, 2, Seq())
            val categorisationInfo         =
              CategorisationInfo(
                commodityCode,
                Seq(assessmentCat2NoExemptions),
                Some("Weight, in kilograms"),
                descendantCount
              )

            val answers =
              emptyUserAnswers
                .set(CategorisationDetailsQuery(recordId), categorisationInfo)
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
        }

        "when recategorising and has previously been answered" in {

          val assessment         = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
          val categorisationInfo = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)

          val answers =
            emptyUserAnswers
              .set(CategorisationDetailsQuery(recordId), categorisationInfo)
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

          val assessment         = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
          val categorisationInfo = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
          val answers            = emptyUserAnswers.set(CategorisationDetailsQuery(recordId), categorisationInfo).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

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

        "when recategorising and has not previously been answered" in {

          val assessment         = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
          val categorisationInfo = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)
          val answers            = emptyUserAnswers
            .set(CategorisationDetailsQuery(recordId), categorisationInfo)
            .success
            .value
            .set(RecategorisingQuery(testRecordId), true)
            .success
            .value
          val listItems          = assessment.exemptions.map { exemption =>
            exemption.code + " - " + exemption.description
          }

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {
            val request = FakeRequest(GET, assessmentRoute)

            val result = route(application, request).value

            val view = application.injector.instanceOf[AssessmentView]
            val form = formProvider(1)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              form,
              NormalMode,
              recordId,
              index,
              listItems,
              categorisationInfo.commodityCode
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "and has previously been answered" in {

          val assessment         = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
          val categorisationInfo = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)

          val answers =
            emptyUserAnswers
              .set(CategorisationDetailsQuery(recordId), categorisationInfo)
              .success
              .value
              .set(AssessmentPage(recordId, index), AssessmentAnswer.NoExemption)
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

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

          val categorisationInfo = CategorisationInfo("123", Nil, Some("Weight, in kilograms"), 0)
          val answers            = emptyUserAnswers.set(CategorisationDetailsQuery(recordId), categorisationInfo).success.value

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

        val assessment         = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
        val categorisationInfo = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)

        val mockRepository = mock[SessionRepository]
        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        val answers = emptyUserAnswers.set(CategorisationDetailsQuery(recordId), categorisationInfo).success.value

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
          verify(mockRepository).set(eqTo(expectedAnswers))
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val assessment         = CategoryAssessment(assessmentId, 1, Seq(Certificate("1", "code", "description")))
        val categorisationInfo = CategorisationInfo("123", Seq(assessment), Some("Weight, in kilograms"), 0)

        val answers = emptyUserAnswers.set(CategorisationDetailsQuery(recordId), categorisationInfo).success.value

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

          val answers = emptyUserAnswers.set(CategorisationDetailsQuery(recordId), categorisationInfo).success.value

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
