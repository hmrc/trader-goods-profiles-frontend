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
import controllers.actions.FakeAuthoriseAction
import forms.NiphlQuestionFormProvider
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.NiphlQuestionView
import forms.NiphlQuestionFormProvider
import models.{CheckMode, NormalMode}

import scala.concurrent.ExecutionContext

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

  "NiphlQuestionController" - {

    "must return OK and the correct view for an onPageLoad" in {

      val result = niphlQuestionController.onPageLoad(NormalMode)(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual niphlQuestionView(formProvider(), NormalMode)(
        fakeRequest,
        messages
      ).toString

    }

    "must redirect on Submit when user selects yes" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "true")

      val result = niphlQuestionController.onSubmit(NormalMode)(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NiphlNumberController.onPageLoad(NormalMode).url)

    }

    "must redirect on Submit when user selects no" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "false")

      val result = niphlQuestionController.onSubmit(NormalMode)(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad.url)

    }

    "must send bad request on Submit when user doesn't select yes or no" in {

      val formWithErrors = formProvider().bind(Map.empty[String, String])

      val result = niphlQuestionController.onSubmit(NormalMode)(fakeRequest)

      status(result) mustEqual BAD_REQUEST

      val pageContent = contentAsString(result)

      pageContent mustEqual niphlQuestionView(formWithErrors, NormalMode)(fakeRequest, messages).toString

      pageContent must include("niphlQuestion.radio.notSelected")
    }

    "CheckMode" - {

      "must return OK and the correct view for a GET" in {

        val result = niphlQuestionController.onPageLoad(CheckMode)(fakeRequest)

        status(result) mustEqual OK

        contentAsString(result) mustEqual niphlQuestionView(formProvider(), CheckMode)(
          fakeRequest,
          stubMessages()
        ).toString

      }

      "must redirect on Submit when user selects yes" in {

        val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "true")

        val result = niphlQuestionController.onSubmit(CheckMode)(fakeRequestWithData)

        status(result) mustEqual SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.NiphlNumberController.onPageLoad(CheckMode).url)

      }

      "must redirect on Submit when user selects no" in {

        val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "false")

        val result = niphlQuestionController.onSubmit(CheckMode)(fakeRequestWithData)

        status(result) mustEqual SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad.url)

      }
    }
  }
}
