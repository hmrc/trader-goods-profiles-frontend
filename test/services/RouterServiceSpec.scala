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

package services

import base.SpecBase
import cats.data.EitherT
import connectors.RouterConnector
import models.errors.RouterError
import models.router.requests.SetUpProfileRequest
import models.{Eori, MaintainProfileAnswers, NiphlNumber, NirmsNumber, UkimsNumber}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.Future
import scala.concurrent.Future.successful

class RouterServiceSpec extends SpecBase {

  private val connector = mock[RouterConnector]

  private val routerService          = new RouterService(connector)
  private val eori                   = Eori("eori")
  private val maintainProfileAnswers = MaintainProfileAnswers(
    Some(UkimsNumber("ukims")),
    Some(true),
    Some(NirmsNumber("nirms")),
    Some(true),
    Some(NiphlNumber("niphl"))
  )

  "Router Service" - {

    "setUpProfile" - {
      "returns success response when connector is successful" in {

        val expectedRequestData = SetUpProfileRequest(
          "eori",
          Some("ukims"),
          Some("nirms"),
          Some("niphl")
        )

        val connectorResponse = EitherT[Future, RouterError, Unit](successful(Right()))
        when(connector.setUpProfile(any, any)(any, any)).thenReturn(connectorResponse)

        val result = routerService.setUpProfile(eori, maintainProfileAnswers)

        result mustBe connectorResponse

        withClue("should create the router request in the right format") {
          verify(connector).setUpProfile(eqTo(eori), eqTo(expectedRequestData))(any, any)
        }

      }

      "returns failure response when connector is unsuccessful" in {

        val connectorResponse = EitherT[Future, RouterError, Unit](successful(Left(RouterError("blah", None))))
        when(connector.setUpProfile(any, any)(any, any)).thenReturn(connectorResponse)

        val result = routerService.setUpProfile(eori, maintainProfileAnswers)

        result mustBe connectorResponse

      }

    }

  }

}
