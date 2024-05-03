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
import forms.CountryOfOriginFormProvider
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.CountryOfOriginView

import scala.concurrent.ExecutionContext

class CountryOfOriginControllerSpec extends SpecBase {

  private val formProvider        = new CountryOfOriginFormProvider()
  private val fieldName           = "countryOfOrigin"
  private val countryOfOriginView = app.injector.instanceOf[CountryOfOriginView]

  private val countryOfOriginController = new CountryOfOriginController(
    stubMessagesControllerComponents(),
    new FakeAuthoriseAction(defaultBodyParser),
    countryOfOriginView,
    formProvider,
    sessionRequest,
    sessionService
  )

  "Country Of Origin Controller" - {

    "must return OK and the correct view for a GET" in {
      val result = countryOfOriginController.onPageLoad(fakeRequest)

      status(result) mustEqual OK
      contentAsString(result) mustEqual countryOfOriginView(formProvider())(fakeRequest, stubMessages()).toString
    }

    "must redirect on submit when user enters correct country code" in {
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
      contentAsString(result) mustEqual countryOfOriginView(formWithErrors)(fakeRequest, stubMessages()).toString

    }

    "must bad request on submit when user enters an invalid country code" in {
      val invalidCountryCode  = "1B£3"
      val formWithErrors      = formProvider().bind(Map(fieldName -> invalidCountryCode))
      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody(fieldName -> invalidCountryCode)
      val result              = countryOfOriginController.onSubmit()(fakeRequestWithData)

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) mustEqual countryOfOriginView(formWithErrors)(fakeRequest, stubMessages()).toString
    }
  }
}
