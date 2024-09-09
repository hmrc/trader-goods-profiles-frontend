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
import models.UserAnswers
import org.scalatestplus.mockito.MockitoSugar
import pages.{HasNirmsUpdatePage, RemoveNirmsPage}
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.{running, _}
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.HasNirmsSummary
import views.html.CyaMaintainProfileView
import viewmodels.govuk.SummaryListFluency

class CyaMaintainProfileControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  "CyaMaintainProfile Controller" - {

    "Has NIRMS" - {

      def createChangeList(app: Application, userAnswers: UserAnswers): SummaryList = SummaryListViewModel(
        rows = Seq(
          HasNirmsSummary.rowUpdate(userAnswers)(messages(app))
        ).flatten
      )

      "must return OK and the correct view for a GET" in {

        val userAnswers = emptyUserAnswers
          .set(RemoveNirmsPage, true)
          .success
          .value
          .set(HasNirmsUpdatePage, false)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        val action = routes.CyaMaintainProfileController.onSubmitNirms

        running(application) {
          val list = createChangeList(application, userAnswers)

          val request = FakeRequest(GET, routes.CyaMaintainProfileController.onPageLoadNirms.url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CyaMaintainProfileView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list, action)(request, messages(application)).toString
        }
      }
    }

  }
}
