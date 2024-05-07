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
import controllers.actions.{FakeAuthoriseAction, FakeSessionRequestAction}
import forms.NiphlQuestionFormProvider
import models.errors.SessionError
import models.{MaintainProfileAnswers, NiphlNumber, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.NiphlQuestionView

import scala.concurrent.Future

class NiphlQuestionControllerSpec extends SpecBase {

  private val formProvider = new NiphlQuestionFormProvider()

  private val niphlQuestionView = app.injector.instanceOf[NiphlQuestionView]

  private val niphlQuestionController = new NiphlQuestionController(
    messageComponentControllers,
    new FakeAuthoriseAction(defaultBodyParser),
    niphlQuestionView,
    formProvider,
    sessionRequest,
    sessionService
  )

  "NiphlQuestion Controller" - {

    "must return OK and the correct view for an onPageLoad" in {

      val result = niphlQuestionController.onPageLoad(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual niphlQuestionView(formProvider())(fakeRequest, messages).toString

    }

    "must return OK and the correct view when there's a niphl question in the session data" in {

      val value = true

      val hasNiphl = value

      val ukimsNumber = None
      val hasNirms    = None
      val nirmsNumber = None

      val profileAnswers = MaintainProfileAnswers(
        ukimsNumber = ukimsNumber,
        hasNirms = hasNirms,
        nirmsNumber = nirmsNumber,
        hasNiphl = Some(hasNiphl)
      )

      val expectedPreFilledForm = formProvider().fill(value)

      val userAnswerMock = UserAnswers(userAnswersId, maintainProfileAnswers = profileAnswers)

      val fakeSessionRequest = new FakeSessionRequestAction(userAnswerMock)

      val niphlQuestionController = new NiphlQuestionController(
        messageComponentControllers,
        new FakeAuthoriseAction(defaultBodyParser),
        niphlQuestionView,
        formProvider,
        fakeSessionRequest,
        sessionService
      )

      val result = niphlQuestionController.onPageLoad(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual niphlQuestionView(expectedPreFilledForm)(fakeRequest, messages).toString

    }

    "must redirect on Submit when user selects yes" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "true")

      val result = niphlQuestionController.onSubmit(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NiphlNumberController.onPageLoad.url)

    }

    "must redirect on Submit when user selects no" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "false")

      val result = niphlQuestionController.onSubmit(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.DummyController.onPageLoad.url)

    }

    "must send bad request on Submit when user doesn't select yes or no" in {

      val formWithErrors = formProvider().bind(Map.empty[String, String])

      val result = niphlQuestionController.onSubmit(fakeRequest)

      status(result) mustEqual BAD_REQUEST

      val pageContent = contentAsString(result)

      pageContent mustEqual niphlQuestionView(formWithErrors)(fakeRequest, messages).toString

      pageContent must include("niphlQuestion.radio.notSelected")
    }

    "must redirect on Submit when session fails" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "true")

      val unexpectedError = new Exception("Session error")

      when(sessionService.updateUserAnswers(any[UserAnswers]))
        .thenReturn(EitherT.leftT[Future, Unit](SessionError.InternalUnexpectedError(unexpectedError)))

      val result = niphlQuestionController.onSubmit(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.JourneyRecoveryController.onPageLoad().url)

    }
  }
}
