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
import forms.NiphlNumberFormProvider
import generators.NiphlNumberGenerator
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.NiphlNumberView
import forms.NiphlNumberFormProvider
import generators.NiphlNumberGenerator
import models.{CheckMode, NormalMode}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

import scala.concurrent.ExecutionContext

class NiphlNumberControllerSpec extends SpecBase with NiphlNumberGenerator {

  private val formProvider = new NiphlNumberFormProvider()

  private val niphlNumberView = app.injector.instanceOf[NiphlNumberView]

  private val niphlNumberController = new NiphlNumberController(
    messageComponentControllers,
    new FakeAuthoriseAction(defaultBodyParser),
    niphlNumberView,
    formProvider,
    sessionRequest,
    sessionService
  )

  "NiphlNumberController" - {

    "must return OK and the correct view for a GET" in {

      val result = niphlNumberController.onPageLoad(NormalMode)(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual niphlNumberView(formProvider())(fakeRequest, messages).toString

    }

    "must redirect on Submit when user enters valid NIPHL number" - {

      def testNiphlNumber(niphlNumber: String) = {
        val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> niphlNumber)

        val result = niphlNumberController.onSubmit(fakeRequestWithData)

        status(result) mustEqual SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad.url)
      }

      "with 2 letters and 5 numbers" in {
        forAll(niphlAlphaNumericGenerator(2, 5))(niphlNumber => testNiphlNumber(niphlNumber))
      }

      "with 1 letter and 5 numbers" in {
        forAll(niphlAlphaNumericGenerator(1, 5))(niphlNumber => testNiphlNumber(niphlNumber))
      }

      "with 4 to 6 numbers" in {
        forAll(niphlNumericGenerator(1000, 999999))(niphlNumber => testNiphlNumber(niphlNumber))
      }

    }

    "must send bad request on Submit when user doesn't enter anything" in {

      val formWithErrors = formProvider().bind(Map.empty[String, String])

      val fakeRequestWithData = FakeRequest()

      val result = niphlNumberController.onSubmit(fakeRequestWithData)

      status(result) mustEqual BAD_REQUEST

      val pageContent = contentAsString(result)

      pageContent mustEqual niphlNumberView(formWithErrors)(fakeRequestWithData, messages).toString

      pageContent must include("niphlNumber.error.notSupplied")

    }

    "must send bad request on Submit when user entry is invalid format" in {

      val formWithErrors = formProvider().bind(Map("value" -> "A123"))

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "A123")

      val result = niphlNumberController.onSubmit(fakeRequestWithData)

      status(result) mustEqual BAD_REQUEST

      val pageContent = contentAsString(result)

      pageContent mustEqual niphlNumberView(formWithErrors)(fakeRequest, messages).toString

      pageContent must include("niphlNumber.error.wrongFormat")

    }
    "CheckMode" - {

      "must return OK and the correct view for a GET" in {

        val result = niphlNumberController.onPageLoad(CheckMode)(fakeRequest)

        status(result) mustEqual OK

        contentAsString(result) mustEqual niphlNumberView(formProvider())(fakeRequest, stubMessages()).toString

      }
    }
  }
}
