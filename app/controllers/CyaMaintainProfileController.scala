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

import controllers.actions._

import javax.inject.Inject
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import views.html.CyaMaintainProfileView
import models.{NormalMode, TraderProfile}
import viewmodels.checkAnswers.HasNirmsSummary
import viewmodels.govuk.summarylist._
import services.DataCleansingService
import models.helper.CreateProfileJourney
import connectors.TraderProfileConnector
import navigation.Navigator
import pages.CyaMaintainProfilePage
import services.AuditService

import scala.concurrent.{ExecutionContext, Future}

class CyaMaintainProfileController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaMaintainProfileView,
  dataCleansingService: DataCleansingService,
  traderProfileConnector: TraderProfileConnector,
  navigator: Navigator,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String = "Unable to update Trader profile."
  private val continueUrl: Call    = routes.ProfileController.onPageLoad()

  def onPageLoadNirms(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    TraderProfile.validateHasNirms(request.userAnswers) match {
      case Right(_)     =>
        val list = SummaryListViewModel(
          rows = Seq(
            HasNirmsSummary.rowUpdate(request.userAnswers)
          ).flatten
        )
        Ok(view(list, routes.CyaMaintainProfileController.onSubmitNirms))
      case Left(errors) =>
        logErrorsAndContinue(errorMessage, continueUrl, errors)
    }
  }

  def onSubmitNirms(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    TraderProfile.validateHasNirms(request.userAnswers) match {
      case Right(_)     =>
        traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
          val updatedProfile = traderProfile.copy(nirmsNumber = None)
          auditService.auditMaintainProfile(traderProfile, updatedProfile, request.affinityGroup)
          for {
            _ <- traderProfileConnector.submitTraderProfile(updatedProfile, request.eori)
          } yield Redirect(navigator.nextPage(CyaMaintainProfilePage, NormalMode, request.userAnswers))
        }
      case Left(errors) =>
        val errorMessage = "Unable to update Trader profile."
        val continueUrl  = routes.ProfileController.onPageLoad()
        Future.successful(logErrorsAndContinue(errorMessage, continueUrl, errors))
    }

  }
}
