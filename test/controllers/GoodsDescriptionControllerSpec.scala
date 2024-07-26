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
import base.TestConstants.{testRecordId, userAnswersId}
import connectors.TraderProfileConnector
import forms.GoodsDescriptionFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.{GoodsDescriptionPage, GoodsDescriptionUpdatePage}
import play.api.inject.bind
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import utils.SessionData.{dataUpdated, pageUpdated}
import views.html.GoodsDescriptionView

import scala.concurrent.Future

class GoodsDescriptionControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new GoodsDescriptionFormProvider()
  private val form = formProvider()

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

  "GoodsDescription Controller" - {

    ".create journey" - {

      lazy val goodsDescriptionCreateRoute = routes.GoodsDescriptionController.onPageLoadCreate(NormalMode).url
      lazy val onSubmitAction: Call        = routes.GoodsDescriptionController.onSubmitCreate(NormalMode)

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, goodsDescriptionCreateRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[GoodsDescriptionView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId).set(GoodsDescriptionPage, "answer").success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, goodsDescriptionCreateRoute)

          val view = application.injector.instanceOf[GoodsDescriptionView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("answer"), NormalMode, onSubmitAction)(
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
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
            )
            .build()

        val length              = 512
        val description: String = Gen.listOfN(length, Gen.alphaNumChar).map(_.mkString).sample.value

        running(application) {
          val request =
            FakeRequest(POST, goodsDescriptionCreateRoute)
              .withFormUrlEncodedBody(("value", description))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when no description is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, goodsDescriptionCreateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[GoodsDescriptionView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must return a Bad Request and errors when user submits a description longer than 512 characters" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        val invalidLength              = 513
        val invalidDescription: String = Gen.listOfN(invalidLength, Gen.alphaNumChar).map(_.mkString).sample.value

        running(application) {
          val request =
            FakeRequest(POST, goodsDescriptionCreateRoute)
              .withFormUrlEncodedBody(("value", invalidDescription))

          val boundForm = form.bind(Map("value" -> invalidDescription))

          val view = application.injector.instanceOf[GoodsDescriptionView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, goodsDescriptionCreateRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, goodsDescriptionCreateRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    ".update journey" - {

      lazy val goodsDescriptionUpdateRoute =
        routes.GoodsDescriptionController.onPageLoadUpdate(NormalMode, testRecordId).url
      lazy val onSubmitAction: Call        = routes.GoodsDescriptionController.onSubmitUpdate(NormalMode, testRecordId)

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, goodsDescriptionUpdateRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[GoodsDescriptionView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers =
          UserAnswers(userAnswersId).set(GoodsDescriptionUpdatePage(testRecordId), "answer").success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, goodsDescriptionUpdateRoute)

          val view = application.injector.instanceOf[GoodsDescriptionView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("answer"), NormalMode, onSubmitAction)(
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

        val length              = 512
        val description: String = Gen.listOfN(length, Gen.alphaNumChar).map(_.mkString).sample.value

        running(application) {
          val request =
            FakeRequest(POST, goodsDescriptionUpdateRoute)
              .withFormUrlEncodedBody(("value", description))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must set changesMade to true if goods description is updated" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val userAnswers =
          UserAnswers(userAnswersId).set(GoodsDescriptionUpdatePage(testRecordId), "oldValue").success.value

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val controller = application.injector.instanceOf[GoodsDescriptionController]
          val request    =
            FakeRequest(POST, goodsDescriptionUpdateRoute)
              .withFormUrlEncodedBody(("value", "newValue"))

          val result: Future[Result] = controller.onSubmitUpdate(NormalMode, testRecordId)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          session(result).get(dataUpdated) must be(Some("true"))
          session(result).get(pageUpdated) must be(Some("goods description"))
        }
      }

      "must set changesMade to false if goods description is not updated" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val userAnswers =
          UserAnswers(userAnswersId).set(GoodsDescriptionUpdatePage(testRecordId), "OldValue").success.value

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val controller = application.injector.instanceOf[GoodsDescriptionController]
          val request    =
            FakeRequest(POST, goodsDescriptionUpdateRoute)
              .withFormUrlEncodedBody(("value", "OldValue"))

          val result: Future[Result] = controller.onSubmitUpdate(NormalMode, testRecordId)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          session(result).get(dataUpdated) must be(Some("false"))
        }
      }

      "must return a Bad Request and errors when no description is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, goodsDescriptionUpdateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[GoodsDescriptionView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must return a Bad Request and errors when user submits a description longer than 512 characters" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        val invalidLength              = 513
        val invalidDescription: String = Gen.listOfN(invalidLength, Gen.alphaNumChar).map(_.mkString).sample.value

        running(application) {
          val request =
            FakeRequest(POST, goodsDescriptionUpdateRoute)
              .withFormUrlEncodedBody(("value", invalidDescription))

          val boundForm = form.bind(Map("value" -> invalidDescription))

          val view = application.injector.instanceOf[GoodsDescriptionView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, onSubmitAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, goodsDescriptionUpdateRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, goodsDescriptionUpdateRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
