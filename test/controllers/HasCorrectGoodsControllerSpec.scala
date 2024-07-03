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
import forms.HasCorrectGoodsFormProvider
import models.requests.DataRequest
import models.{Commodity, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{HasCorrectGoodsLongerCommodityCodePage, HasCorrectGoodsPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CommodityQuery, LongerCommodityQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import services.CategorisationService
import views.html.HasCorrectGoodsView

import java.time.Instant
import scala.concurrent.Future

class HasCorrectGoodsControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new HasCorrectGoodsFormProvider()
  private val form = formProvider()

  "HasCorrectGoodsController" - {

    "for the Create Journey" - {

      lazy val hasCorrectGoodsRoute = routes.HasCorrectGoodsController.onPageLoad(NormalMode).url
      lazy val onSubmitAction: Call = routes.HasCorrectGoodsController.onSubmit(NormalMode)

      "must return OK and the correct view for a GET" in {

        val userAnswers =
          emptyUserAnswers
            .set(CommodityQuery, Commodity("654321", List("Description", "Other"), Instant.now, None))
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            Commodity("654321", List("Description", "Other"), Instant.now, None),
            onSubmitAction
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect on GET to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val commodity   = Commodity("654321", List("Description"), Instant.now, None)
        val userAnswers = emptyUserAnswers
          .set(CommodityQuery, commodity)
          .success
          .value
          .set(HasCorrectGoodsPage, true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsRoute)

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), commodity, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must redirect on POST to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val commodity = Commodity("654321", List("Description"), Instant.now, None)

        val userAnswers =
          emptyUserAnswers.set(CommodityQuery, commodity).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, commodity, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "for the Longer Commodity Code Journey" - {

      lazy val hasCorrectGoodsRoute =
        routes.HasCorrectGoodsController.onPageLoadLongerCommodityCode(NormalMode, testRecordId).url
      lazy val onSubmitAction: Call =
        routes.HasCorrectGoodsController.onSubmitLongerCommodityCode(NormalMode, testRecordId)

      "for a GET" - {

        "must return OK and the correct view for a GET" in {

          val userAnswers =
            emptyUserAnswers
              .set(
                LongerCommodityQuery(testRecordId),
                Commodity("654321", List("Description", "Other"), Instant.now, None)
              )
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .build()

          running(application) {
            val request = FakeRequest(GET, hasCorrectGoodsRoute)

            val result = route(application, request).value

            val view = application.injector.instanceOf[HasCorrectGoodsView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              form,
              Commodity("654321", List("Description", "Other"), Instant.now, None),
              onSubmitAction
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect on GET to JourneyRecovery Page if user doesn't have commodity answer" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, hasCorrectGoodsRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must populate the view correctly on a GET when the question has previously been answered" in {

          val commodity   = Commodity("654321", List("Description"), Instant.now, None)
          val userAnswers = emptyUserAnswers
            .set(LongerCommodityQuery(testRecordId), commodity)
            .success
            .value
            .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, hasCorrectGoodsRoute)

            val view = application.injector.instanceOf[HasCorrectGoodsView]

            val result = route(application, request).value

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form.fill(true), commodity, onSubmitAction)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery for a GET if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, hasCorrectGoodsRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

      }

      "for a POST" - {
        "must redirect to the next page when valid data is submitted" in {

          val mockSessionRepository                                = mock[SessionRepository]
          val captorForEndUserAnswers: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
          when(mockSessionRepository.set(captorForEndUserAnswers.capture())) thenReturn Future.successful(true)

          val userAnswers = emptyUserAnswers
            .set(RecordCategorisationsQuery, recordCategorisations)
            .success
            .value

          val mockCategorisationService = mock[CategorisationService]
          when(mockCategorisationService.updateCategorisationWithNewCommodityCode(any(), any())(any()))
            .thenReturn(Future.successful(userAnswers))

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                bind[SessionRepository].toInstance(mockSessionRepository),
                bind[CategorisationService].toInstance(mockCategorisationService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, hasCorrectGoodsRoute)
                .withFormUrlEncodedBody(("value", "true"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            val endUserAnswers = captorForEndUserAnswers.getValue

// TODO would like to test these things are set if it still happens here.
//            withClue("should have set longCommodityCodePage") {
//              endUserAnswers.get(HasCorrectGoodsLongerCommodityCodePage(testRecordId)) mustBe Some(true)
//            }

          }
        }

        "must redirect on POST to JourneyRecovery Page if user doesn't have commodity answer" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request =
              FakeRequest(POST, hasCorrectGoodsRoute)
                .withFormUrlEncodedBody(("value", ""))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must return a Bad Request and errors when invalid data is submitted" in {

          val commodity = Commodity("654321", List("Description"), Instant.now, None)

          val userAnswers =
            emptyUserAnswers.set(LongerCommodityQuery(testRecordId), commodity).success.value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request =
              FakeRequest(POST, hasCorrectGoodsRoute)
                .withFormUrlEncodedBody(("value", ""))

            val boundForm = form.bind(Map("value" -> ""))

            val view = application.injector.instanceOf[HasCorrectGoodsView]

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(boundForm, commodity, onSubmitAction)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery for a POST if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request =
              FakeRequest(POST, hasCorrectGoodsRoute)
                .withFormUrlEncodedBody(("value", "true"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

      }

    }

  }
}
