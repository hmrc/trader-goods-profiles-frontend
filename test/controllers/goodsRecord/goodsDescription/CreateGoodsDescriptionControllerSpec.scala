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

package controllers.goodsRecord.goodsDescription

import base.SpecBase
import base.TestConstants.userAnswersId
import forms.goodsRecord.GoodsDescriptionFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeGoodsRecordNavigator, GoodsRecordNavigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsRecord.GoodsDescriptionPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.goodsRecord.GoodsDescriptionView

import scala.concurrent.Future

class CreateGoodsDescriptionControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new GoodsDescriptionFormProvider()
  private val form = formProvider()

  "CreateGoodsDescriptionController" - {

    lazy val goodsDescriptionCreateRoute =
      controllers.goodsRecord.goodsDescription.routes.CreateGoodsDescriptionController.onPageLoad(NormalMode).url
    lazy val onSubmitAction: Call        =
      controllers.goodsRecord.goodsDescription.routes.CreateGoodsDescriptionController.onSubmit(NormalMode)

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
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
            bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
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
        .build()

      running(application) {
        val request = FakeRequest(GET, goodsDescriptionCreateRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
        val request =
          FakeRequest(POST, goodsDescriptionCreateRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
