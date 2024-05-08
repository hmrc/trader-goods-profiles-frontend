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
import cats.data.EitherT
import controllers.actions.FakeAuthoriseAction
import forms.CountryOfOriginFormProvider
import models.UserAnswers
import models.errors.SessionError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.CountryOfOriginView

import scala.concurrent.Future

class CountryOfOriginControllerSpec extends SpecBase {

  private val formProvider        = new CountryOfOriginFormProvider()
  private val fieldName           = "countryOfOrigin"
  private val countryOfOriginView = app.injector.instanceOf[CountryOfOriginView]

  private val countryOfOriginController = new CountryOfOriginController(
    messageComponentControllers,
    new FakeAuthoriseAction(defaultBodyParser),
    countryOfOriginView,
    formProvider,
    emptySessionRequest,
    sessionService
  )

  "Country Of Origin Controller" - {

    "must return OK and the empty view for a GET" in {
      val result = countryOfOriginController.onPageLoad(fakeRequest)

      status(result) mustEqual OK
      contentAsString(result) mustEqual countryOfOriginView(formProvider())(fakeRequest, messages).toString
    }

    "must return OK and the full view for a GET" in {
      val fullCommodityCodeController = new CountryOfOriginController(
        messageComponentControllers,
        new FakeAuthoriseAction(defaultBodyParser),
        countryOfOriginView,
        formProvider,
        fullSessionRequest,
        sessionService
      )

      val result = fullCommodityCodeController.onPageLoad(fakeRequest)

      status(result) mustEqual OK
      contentAsString(result) mustEqual countryOfOriginView(formProvider().fill("GB"))(
        fakeRequest,
        messages
      ).toString
    }

    "must redirect on Submit to error page when there is a session error" in {
      when(sessionService.updateUserAnswers(any[UserAnswers])) thenReturn EitherT[Future, SessionError, Unit](
        Future.successful(Left(SessionError.InternalUnexpectedError(new Error("session error"))))
      )

      val validCountryCode    = "GB"
      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody(fieldName -> validCountryCode)
      val result              = countryOfOriginController.onSubmit()(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.JourneyRecoveryController.onPageLoad().url)
    }

    "must redirect on submit when user enters correct country code" in {
      when(sessionService.updateUserAnswers(any[UserAnswers])) thenReturn EitherT[Future, SessionError, Unit](
        Future.successful(Right(()))
      )

      val validCountryCode    = "GB"
      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody(fieldName -> validCountryCode)
      val result              = countryOfOriginController.onSubmit()(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DummyController.onPageLoad.url)
    }

    "must bad request on submit when user leave the field blank" in {
      val formWithErrors = formProvider().bind(Map.empty[String, String])
      val result         = countryOfOriginController.onSubmit()(fakeRequest)

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) mustEqual countryOfOriginView(formWithErrors)(fakeRequest, messages).toString

    }

    "must bad request on submit when user enters an invalid country code" in {
      val invalidCountryCode  = "1B£3"
      val formWithErrors      = formProvider().bind(Map(fieldName -> invalidCountryCode))
      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody(fieldName -> invalidCountryCode)
      val result              = countryOfOriginController.onSubmit()(fakeRequestWithData)

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) mustEqual countryOfOriginView(formWithErrors)(fakeRequest, messages).toString
    }
  }
}
