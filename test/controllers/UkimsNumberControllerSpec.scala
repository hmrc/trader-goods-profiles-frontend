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
import forms.UkimsNumberFormProvider
import models.{CheckMode, NormalMode}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.UkimsNumberView

import scala.concurrent.ExecutionContext

class UkimsNumberControllerSpec extends SpecBase {

  private val formProvider = new UkimsNumberFormProvider()

  private val fieldName = "ukimsNumber"

  private val ukimsNumberView = app.injector.instanceOf[UkimsNumberView]

  private val ukimsNumberController = new UkimsNumberController(
    messageComponentControllers,
    new FakeAuthoriseAction(defaultBodyParser),
    ukimsNumberView,
    formProvider,
    sessionRequest,
    sessionService
  )

  "UkimsNumberController" - {

    ukimsNumberController.onPageLoad(NormalMode)(
      fakeRequest
    )

    "must return OK and the correct view for a GET" in {

      val result = ukimsNumberController.onPageLoad(NormalMode)(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual ukimsNumberView(formProvider(), NormalMode)(
        fakeRequest,
        messages
      ).toString

    }

    "must redirect on Submit when user enters correct Ukims number" in {

      val validUkimsNumber = "XI47699357400020231115081800"

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody(fieldName -> validUkimsNumber)

      val result = ukimsNumberController.onSubmit(NormalMode)(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NirmsQuestionController.onPageLoad(NormalMode).url)

    }

    "must send bad request on Submit when user leave the field blank" in {

      val formWithErrors = formProvider().bind(Map.empty[String, String])

      val result = ukimsNumberController.onSubmit(NormalMode)(fakeRequest)

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) mustEqual ukimsNumberView(formWithErrors, NormalMode)(
        fakeRequest,
        messages
      ).toString

    }

    "must send bad request on Submit when user enters invalid ukims number" in {

      val invalidUkimsNumber = "XIAA476993574000202311"

      val formWithErrors = formProvider().bind(Map(fieldName -> invalidUkimsNumber))

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody(fieldName -> invalidUkimsNumber)

      val result = ukimsNumberController.onSubmit(NormalMode)(fakeRequestWithData)

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) mustEqual ukimsNumberView(formWithErrors, NormalMode)(
        fakeRequest,
        messages
      ).toString

    }

    "CheckMode" - {

      "must return OK and the correct view for a GET" in {

        val result = ukimsNumberController.onPageLoad(CheckMode)(fakeRequest)

        status(result) mustEqual OK

        contentAsString(result) mustEqual ukimsNumberView(formProvider(), CheckMode)(
          fakeRequest,
          messages
        ).toString

      }

      "must redirect on Submit when user enters correct Ukims number" in {

        val validUkimsNumber = "XI47699357400020231115081800"

        val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody(fieldName -> validUkimsNumber)

        val result = ukimsNumberController.onSubmit(CheckMode)(fakeRequestWithData)

        status(result) mustEqual SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad.url)

      }
    }
  }
}
