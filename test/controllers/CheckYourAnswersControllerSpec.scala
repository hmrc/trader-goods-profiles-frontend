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
import controllers.actions.{FakeAuthoriseAction, FakeSessionRequestAction}
import play.api.test.Helpers._
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency {

  private val checkYourAnswersView: CheckYourAnswersView = app.injector.instanceOf[CheckYourAnswersView]

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a onPageLoad with user answers set" in {
      val checkYourAnswersControllerWithData = new CheckYourAnswersController(
        messagesApi,
        new FakeAuthoriseAction(defaultBodyParser),
        new FakeSessionRequestAction(emptyUserAnswers),
        messageComponentControllers,
        checkYourAnswersView
      )

      val result = checkYourAnswersControllerWithData.onPageLoad()(fakeRequest)

      val expectedList = SummaryListViewModel(
        rows = Seq.empty
      )

      status(result) mustEqual OK

      contentAsString(result) mustEqual checkYourAnswersView(expectedList)(fakeRequest, messages).toString

    }

  }
}
