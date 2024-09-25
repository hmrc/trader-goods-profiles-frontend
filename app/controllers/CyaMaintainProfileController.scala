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
import org.apache.pekko.Done
import pages.{CyaMaintainProfilePage, HasNiphlUpdatePage, HasNirmsUpdatePage, NiphlNumberUpdatePage, NirmsNumberUpdatePage, Page, RemoveNiphlPage, RemoveNirmsPage, UkimsNumberUpdatePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuditService
import utils.SessionData.{dataAdded, dataRemoved, dataUpdated, niphlNumberUpdatePage, nirmsNumberUpdatePage, pageUpdated, ukimsNumberUpdatePage}
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.CyaMaintainProfileView
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CyaMaintainProfileController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
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
          .removingFromSession(dataUpdated, pageUpdated, dataRemoved, dataAdded)
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
          .removingFromSession(dataUpdated, pageUpdated, dataRemoved, dataAdded)
      case Left(errors) =>
        logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors)
    }
  }

  def onSubmitUkimsNumber(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      (for {
        oldTraderProfile       <- traderProfileConnector.getTraderProfile(request.eori)
        Right(newTraderProfile) = TraderProfile.buildUkims(request.userAnswers, request.eori, oldTraderProfile)
        _                       = auditService.auditMaintainProfile(oldTraderProfile, newTraderProfile, request.affinityGroup)
        _                      <- submitTraderProfileIfValueChanged(oldTraderProfile, newTraderProfile, UkimsNumberUpdatePage, request.eori)
      } yield Redirect(navigator.nextPage(CyaMaintainProfilePage, NormalMode, request.userAnswers))
        .addingToSession(
          dataUpdated -> isValueChanged(newTraderProfile, oldTraderProfile, UkimsNumberUpdatePage).toString
        )
        .addingToSession(pageUpdated -> ukimsNumberUpdatePage)).recover { case _ =>
        navigator.journeyRecovery(Some(RedirectUrl(routes.ProfileController.onPageLoad().url)))
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

  def onPageLoadNiphl(): Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData).async {
    implicit request =>
      TraderProfile.validateNiphlsUpdate(request.userAnswers) match {
        case Right(_)     =>
          val list = SummaryListViewModel(
            rows = Seq(
              HasNiphlSummary.rowUpdate(request.userAnswers),
              NiphlNumberSummary.rowUpdate(request.userAnswers)
            ).flatten
          )
          Future.successful(
            Ok(view(list, routes.CyaMaintainProfileController.onSubmitNiphl))
              .removingFromSession(dataUpdated, pageUpdated, dataRemoved, dataAdded)
          )
        case Left(errors) =>
          Future.successful(logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors))
      }
  }

  def onSubmitNiphl(): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      TraderProfile.validateNiphlsUpdate(request.userAnswers) match {
        case Right(niphlNumber) =>
          traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
            val updatedProfile = traderProfile.copy(niphlNumber = niphlNumber)
            auditService.auditMaintainProfile(traderProfile, updatedProfile, request.affinityGroup)
            for {
              _ <- traderProfileConnector.submitTraderProfile(updatedProfile, request.eori)
            } yield Redirect(navigator.nextPage(CyaMaintainProfilePage, NormalMode, request.userAnswers))
          }
        case Left(errors)       =>
          Future.successful(logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors))
      }
    }

  def onPageLoadNirmsNumber(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      TraderProfile.getOptionallyRemovedPage(
        request.userAnswers,
        HasNirmsUpdatePage,
        RemoveNirmsPage,
        NirmsNumberUpdatePage
      ) match {
        case Right(_)     =>
          val list = SummaryListViewModel(
            rows = Seq(
              HasNirmsSummary.rowUpdate(request.userAnswers),
              NirmsNumberSummary.rowUpdate(request.userAnswers)
            ).flatten
          )
          Future.successful(
            Ok(view(list, routes.CyaMaintainProfileController.onSubmitNirmsNumber))
              .removingFromSession(dataUpdated, pageUpdated, dataRemoved, dataAdded)
          )
        case Left(errors) =>
          Future.successful(logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors))
      }
    }

  def onSubmitNirmsNumber(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      (for {
        oldTraderProfile       <- traderProfileConnector.getTraderProfile(request.eori)
        Right(newTraderProfile) = TraderProfile.buildNirms(request.userAnswers, request.eori, oldTraderProfile)
        _                       = auditService.auditMaintainProfile(oldTraderProfile, newTraderProfile, request.affinityGroup)
        _                      <- submitTraderProfileIfValueChanged(oldTraderProfile, newTraderProfile, NirmsNumberUpdatePage, request.eori)
      } yield Redirect(navigator.nextPage(CyaMaintainProfilePage, NormalMode, request.userAnswers))
        .addingToSession(
          dataUpdated -> isValueChanged(newTraderProfile, oldTraderProfile, NirmsNumberUpdatePage).toString
        )
        .addingToSession(dataAdded -> isValueAdded(newTraderProfile, oldTraderProfile, NirmsNumberUpdatePage).toString)
        .addingToSession(pageUpdated -> nirmsNumberUpdatePage)).recover { case _ =>
        navigator.journeyRecovery(Some(RedirectUrl(routes.ProfileController.onPageLoad().url)))
      }
  }

  def onPageLoadNiphlNumber(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      TraderProfile.getOptionallyRemovedPage(
        request.userAnswers,
        HasNiphlUpdatePage,
        RemoveNiphlPage,
        NiphlNumberUpdatePage
      ) match {
        case Right(_)     =>
          val list = SummaryListViewModel(
            rows = Seq(
              HasNiphlSummary.rowUpdate(request.userAnswers),
              NiphlNumberSummary.rowUpdate(request.userAnswers)
            ).flatten
          )
          Future.successful(
            Ok(view(list, routes.CyaMaintainProfileController.onSubmitNiphlNumber))
              .removingFromSession(dataUpdated, pageUpdated, dataRemoved, dataAdded)
          )
        case Left(errors) =>
          Future.successful(logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors))
      }
    }

  def onSubmitNiphlNumber(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      (for {
        oldTraderProfile       <- traderProfileConnector.getTraderProfile(request.eori)
        Right(newTraderProfile) = TraderProfile.buildNiphl(request.userAnswers, request.eori, oldTraderProfile)
        _                       = auditService.auditMaintainProfile(oldTraderProfile, newTraderProfile, request.affinityGroup)
        _                      <- submitTraderProfileIfValueChanged(oldTraderProfile, newTraderProfile, NiphlNumberUpdatePage, request.eori)
      } yield Redirect(navigator.nextPage(CyaMaintainProfilePage, NormalMode, request.userAnswers))
        .addingToSession(
          dataUpdated -> isValueChanged(newTraderProfile, oldTraderProfile, NiphlNumberUpdatePage).toString
        )
        .addingToSession(dataAdded -> isValueAdded(newTraderProfile, oldTraderProfile, NiphlNumberUpdatePage).toString)
        .addingToSession(pageUpdated -> niphlNumberUpdatePage)).recover { case _ =>
        navigator.journeyRecovery(Some(RedirectUrl(routes.ProfileController.onPageLoad().url)))
      }
  }

  def isValueChanged(newTraderProfile: TraderProfile, oldTraderProfile: TraderProfile, page: Page): Boolean =
    page match {
      case UkimsNumberUpdatePage => oldTraderProfile.ukimsNumber != newTraderProfile.ukimsNumber
      case NirmsNumberUpdatePage => oldTraderProfile.nirmsNumber != newTraderProfile.nirmsNumber
      case NiphlNumberUpdatePage => oldTraderProfile.niphlNumber != newTraderProfile.niphlNumber
    }

  def isValueAdded(newTraderProfile: TraderProfile, oldTraderProfile: TraderProfile, page: Page): Boolean =
    page match {
      case NirmsNumberUpdatePage => oldTraderProfile.nirmsNumber.isEmpty && newTraderProfile.nirmsNumber.isDefined
      case NiphlNumberUpdatePage => oldTraderProfile.niphlNumber.isEmpty && newTraderProfile.niphlNumber.isDefined
    }

  def submitTraderProfileIfValueChanged(
    newTraderProfile: TraderProfile,
    oldTraderProfile: TraderProfile,
    page: Page,
    eori: String
  )(implicit hc: HeaderCarrier): Future[Done] =
    if (isValueChanged(newTraderProfile, oldTraderProfile, page)) {
      traderProfileConnector.submitTraderProfile(newTraderProfile, eori)
    } else {
      Future.successful(Done)
    }
}
