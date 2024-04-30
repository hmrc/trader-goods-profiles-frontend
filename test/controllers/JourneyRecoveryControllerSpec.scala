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
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.{JourneyRecoveryContinueView, JourneyRecoveryStartAgainView}

class JourneyRecoveryControllerSpec extends SpecBase {

  private val journeyRecoveryContinueView = app.injector.instanceOf[JourneyRecoveryContinueView]
  private val journeyRecoveryStartAgainView = app.injector.instanceOf[JourneyRecoveryStartAgainView]

  private val journeyRecoveryController = new JourneyRecoveryController(
    messageComponentControllers,
    new FakeAuthoriseAction(defaultBodyParser),
    journeyRecoveryContinueView,
    journeyRecoveryStartAgainView
  )

  "JourneyRecovery Controller" - {

    "when a relative continue Url is supplied" - {

      "must return OK and the continue view" in {
        val continueUrl = RedirectUrl("/foo")
        val result = journeyRecoveryController.onPageLoad(Some(continueUrl))(fakeRequest)

        status(result) mustEqual OK
        contentAsString(result) mustEqual journeyRecoveryContinueView(continueUrl.unsafeValue)(
          fakeRequest,
          messages
        ).toString

      }
    }

    "when an absolute continue Url is supplied" - {

      "must return OK and the start again view" in {

        val continueUrl = RedirectUrl("https://foo.com")
        val result = journeyRecoveryController.onPageLoad(Some(continueUrl))(fakeRequest)

        status(result) mustEqual OK
        contentAsString(result) mustEqual journeyRecoveryStartAgainView()(
          fakeRequest,
          messages
        ).toString

      }
    }

    "when no continue Url is supplied" - {

      "must return OK and the start again view" in {

        val result = journeyRecoveryController.onPageLoad()(fakeRequest)

        status(result) mustEqual OK
        contentAsString(result) mustEqual journeyRecoveryStartAgainView()(
          fakeRequest,
          messages
        ).toString

      }
    }
  }
}
