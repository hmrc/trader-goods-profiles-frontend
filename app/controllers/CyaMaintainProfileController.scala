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
import controllers.actions._
import models.{NormalMode, TraderProfile}
import navigation.Navigator
import pages.CyaMaintainProfilePage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuditService
import viewmodels.checkAnswers.{HasNirmsSummary, NirmsNumberSummary, UkimsNumberSummary}
import viewmodels.govuk.summarylist._
import views.html.CyaMaintainProfileView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CyaMaintainProfileController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaMaintainProfileView,
  traderProfileConnector: TraderProfileConnector,
  navigator: Navigator,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String = "Unable to update Trader profile."

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
        logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors)
    }
  }

  def onPageLoadUkimsNumber(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    TraderProfile.validateUkimsNumber(request.userAnswers) match {
      case Right(_)     =>
        val list = SummaryListViewModel(
          rows = Seq(
            UkimsNumberSummary.rowUpdate(request.userAnswers)
          ).flatten
        )
        Ok(view(list, routes.CyaMaintainProfileController.onSubmitUkimsNumber))
      case Left(errors) =>
        logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors)
    }
  }

  def onSubmitUkimsNumber(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      TraderProfile.validateUkimsNumber(request.userAnswers) match {
        case Right(value) =>
          for {
            traderProfile <- traderProfileConnector.getTraderProfile(request.eori)
            _             <-
              auditService
                .auditMaintainProfile(traderProfile, traderProfile.copy(ukimsNumber = value), request.affinityGroup)
            _             <- traderProfileConnector.submitTraderProfile(traderProfile.copy(ukimsNumber = value), request.eori)
          } yield Redirect(navigator.nextPage(CyaMaintainProfilePage, NormalMode, request.userAnswers))
        case Left(errors) =>
          Future.successful(logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors))
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
        Future.successful(logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors))
    }
  }

  def onPageLoadNirmsNumber(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    TraderProfile.validateNirmsNumber(request.userAnswers) match {
      case Right(_)     =>
        val list = SummaryListViewModel(
          rows = Seq(
            HasNirmsSummary.rowUpdate(request.userAnswers),
            NirmsNumberSummary.rowUpdate(request.userAnswers)
          ).flatten
        )
        Ok(view(list, routes.CyaMaintainProfileController.onSubmitNirmsNumber))
      case Left(errors) =>
        logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors)
    }
  }

  def onSubmitNirmsNumber(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
        TraderProfile.buildNirms(request.userAnswers, request.eori, traderProfile) match {
          case Right(model) =>
            auditService.auditMaintainProfile(traderProfile, model, request.affinityGroup)
            for {
              _ <- traderProfileConnector.submitTraderProfile(model, request.eori)
            } yield Redirect(navigator.nextPage(CyaMaintainProfilePage, NormalMode, request.userAnswers))
          case Left(errors) =>
            val errorMessage = "Unable to update Trader profile."
            Future.successful(
              logErrorsAndContinue(errorMessage, routes.HasNirmsController.onPageLoadUpdate(NormalMode), errors)
            )
        }
      }
  }
}
