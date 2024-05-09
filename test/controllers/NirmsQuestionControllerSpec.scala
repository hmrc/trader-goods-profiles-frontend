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
import forms.NirmsQuestionFormProvider
import models.errors.SessionError
import models.{CheckMode, NormalMode, UserAnswers}
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
    emptySessionRequest,
    sessionService
  )

  "NirmsQuestionController" - {

    nirmsQuestionController.onPageLoad(NormalMode)(
      fakeRequest
    )

    "must return OK and the empty view for a GET" in {

      val result = nirmsQuestionController.onPageLoad(NormalMode)(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual nirmsQuestionView(formProvider(), NormalMode)(
        fakeRequest,
        messages
      ).toString

    }

    "must return OK and the full view for a GET" in {

      val fullNirmsQuestionController = new NirmsQuestionController(
        messageComponentControllers,
        new FakeAuthoriseAction(defaultBodyParser),
        nirmsQuestionView,
        formProvider,
        fullSessionRequest,
        sessionService
      )

      val result = fullNirmsQuestionController.onPageLoad(NormalMode)(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual nirmsQuestionView(formProvider().fill(true), NormalMode)(
        fakeRequest,
        messages
      ).toString

    }

    "must redirect on Submit to error page when there is a session error" in {

      when(sessionService.updateUserAnswers(any[UserAnswers])) thenReturn EitherT[Future, SessionError, Unit](
        Future.successful(Left(SessionError.InternalUnexpectedError(new Error("session error"))))
      )

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "true")

      val result = nirmsQuestionController.onSubmit(NormalMode)(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.JourneyRecoveryController.onPageLoad().url)

    }

    "must redirect on Submit when user selects yes" in {

      when(sessionService.updateUserAnswers(any[UserAnswers])) thenReturn EitherT[Future, SessionError, Unit](
        Future.successful(Right(()))
      )

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "true")

      val result = nirmsQuestionController.onSubmit(NormalMode)(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NirmsNumberController.onPageLoad(NormalMode).url)

    }

    "must redirect on Submit when user selects no" in {

      when(sessionService.updateUserAnswers(any[UserAnswers])) thenReturn EitherT[Future, SessionError, Unit](
        Future.successful(Right(()))
      )

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "false")

      val result = nirmsQuestionController.onSubmit(NormalMode)(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NiphlQuestionController.onPageLoad(NormalMode).url)

    }

    "must send bad request on Submit when user doesn't select yes or no" in {

      val formWithErrors = formProvider().bind(Map.empty[String, String])

      val result = nirmsQuestionController.onSubmit(NormalMode)(fakeRequest)

      status(result) mustEqual BAD_REQUEST

      val pageContent = contentAsString(result)

      pageContent mustEqual nirmsQuestionView(formWithErrors, NormalMode)(fakeRequest, messages).toString

      pageContent must include("nirmsQuestion.error.notSelected")

    }

    "CheckMode" - {

      "must return OK and the correct view for a GET" in {

        val result = nirmsQuestionController.onPageLoad(CheckMode)(fakeRequest)

        status(result) mustEqual OK

        contentAsString(result) mustEqual nirmsQuestionView(formProvider(), CheckMode)(
          fakeRequest,
          messages
        ).toString

      }

      "must redirect on Submit when user selects yes" in {

        when(sessionService.updateUserAnswers(any[UserAnswers])) thenReturn EitherT[Future, SessionError, Unit](
          Future.successful(Right(()))
        )

        val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "true")

        val result = nirmsQuestionController.onSubmit(CheckMode)(fakeRequestWithData)

        status(result) mustEqual SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.NirmsNumberController.onPageLoad(CheckMode).url)

      }

      "must redirect on Submit when user selects no" in {

        when(sessionService.updateUserAnswers(any[UserAnswers])) thenReturn EitherT[Future, SessionError, Unit](
          Future.successful(Right(()))
        )

        val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "false")

        val result = nirmsQuestionController.onSubmit(CheckMode)(fakeRequestWithData)

        status(result) mustEqual SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad.url)

      }
    }
  }
}
