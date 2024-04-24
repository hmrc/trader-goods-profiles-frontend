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
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.UkimsNumberView

class UkimsNumberControllerSpec extends SpecBase {

  private val formProvider = new UkimsNumberFormProvider()

  private val ukimsNumberView = app.injector.instanceOf[UkimsNumberView]

  private val ukimsNumberController = new UkimsNumberController(
    stubMessagesControllerComponents(),
    new FakeAuthoriseAction(defaultBodyParser),
    ukimsNumberView,
    formProvider
  )

  "Ukims Number Controller" - {

    "must return OK and the correct view for a GET" in {

      val result = ukimsNumberController.onPageLoad(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual ukimsNumberView(formProvider())(fakeRequest, stubMessages()).toString

    }

    "must redirect on Submit when user enters correct Ukims number" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("ukimsNumber" -> "XI47699357400020231115081800")

      val result = ukimsNumberController.onSubmit(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.DummyController.onPageLoad.url)

    }

    "must send bad request on Submit when user leave the field blank" in {

      val formWithErrors = formProvider().bind(Map.empty[String, String])

      val result = ukimsNumberController.onSubmit(fakeRequest)

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) mustEqual ukimsNumberView(formWithErrors)(fakeRequest, stubMessages()).toString

    }
  }
}
