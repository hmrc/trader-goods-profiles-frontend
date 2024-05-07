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
import forms.NirmsNumberFormProvider
import models.{CheckMode, NormalMode}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.Helpers._
import play.api.test.FakeRequest
import views.html.NirmsNumberView

class NirmsNumberControllerSpec extends SpecBase {

  private val formProvider          = new NirmsNumberFormProvider()
  private val nirmsNumberView       = app.injector.instanceOf[NirmsNumberView]
  private val nirmsNumberController = new NirmsNumberController(
    messageComponentControllers,
    new FakeAuthoriseAction(defaultBodyParser),
    nirmsNumberView,
    formProvider,
    emptySessionRequest,
    sessionService
  )

  "NirmsNumberController" - {

    "must return OK and the empty view for a GET" in {

      val result = nirmsNumberController.onPageLoad(NormalMode)(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual nirmsNumberView(formProvider(), NormalMode)(
        fakeRequest,
        messages
      ).toString

    }

    "must return OK and the full view for a GET" in {

      val fullNirmsNumberController = new NirmsNumberController(
        messageComponentControllers,
        new FakeAuthoriseAction(defaultBodyParser),
        nirmsNumberView,
        formProvider,
        fullSessionRequest,
        sessionService
      )

      val result = fullNirmsNumberController.onPageLoad(NormalMode)(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual nirmsNumberView(formProvider().fill("anything"), NormalMode)(
        fakeRequest,
        messages
      ).toString

    }

    "must redirect on Submit to error page when there is a session error" in {

      val badNirmsNumberController = new NirmsNumberController(
        messageComponentControllers,
        new FakeAuthoriseAction(defaultBodyParser),
        nirmsNumberView,
        formProvider,
        emptySessionRequest,
        badSessionService
      )

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("nirmsNumber" -> "RMS-GB-123456")

      val result = badNirmsNumberController.onSubmit(NormalMode)(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.JourneyRecoveryController.onPageLoad().url)

    }

    "must redirect on Submit when user enters correct Nirms number (GB region)" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("nirmsNumber" -> "RMS-GB-123456")

      val result = nirmsNumberController.onSubmit(NormalMode)(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NiphlQuestionController.onPageLoad(NormalMode).url)

    }

    "must redirect on Submit when user enters correct Nirms number (NI region)" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("nirmsNumber" -> "RMS-NI-123456")

      val result = nirmsNumberController.onSubmit(NormalMode)(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NiphlQuestionController.onPageLoad(NormalMode).url)

    }

    "must redirect on Submit when user enters correct Nirms number without hyphens" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("nirmsNumber" -> "RMSGB123456")

      val result = nirmsNumberController.onSubmit(NormalMode)(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NiphlQuestionController.onPageLoad(NormalMode).url)

    }

    "must send bad request when user enters incorrect Nirms number" in {

      val inCorrectnirmsNumber = "DDD-GB-123456"

      val formWithErrors = formProvider().bind(Map("nirmsNumber" -> inCorrectnirmsNumber))

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("nirmsNumber" -> inCorrectnirmsNumber)

      val result = nirmsNumberController.onSubmit(NormalMode)(fakeRequestWithData)

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) mustEqual nirmsNumberView(formWithErrors, NormalMode)(
        fakeRequest,
        messages
      ).toString

      contentAsString(result) must include("nirmsNumber.error.invalidFormat")

    }

    "must send bad request on Submit when user leave the field blank" in {

      val formWithErrors = formProvider().bind(Map.empty[String, String])

      val result = nirmsNumberController.onSubmit(NormalMode)(fakeRequest)

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) mustEqual nirmsNumberView(formWithErrors, NormalMode)(
        fakeRequest,
        messages
      ).toString

      contentAsString(result) must include("nirmsNumber.error.required")

    }

    "CheckMode" - {

      "must return OK and the correct view for a GET" in {

        val result = nirmsNumberController.onPageLoad(CheckMode)(fakeRequest)

        status(result) mustEqual OK

        contentAsString(result) mustEqual nirmsNumberView(formProvider(), CheckMode)(
          fakeRequest,
          messages
        ).toString

      }

      "must redirect on Submit when user enters correct Nirms number (GB region)" in {

        val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("nirmsNumber" -> "RMS-GB-123456")

        val result = nirmsNumberController.onSubmit(CheckMode)(fakeRequestWithData)

        status(result) mustEqual SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad.url)

      }

      "must redirect on Submit when user enters correct Nirms number (NI region)" in {

        val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("nirmsNumber" -> "RMS-NI-123456")

        val result = nirmsNumberController.onSubmit(CheckMode)(fakeRequestWithData)

        status(result) mustEqual SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad.url)

      }

      "must redirect on Submit when user enters correct Nirms number without hyphens" in {

        val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("nirmsNumber" -> "RMSGB123456")

        val result = nirmsNumberController.onSubmit(CheckMode)(fakeRequestWithData)

        status(result) mustEqual SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad.url)

      }
    }
  }
}
