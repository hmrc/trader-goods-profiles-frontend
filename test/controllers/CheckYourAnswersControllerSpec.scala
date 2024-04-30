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
import controllers.actions.{DataRequiredActionImpl, DataRetrievalActionImpl, FakeAuthoriseAction}
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.Helpers._
import repositories.SessionRepository
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency {

  private val checkYourAnswersView: CheckYourAnswersView = app.injector.instanceOf[CheckYourAnswersView]
  private val sessionRepository: SessionRepository = mock[SessionRepository]

  //TODO needs to be updated when session action added to replace the dataretrieval / required actions
  // Could make fake actions then but not doing it now because no point creating fake actions that will be deleted tomorrow
  val checkYourAnswersController = new CheckYourAnswersController(
    messagesApi,
    new FakeAuthoriseAction(defaultBodyParser),
    new DataRetrievalActionImpl(sessionRepository),
    new DataRequiredActionImpl(),
    stubMessagesControllerComponents(),
    checkYourAnswersView
  )

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a onPageLoad with user answers set" in {
      when(sessionRepository.get(any)).thenReturn(Future.successful(Some(UserAnswers("id"))))

      val result = checkYourAnswersController.onPageLoad()(fakeRequest)

      val expectedList = SummaryListViewModel(
        rows = Seq.empty
      )

      status(result) mustEqual OK

      contentAsString(result) mustEqual checkYourAnswersView(expectedList)(fakeRequest, messages).toString

    }

    "must redirect to Journey Recovery for a onPageLoad if no existing data is found" in {

      when(sessionRepository.get(any)).thenReturn(Future.successful(None))

      val result = checkYourAnswersController.onPageLoad()(fakeRequest)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

    }
  }
}
