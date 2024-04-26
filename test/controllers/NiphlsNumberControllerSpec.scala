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
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.NiphlsNumberView
import forms.NiphlsNumberFormProvider
import generators.NiphlsNumberGenerator
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

class NiphlsNumberControllerSpec extends SpecBase with NiphlsNumberGenerator {

  private val formProvider = new NiphlsNumberFormProvider()

  private val niphlsNumberView = app.injector.instanceOf[NiphlsNumberView]

  private val niphlsNumberController = new NiphlsNumberController(
    stubMessagesControllerComponents(),
    new FakeAuthoriseAction(defaultBodyParser),
    niphlsNumberView,
    formProvider
  )

  "NiphlsNumber Controller" - {


    "must return OK and the correct view for a GET" in {

      val result = niphlsNumberController.onPageLoad(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual niphlsNumberView(formProvider())(fakeRequest, stubMessages()).toString

    }

    "must redirect on Submit when user enters valid NIPHL number" - {

      "with 2 letters and 5 numbers" in {
        forAll(niphlsAlphaNumericGenerator(2, 5) -> "validDataItem") { niphlNumber =>
          val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> niphlNumber)

          val result = niphlsNumberController.onSubmit(fakeRequestWithData)

          status(result) mustEqual SEE_OTHER

          //TODO point to real next page
          redirectLocation(result) shouldBe Some(routes.DummyController.onPageLoad.url)
        }

      }

      "with 1 letter and 5 numbers" in {
        forAll(niphlsAlphaNumericGenerator(1, 5) -> "validDataItem") { niphlNumber =>
          val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> niphlNumber)

          val result = niphlsNumberController.onSubmit(fakeRequestWithData)

          status(result) mustEqual SEE_OTHER

          //TODO point to real next page
          redirectLocation(result) shouldBe Some(routes.DummyController.onPageLoad.url)
        }
      }

      "with 4 to 6 numbers" in {
        forAll(niphlsNumericGenerator(1000, 999999) -> "validDataItem") { niphlNumber =>
          val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> niphlNumber)

          val result = niphlsNumberController.onSubmit(fakeRequestWithData)

          status(result) mustEqual SEE_OTHER

          //TODO point to real next page
          redirectLocation(result) shouldBe Some(routes.DummyController.onPageLoad.url)
        }
      }



    }



    "must send bad request on Submit when user doesn't enter anything" in {

      val formWithErrors = formProvider().bind(Map.empty[String, String])

      val fakeRequestWithData = FakeRequest()

      val result = niphlsNumberController.onSubmit(fakeRequestWithData)

      status(result) mustEqual BAD_REQUEST

      val pageContent = contentAsString(result)

      pageContent mustEqual niphlsNumberView(formWithErrors)(fakeRequestWithData, stubMessages()).toString

      pageContent must include("niphlsNumber.error.notSupplied")

    }

    "must send bad request on Submit when user entry is invalid format" in {

      val formWithErrors = formProvider().bind(Map("value" -> "A123"))

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "A123")

      val result = niphlsNumberController.onSubmit(fakeRequestWithData)

      status(result) mustEqual BAD_REQUEST

      val pageContent = contentAsString(result)

      pageContent mustEqual niphlsNumberView(formWithErrors)(fakeRequest, stubMessages()).toString

      pageContent must include("niphlsNumber.error.wrongFormat")

    }
  }
}
