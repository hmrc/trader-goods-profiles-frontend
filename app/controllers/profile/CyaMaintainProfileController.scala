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

package controllers.profile

import cats.data
import cats.data.EitherNec
import connectors.TraderProfileConnector
import controllers.BaseController
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import models.{NormalMode, TraderProfile, ValidationError}
import navigation.ProfileNavigator
import org.apache.pekko.Done
import pages.profile.CyaMaintainProfilePage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants._
import utils.SessionData._
import viewmodels.checkAnswers.profile._
import viewmodels.govuk.all.SummaryListViewModel
import views.html.profile.CyaMaintainProfileView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class CyaMaintainProfileController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              profileAuth: ProfileAuthenticateAction,
                                              val controllerComponents: MessagesControllerComponents,
                                              view: CyaMaintainProfileView,
                                              traderProfileConnector: TraderProfileConnector,
                                              navigator: ProfileNavigator,
                                              auditService: AuditService
                                            )(implicit ec: ExecutionContext)
  extends BaseController {

  private val errorMessage: String = "Unable to update Trader profile."

  def onPageLoadNirms(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    TraderProfile.validateHasNirms(request.userAnswers) match {
      case Right(_) =>
        val list = SummaryListViewModel(
          rows = Seq(
            HasNirmsSummary.rowUpdate(request.userAnswers)
          ).flatten
        )
        Ok(view(list, routes.CyaMaintainProfileController.onSubmitNirms(), hasNirmsKey))
          .removingFromSession(dataUpdated, pageUpdated, dataRemoved, dataAdded)
      case Left(errors) =>
        logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors)
    }
  }

  def onPageLoadUkimsNumber(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    TraderProfile.validateUkimsNumber(request.userAnswers) match {
      case Right(_) =>
        val list = SummaryListViewModel(
          rows = Seq(
            UkimsNumberSummary.rowUpdate(request.userAnswers)
          ).flatten
        )
        Ok(view(list, routes.CyaMaintainProfileController.onSubmitUkimsNumber(), ukimsNumberKey))
          .removingFromSession(dataUpdated, pageUpdated, dataRemoved, dataAdded)
      case Left(errors) =>
        logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors)
    }
  }

  def onSubmitUkimsNumber(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      (for {
        ukimsNumber <- handleValidateError(TraderProfile.validateUkimsNumber(request.userAnswers))
        oldTraderProfile <- traderProfileConnector.getTraderProfile(request.eori)
        newTraderProfile <- Future.successful(oldTraderProfile.copy(ukimsNumber = ukimsNumber))
        _ = auditService.auditMaintainProfile(oldTraderProfile, newTraderProfile, request.affinityGroup)
        _ <- submitTraderProfileIfValueChanged(newTraderProfile, oldTraderProfile, request.eori)
      } yield Redirect(navigator.nextPage(CyaMaintainProfilePage, NormalMode, request.userAnswers))
        .addingToSession(
          dataUpdated -> (oldTraderProfile != newTraderProfile).toString
        )
        .addingToSession(pageUpdated -> ukimsNumberUpdatePage)).recover { case e: TraderProfileBuildFailure =>
        logErrorsAndContinue(e.getMessage, routes.ProfileController.onPageLoad())
      }
  }

  def onSubmitNirms(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    (for {
      nirmsNumber <- handleValidateError(TraderProfile.validateHasNirms(request.userAnswers))
      oldTraderProfile <- traderProfileConnector.getTraderProfile(request.eori)
      newTraderProfile <- Future.successful(oldTraderProfile.copy(nirmsNumber = nirmsNumber))
      _ = auditService.auditMaintainProfile(oldTraderProfile, newTraderProfile, request.affinityGroup)
      _ <- submitTraderProfileIfValueChanged(newTraderProfile, oldTraderProfile, request.eori)
    } yield Redirect(navigator.nextPage(CyaMaintainProfilePage, NormalMode, request.userAnswers))
      .addingToSession(
        dataUpdated -> (oldTraderProfile != newTraderProfile).toString
      )
      .addingToSession(
        dataRemoved -> (oldTraderProfile.nirmsNumber.isDefined && newTraderProfile.nirmsNumber.isEmpty).toString
      )
      .addingToSession(pageUpdated -> hasNirmsUpdatePage)).recover { case e: TraderProfileBuildFailure =>
      logErrorsAndContinue(e.getMessage, routes.ProfileController.onPageLoad())
    }
  }

  def onPageLoadNiphl(): Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData).async {
    implicit request =>
      TraderProfile.validateHasNiphl(request.userAnswers) match {
        case Right(_) =>
          val list = SummaryListViewModel(
            rows = Seq(
              HasNiphlSummary.rowUpdate(request.userAnswers)
            ).flatten
          )
          Future.successful(
            Ok(view(list, routes.CyaMaintainProfileController.onSubmitNiphl(), hasNiphlKey))
              .removingFromSession(dataUpdated, pageUpdated, dataRemoved, dataAdded)
          )
        case Left(errors) =>
          Future.successful(logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors))
      }
  }

  def onSubmitNiphl(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    (for {
      niphlNumber <- handleValidateError(TraderProfile.validateHasNiphl(request.userAnswers))
      oldTraderProfile <- traderProfileConnector.getTraderProfile(request.eori)
      newTraderProfile <- Future.successful(oldTraderProfile.copy(niphlNumber = niphlNumber))
      _ = auditService.auditMaintainProfile(oldTraderProfile, newTraderProfile, request.affinityGroup)
      _ <- submitTraderProfileIfValueChanged(newTraderProfile, oldTraderProfile, request.eori)
    } yield Redirect(navigator.nextPage(CyaMaintainProfilePage, NormalMode, request.userAnswers))
      .addingToSession(
        dataUpdated -> (oldTraderProfile != newTraderProfile).toString
      )
      .addingToSession(
        dataRemoved -> (oldTraderProfile.niphlNumber.isDefined && newTraderProfile.niphlNumber.isEmpty).toString
      )
      .addingToSession(pageUpdated -> hasNiphlUpdatePage)).recover { case e: TraderProfileBuildFailure =>
      logErrorsAndContinue(e.getMessage, routes.ProfileController.onPageLoad())
    }
  }

  def onPageLoadNirmsNumber(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      TraderProfile.validateNirmsNumber(
        request.userAnswers
      ) match {
        case Right(_) =>
          val list = SummaryListViewModel(
            rows = Seq(
              HasNirmsSummary.rowUpdate(request.userAnswers),
              NirmsNumberSummary.rowUpdate(request.userAnswers)
            ).flatten
          )
          Future.successful(
            Ok(view(list, routes.CyaMaintainProfileController.onSubmitNirmsNumber(), nirmsNumberKey))
              .removingFromSession(dataUpdated, pageUpdated, dataRemoved, dataAdded)
          )
        case Left(errors) =>
          Future.successful(logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors))
      }
    }

  def onSubmitNirmsNumber(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      (for {
        nirmsNumber <- handleValidateError(TraderProfile.validateNirmsNumber(request.userAnswers))
        oldTraderProfile <- traderProfileConnector.getTraderProfile(request.eori)
        newTraderProfile <- Future.successful(oldTraderProfile.copy(nirmsNumber = nirmsNumber))
        _ = auditService.auditMaintainProfile(oldTraderProfile, newTraderProfile, request.affinityGroup)
        _ <- submitTraderProfileIfValueChanged(newTraderProfile, oldTraderProfile, request.eori)
      } yield Redirect(navigator.nextPage(CyaMaintainProfilePage, NormalMode, request.userAnswers))
        .addingToSession(
          dataUpdated -> (oldTraderProfile != newTraderProfile).toString
        )
        .addingToSession(
          dataAdded -> (oldTraderProfile.nirmsNumber.isEmpty && newTraderProfile.nirmsNumber.isDefined).toString
        )
        .addingToSession(pageUpdated -> nirmsNumberUpdatePage)).recover { case e: TraderProfileBuildFailure =>
        logErrorsAndContinue(e.getMessage, routes.ProfileController.onPageLoad())
      }
  }

  def onPageLoadNiphlNumber(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      TraderProfile.validateNiphlNumber(
        request.userAnswers
      ) match {
        case Right(_) =>
          val list = SummaryListViewModel(
            rows = Seq(
              HasNiphlSummary.rowUpdate(request.userAnswers),
              NiphlNumberSummary.rowUpdate(request.userAnswers)
            ).flatten
          )
          Future.successful(
            Ok(view(list, routes.CyaMaintainProfileController.onSubmitNiphlNumber(), niphlNumberKey))
              .removingFromSession(dataUpdated, pageUpdated, dataRemoved, dataAdded)
          )
        case Left(errors) =>
          Future.successful(logErrorsAndContinue(errorMessage, routes.ProfileController.onPageLoad(), errors))
      }
    }

  def onSubmitNiphlNumber(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      (for {
        niphlNumber <- handleValidateError(TraderProfile.validateNiphlNumber(request.userAnswers))
        oldTraderProfile <- traderProfileConnector.getTraderProfile(request.eori)
        newTraderProfile <- Future.successful(oldTraderProfile.copy(niphlNumber = niphlNumber))
        _ = auditService.auditMaintainProfile(oldTraderProfile, newTraderProfile, request.affinityGroup)
        _ <- submitTraderProfileIfValueChanged(newTraderProfile, oldTraderProfile, request.eori)
      } yield Redirect(navigator.nextPage(CyaMaintainProfilePage, NormalMode, request.userAnswers))
        .addingToSession(
          dataUpdated -> (oldTraderProfile != newTraderProfile).toString
        )
        .addingToSession(
          dataAdded -> (oldTraderProfile.niphlNumber.isEmpty && newTraderProfile.niphlNumber.isDefined).toString
        )
        .addingToSession(pageUpdated -> niphlNumberUpdatePage)).recover { case e: TraderProfileBuildFailure =>
        logErrorsAndContinue(e.getMessage, routes.ProfileController.onPageLoad())
      }
  }

  private def submitTraderProfileIfValueChanged(
                                                 newTraderProfile: TraderProfile,
                                                 oldTraderProfile: TraderProfile,
                                                 eori: String
                                               )(implicit hc: HeaderCarrier): Future[Done] =
    if (newTraderProfile != oldTraderProfile) {
      traderProfileConnector.submitTraderProfile(newTraderProfile, eori)
    } else {
      Future.successful(Done)
    }

  private def handleValidateError[T](result: EitherNec[ValidationError, T]): Future[T] =
    result match {
      case Right(value) => Future.successful(value)
      case Left(errors) => Future.failed(TraderProfileBuildFailure(errors))
    }

  private case class TraderProfileBuildFailure(errors: data.NonEmptyChain[ValidationError]) extends Exception {
    private val errorsAsString = errors.toChain.toList.map(_.message).mkString(", ")

    override def getMessage: String = s"$errorMessage Missing pages: $errorsAsString"
  }
}
