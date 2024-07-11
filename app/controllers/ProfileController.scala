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

import connectors.TraderProfileConnector
import controllers.actions.IdentifierAction
import models.NormalMode
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.ProfileView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ProfileController @Inject() (
  override val messagesApi: MessagesApi,
  traderProfileConnector: TraderProfileConnector,
  identify: IdentifierAction,
  val controllerComponents: MessagesControllerComponents,
  view: ProfileView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = identify.async { implicit request =>
    traderProfileConnector.getTraderProfile(request.eori).map { profile =>
      val detailsList = SummaryListViewModel(
        rows = Seq(
          Some(UkimsNumberSummary.row(profile.ukimsNumber, NormalMode)),
          Some(HasNirmsSummary.row(profile.nirmsNumber.isDefined, NormalMode)),
          NirmsNumberSummary.row(profile.nirmsNumber, NormalMode),
          Some(HasNiphlSummary.row(profile.niphlNumber.isDefined, NormalMode)),
          NiphlNumberSummary.row(profile.niphlNumber, NormalMode)
        ).flatten
      )

      Ok(view(detailsList))
    }
  }
}
