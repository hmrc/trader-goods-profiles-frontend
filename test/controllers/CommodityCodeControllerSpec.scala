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
import forms.{CommodityCodeFormProvider, UkimsNumberFormProvider}
import models.errors.SessionError
import models.{CategorisationAnswers, CommodityCode, MaintainProfileAnswers, UkimsNumber, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.{CommodityCodeView, UkimsNumberView}

import scala.concurrent.Future

class CommodityCodeControllerSpec extends SpecBase {

  private val formProvider = new CommodityCodeFormProvider()

  private val fieldName = "commodityCode"

  private val commodityCodeView = app.injector.instanceOf[CommodityCodeView]

  private val commodityCodeController = new CommodityCodeController(
    stubMessagesControllerComponents(),
    new FakeAuthoriseAction(defaultBodyParser),
    commodityCodeView,
    formProvider,
    sessionRequest,
    sessionService
  )

  "Commodity Code   Controller" - {

    "must return OK and the correct view for a GET" in {

      val result = commodityCodeController.onPageLoad(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual commodityCodeView(formProvider())(fakeRequest, stubMessages()).toString

    }

    "must return OK and the correct view when there's a commodity number" in {

      val validCommodityCode = "654321"

      val commodityCode = CommodityCode(validCommodityCode)

      val categorisationAnswers = CategorisationAnswers(commodityCode = Some(commodityCode))

      val expectedPreFilledForm = formProvider().fill(validCommodityCode)

      val userAnswerMock = UserAnswers(userAnswersId, categorisationAnswers = categorisationAnswers)

      val fakeSessionRequest = new FakeSessionRequestAction(userAnswerMock)

      val commodityCodeController = new CommodityCodeController(
        messageComponentControllers,
        new FakeAuthoriseAction(defaultBodyParser),
        commodityCodeView,
        formProvider,
        fakeSessionRequest,
        sessionService
      )

      val result = commodityCodeController.onPageLoad(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual commodityCodeView(expectedPreFilledForm)(fakeRequest, messages).toString

    }

    "must redirect on Submit when user enters correct commodity code" in {

      val validCommodityCode = "654321"

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody(fieldName -> validCommodityCode)

      val result = commodityCodeController.onSubmit(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER
      //TODO- change redirect when it becomes available
      redirectLocation(result) shouldBe Some(routes.DummyController.onPageLoad.url)

    }

    "must bad request on Submit when user leave the field blank" in {

      val formWithErrors = formProvider().bind(Map.empty[String, String])

      val result = commodityCodeController.onSubmit(fakeRequest)

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) mustEqual commodityCodeView(formWithErrors)(fakeRequest, stubMessages()).toString

    }

    "must bad request on Submit when commodity code is not one of {6,8,10} digits" in {

      val inValidCommodityCode = "10987654321"

      val formWithErrors = formProvider().bind(Map(fieldName -> inValidCommodityCode))

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody(fieldName -> inValidCommodityCode)

      val result = commodityCodeController.onSubmit(fakeRequestWithData)

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) mustEqual commodityCodeView(formWithErrors)(fakeRequest, stubMessages()).toString

      contentAsString(result) must include("commodityCode.error.invalidFormat")

    }

    "must bad request on Submit when user enters invalid commodity code format" in {

      val inValidCommodityCode = "ACCBDGDD"

      val formWithErrors = formProvider().bind(Map(fieldName -> inValidCommodityCode))

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody(fieldName -> inValidCommodityCode)

      val result = commodityCodeController.onSubmit(fakeRequestWithData)

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) mustEqual commodityCodeView(formWithErrors)(fakeRequest, stubMessages()).toString

      contentAsString(result) must include("commodityCode.error.invalidFormat")

    }

    "must redirect on Submit when session fails" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody(fieldName -> "654321")

      val unexpectedError = new Exception("Session error")

      when(sessionService.updateUserAnswers(any[UserAnswers]))
        .thenReturn(EitherT.leftT[Future, Unit](SessionError.InternalUnexpectedError(unexpectedError)))

      val result = commodityCodeController.onSubmit()(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.JourneyRecoveryController.onPageLoad().url)

    }
  }
}
