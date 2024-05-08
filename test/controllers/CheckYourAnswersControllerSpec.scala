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
import controllers.actions.FakeAuthoriseAction
import controllers.helpers.CheckYourAnswersHelper
import models.{Eori, MaintainProfileAnswers}
import models.errors.RouterError
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.Helpers._
import services.RouterService
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import scala.concurrent.Future
import scala.concurrent.Future.successful

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency {
  private val checkYourAnswersView: CheckYourAnswersView         = app.injector.instanceOf[CheckYourAnswersView]
  private val mockCheckYourAnswersHelper: CheckYourAnswersHelper = mock[CheckYourAnswersHelper]
  private val routerService                                      = mock[RouterService]

  private val checkYourAnswersController = new CheckYourAnswersController(
    stubMessagesControllerComponents(),
    new FakeAuthoriseAction(defaultBodyParser),
    checkYourAnswersView,
    sessionRequest,
    mockCheckYourAnswersHelper,
    routerService
  )

  "CheckYourAnswersController" - {

    "must return OK and the correct view for a GET" in {

      val summaryList = List(
        SummaryListRow(
          Key(HtmlContent("UKIMS number")),
          Value(HtmlContent("11")),
          "",
          Some(
            Actions(
              "",
              List(ActionItem("/trader-goods-profiles/ukims-number/check", HtmlContent("Change"), None, "", Map()))
            )
          )
        ),
        SummaryListRow(
          Key(HtmlContent("NIRMS registered"), ""),
          Value(HtmlContent("No"), ""),
          "",
          Some(
            Actions(
              "",
              List(ActionItem("/trader-goods-profiles/nirms-question/check", HtmlContent("Change"), None, "", Map()))
            )
          )
        ),
        SummaryListRow(
          Key(HtmlContent("NIPHL registered"), ""),
          Value(HtmlContent("No"), ""),
          "",
          Some(
            Actions(
              "",
              List(ActionItem("/trader-goods-profiles/niphl-question/check", HtmlContent("Change"), None, "", Map()))
            )
          )
        )
      )

      val list = SummaryListViewModel(
        rows = summaryList
      )

      when(mockCheckYourAnswersHelper.createSummaryList(any[MaintainProfileAnswers])(any())) thenReturn summaryList

      val result = checkYourAnswersController.onPageLoad()(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual checkYourAnswersView(list)(fakeRequest, messages).toString()

    }

    "must redirect to homepage when successful on Submit" in {

      when(routerService.setUpProfile(any, any)(any))
        .thenReturn(EitherT[Future, RouterError, Unit](successful(Right())))

      val result = checkYourAnswersController.onSubmit()(fakeRequest)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.HomepageController.onPageLoad.url)

      withClue("Ensure correct data is sent to router service") {
        verify(routerService).setUpProfile(eqTo(Eori("eori")), eqTo(emptyUserAnswers.maintainProfileAnswers))(any)
      }

    }

    "must redirect to dummy page when error on Submit" in {

      when(routerService.setUpProfile(any, any)(any))
        .thenReturn(EitherT[Future, RouterError, Unit](successful(Left(RouterError("error", Some(BAD_REQUEST))))))

      val result = checkYourAnswersController.onSubmit()(fakeRequest)

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.DummyController.onPageLoad.url)

    }
  }
}
