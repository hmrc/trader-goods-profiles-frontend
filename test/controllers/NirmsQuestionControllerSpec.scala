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
import forms.NirmsQuestionFormProvider
import models.errors.SessionError
import models.{MaintainProfileAnswers, NirmsNumber, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.NirmsQuestionView

import scala.concurrent.Future

class NirmsQuestionControllerSpec extends SpecBase {

  private val formProvider = new NirmsQuestionFormProvider()

  private val nirmsQuestionView = app.injector.instanceOf[NirmsQuestionView]

  private val nirmsQuestionController = new NirmsQuestionController(
    messageComponentControllers,
    new FakeAuthoriseAction(defaultBodyParser),
    nirmsQuestionView,
    formProvider,
    sessionRequest,
    sessionService
  )

  "NirmsQuestion Controller" - {

    nirmsQuestionController.onPageLoad(
      fakeRequest
    )

    "must return OK and the correct view for a GET" in {

      val result = nirmsQuestionController.onPageLoad(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual nirmsQuestionView(formProvider())(fakeRequest, messages).toString

    }

    "must return OK and the correct view when there's a nirms question in the session data" in {

      val hasNirms = true

      val ukimsNumber = None

      val profileAnswers = MaintainProfileAnswers(
        ukimsNumber = ukimsNumber,
        hasNirms = Some(hasNirms)
      )

      val expectedPreFilledForm = formProvider().fill(hasNirms)

      val userAnswerMock = UserAnswers(userAnswersId, maintainProfileAnswers = profileAnswers)

      val fakeSessionRequest = new FakeSessionRequestAction(userAnswerMock)

      val nirmsQuestionController = new NirmsQuestionController(
        messageComponentControllers,
        new FakeAuthoriseAction(defaultBodyParser),
        nirmsQuestionView,
        formProvider,
        fakeSessionRequest,
        sessionService
      )

      val result = nirmsQuestionController.onPageLoad(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual nirmsQuestionView(expectedPreFilledForm)(fakeRequest, messages).toString

    }

    "must redirect on Submit when user selects yes" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "true")

      val result = nirmsQuestionController.onSubmit(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NirmsNumberController.onPageLoad.url)

    }

    "must redirect on Submit when user selects no" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "false")

      val result = nirmsQuestionController.onSubmit(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NiphlQuestionController.onPageLoad.url)

    }

    "must send bad request on Submit when user doesn't select yes or no" in {

      val formWithErrors = formProvider().bind(Map.empty[String, String])

      val result = nirmsQuestionController.onSubmit(fakeRequest)

      status(result) mustEqual BAD_REQUEST

      val pageContent = contentAsString(result)

      pageContent mustEqual nirmsQuestionView(formWithErrors)(fakeRequest, messages).toString

      pageContent must include("nirmsQuestion.error.notSelected")

    }

    "must redirect on Submit when session fails" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "true")

      val unexpectedError = new Exception("Session error")

      when(sessionService.updateUserAnswers(any[UserAnswers]))
        .thenReturn(EitherT.leftT[Future, Unit](SessionError.InternalUnexpectedError(unexpectedError)))

      val result = nirmsQuestionController.onSubmit(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.JourneyRecoveryController.onPageLoad().url)

    }
  }
}
