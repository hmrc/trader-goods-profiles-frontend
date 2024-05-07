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
import play.api.test.Helpers._
import views.html.DummyView

class DummyControllerSpec extends SpecBase {

  private val dummyView = app.injector.instanceOf[DummyView]

  private val dummyController = new DummyController(
    messageComponentControllers,
    new FakeAuthoriseAction(defaultBodyParser),
    dummyView
  )

  "DummyController" - {

    "must return OK and the correct view onPageLoad" in {

      val result = dummyController.onPageLoad(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual dummyView()(fakeRequest, messages).toString

    }
  }
}
