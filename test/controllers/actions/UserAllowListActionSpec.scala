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
import connectors.UserAllowListConnector
import models.requests.IdentifierRequest
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserAllowListActionSpec extends SpecBase with MockitoSugar {

  class Harness(mockUserAllowListConnector: UserAllowListConnector)
      extends UserAllowListActionImpl(mockUserAllowListConnector) {
    def callRefine[A](request: IdentifierRequest[A]): Future[Either[Result, IdentifierRequest[A]]] = refine(request)
  }

  "refine action" - {}
}
