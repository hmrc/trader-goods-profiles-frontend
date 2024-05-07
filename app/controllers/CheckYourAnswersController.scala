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

import com.google.inject.Inject
import controllers.actions.{AuthoriseAction, SessionRequestAction}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView
import controllers.helpers.CheckYourAnswersHelper

class CheckYourAnswersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthoriseAction,
  view: CheckYourAnswersView,
  getData: SessionRequestAction,
  checkYourAnswersHelper: CheckYourAnswersHelper
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (authorise andThen getData) { implicit request =>
    val list = SummaryListViewModel(
      rows = checkYourAnswersHelper.createSummaryList(request.userAnswers.maintainProfileAnswers)(
        messagesApi.preferred(request)
      )
    )
    Ok(view(list))
  }

  // TODO replace dummy route and post session data
  def onSubmit: Action[AnyContent] = authorise { implicit request =>
    Redirect(routes.DummyController.onPageLoad.url)
  }

}
