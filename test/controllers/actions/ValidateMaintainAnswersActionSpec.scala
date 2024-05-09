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

package controllers.actions

import base.SpecBase
import controllers.routes
import models.requests.DataRequest
import models.{CheckMode, Eori, InternalId, MaintainProfileAnswers, NiphlNumber, NirmsNumber, UkimsNumber, UserAnswers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import scala.concurrent.Future

class ValidateMaintainAnswersActionSpec extends SpecBase with MockitoSugar {
  class Harness() extends ValidateMaintainProfileAnswersActionImpl()(ec) {
    def callRefine[A](
      request: DataRequest[A]
    ): Future[Either[Result, DataRequest[A]]] = refine(request)
  }

  "ValidateMaintainAnswersAction" - {
    val internalId      = InternalId("id")
    val eori            = Eori("eori")
    val fullDataRequest = DataRequest(fakeRequest, internalId, fullUserAnswers, eori)

    "must return data request when userAnswers are valid" in {
      val action = new Harness()
      val result = await(action.callRefine(fullDataRequest))

      result mustBe Right(fullDataRequest)
    }

    "must redirect to UkimsNumberController when ukimsNumber is missing" in {
      val maintainProfileAnswers =
        MaintainProfileAnswers(
          None,
          Some(true),
          Some(NirmsNumber("anything")),
          Some(true),
          Some(NiphlNumber("anything"))
        )

      val partialUserAnswers = UserAnswers(internalId.toString, maintainProfileAnswers, categorisationAnswers)
      val partialDataRequest = DataRequest(fakeRequest, internalId, partialUserAnswers, eori)

      val action = new Harness()
      val result = await(action.callRefine(partialDataRequest))

      result mustBe Left(Redirect(routes.UkimsNumberController.onPageLoad(CheckMode)))
    }

    "must redirect to NirmsQuestionController when hasNirms is missing" in {
      val maintainProfileAnswers =
        MaintainProfileAnswers(
          Some(UkimsNumber("anything")),
          None,
          Some(NirmsNumber("anything")),
          Some(true),
          Some(NiphlNumber("anything"))
        )

      val partialUserAnswers = UserAnswers(internalId.toString, maintainProfileAnswers, categorisationAnswers)
      val partialDataRequest = DataRequest(fakeRequest, internalId, partialUserAnswers, eori)

      val action = new Harness()
      val result = await(action.callRefine(partialDataRequest))

      result mustBe Left(Redirect(routes.NirmsQuestionController.onPageLoad(CheckMode)))
    }

    "must redirect to NirmsNumberController when hasNirms is Yes and nirmsNumber is missing" in {
      val maintainProfileAnswers =
        MaintainProfileAnswers(
          Some(UkimsNumber("anything")),
          Some(true),
          None,
          Some(true),
          Some(NiphlNumber("anything"))
        )

      val partialUserAnswers = UserAnswers(internalId.toString, maintainProfileAnswers, categorisationAnswers)
      val partialDataRequest = DataRequest(fakeRequest, internalId, partialUserAnswers, eori)

      val action = new Harness()
      val result = await(action.callRefine(partialDataRequest))

      result mustBe Left(Redirect(routes.NirmsNumberController.onPageLoad(CheckMode)))
    }

    "must redirect to NiphlQuestionController when hasNiphl is missing" in {
      val maintainProfileAnswers =
        MaintainProfileAnswers(
          Some(UkimsNumber("anything")),
          Some(true),
          Some(NirmsNumber("anything")),
          None,
          Some(NiphlNumber("anything"))
        )

      val partialUserAnswers = UserAnswers(internalId.toString, maintainProfileAnswers, categorisationAnswers)
      val partialDataRequest = DataRequest(fakeRequest, internalId, partialUserAnswers, eori)

      val action = new Harness()
      val result = await(action.callRefine(partialDataRequest))

      result mustBe Left(Redirect(routes.NiphlQuestionController.onPageLoad(CheckMode)))
    }

    "must redirect to NiphlNumberController when hasNiphl is Yes and niphlNumber is missing" in {
      val maintainProfileAnswers =
        MaintainProfileAnswers(
          Some(UkimsNumber("anything")),
          Some(true),
          Some(NirmsNumber("anything")),
          Some(true),
          None
        )

      val partialUserAnswers = UserAnswers(internalId.toString, maintainProfileAnswers, categorisationAnswers)
      val partialDataRequest = DataRequest(fakeRequest, internalId, partialUserAnswers, eori)

      val action = new Harness()
      val result = await(action.callRefine(partialDataRequest))

      result mustBe Left(Redirect(routes.NiphlNumberController.onPageLoad(CheckMode)))
    }

    "must redirect to DummyController when hasNirms is No and nirmsNumber is present" in {
      val maintainProfileAnswers =
        MaintainProfileAnswers(
          Some(UkimsNumber("anything")),
          Some(false),
          Some(NirmsNumber("anything")),
          Some(true),
          Some(NiphlNumber("anything"))
        )

      val partialUserAnswers = UserAnswers(internalId.toString, maintainProfileAnswers, categorisationAnswers)
      val partialDataRequest = DataRequest(fakeRequest, internalId, partialUserAnswers, eori)

      val action = new Harness()
      val result = await(action.callRefine(partialDataRequest))

      result mustBe Left(Redirect(routes.DummyController.onPageLoad))
    }

    "must redirect to DummyController when hasNiphl is No and niphlNumber is present" in {
      val maintainProfileAnswers =
        MaintainProfileAnswers(
          Some(UkimsNumber("anything")),
          Some(true),
          Some(NirmsNumber("anything")),
          Some(false),
          Some(NiphlNumber("anything"))
        )

      val partialUserAnswers = UserAnswers(internalId.toString, maintainProfileAnswers, categorisationAnswers)
      val partialDataRequest = DataRequest(fakeRequest, internalId, partialUserAnswers, eori)

      val action = new Harness()
      val result = await(action.callRefine(partialDataRequest))

      result mustBe Left(Redirect(routes.DummyController.onPageLoad))
    }
  }
}
