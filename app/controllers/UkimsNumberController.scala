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

import cats.data
import connectors.TraderProfileConnector
import controllers.actions._
import forms.UkimsNumberFormProvider

import javax.inject.Inject
import models.{Mode, NormalMode, TraderProfile, UserAnswers, ValidationError}
import navigation.Navigator
import pages.{HasNiphlUpdatePage, HasNirmsUpdatePage, NiphlNumberUpdatePage, NirmsNumberUpdatePage, UkimsNumberPage, UkimsNumberUpdatePage}
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.UkimsNumberView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class UkimsNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  formProvider: UkimsNumberFormProvider,
  traderProfileConnector: TraderProfileConnector,
  val controllerComponents: MessagesControllerComponents,
  view: UkimsNumberView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(UkimsNumberPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, routes.UkimsNumberController.onSubmitCreate(mode)))
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, routes.UkimsNumberController.onSubmitCreate(mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(UkimsNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(UkimsNumberPage, mode, updatedAnswers))
        )
  }

  def onPageLoadUpdate: Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
        if (traderProfile.niphlNumber.isEmpty) {
          if (traderProfile.nirmsNumber.isEmpty) {
            setMandatory(request.userAnswers, traderProfile).map(answers => displayView(answers))
          } else {
            setMandatoryAndNirms(request.userAnswers, traderProfile).map(answers => displayView(answers))
          }
        } else if (traderProfile.nirmsNumber.isEmpty) {
          setMandatoryAndNiphl(request.userAnswers, traderProfile).map(answers => displayView(answers))
        } else {
          setAll(request.userAnswers, traderProfile).map(answers => displayView(answers))
        }
      }
    }

  def setAll(userAnswers: UserAnswers, traderProfile: TraderProfile): Future[UserAnswers] =
    for {
      updatedAnswersWithUkims    <-
        Future.fromTry(userAnswers.set(UkimsNumberUpdatePage, traderProfile.ukimsNumber))
      updatedAnswersWithHasNirms <-
        Future.fromTry(updatedAnswersWithUkims.set(HasNirmsUpdatePage, traderProfile.nirmsNumber.isDefined))
      updatedAnswersWithNirms    <-
        Future.fromTry(updatedAnswersWithHasNirms.set(NirmsNumberUpdatePage, traderProfile.nirmsNumber.getOrElse("")))
      updatedAnswersWithHasNiphl <-
        Future.fromTry(updatedAnswersWithNirms.set(HasNiphlUpdatePage, traderProfile.niphlNumber.isDefined))
      updatedAnswers             <-
        Future.fromTry(updatedAnswersWithHasNiphl.set(NiphlNumberUpdatePage, traderProfile.niphlNumber.getOrElse("")))
      _                          <- sessionRepository.set(updatedAnswers)
    } yield updatedAnswers

  def setMandatory(userAnswers: UserAnswers, traderProfile: TraderProfile): Future[UserAnswers] =
    for {
      updatedAnswersWithUkims    <-
        Future.fromTry(userAnswers.set(UkimsNumberUpdatePage, traderProfile.ukimsNumber))
      updatedAnswersWithHasNirms <-
        Future.fromTry(updatedAnswersWithUkims.set(HasNirmsUpdatePage, traderProfile.nirmsNumber.isDefined))
      updatedAnswers             <-
        Future.fromTry(updatedAnswersWithHasNirms.set(HasNiphlUpdatePage, traderProfile.niphlNumber.isDefined))
      _                          <- sessionRepository.set(updatedAnswers)
    } yield updatedAnswers

  def setMandatoryAndNirms(userAnswers: UserAnswers, traderProfile: TraderProfile): Future[UserAnswers] =
    for {
      updatedAnswersWithUkims    <-
        Future.fromTry(userAnswers.set(UkimsNumberUpdatePage, traderProfile.ukimsNumber))
      updatedAnswersWithHasNirms <-
        Future.fromTry(updatedAnswersWithUkims.set(HasNirmsUpdatePage, traderProfile.nirmsNumber.isDefined))
      updatedAnswersWithNirms    <-
        Future.fromTry(updatedAnswersWithHasNirms.set(NirmsNumberUpdatePage, traderProfile.nirmsNumber.getOrElse("")))
      updatedAnswers             <-
        Future.fromTry(updatedAnswersWithNirms.set(HasNiphlUpdatePage, traderProfile.niphlNumber.isDefined))
      _                          <- sessionRepository.set(updatedAnswers)
    } yield updatedAnswers

  def setMandatoryAndNiphl(userAnswers: UserAnswers, traderProfile: TraderProfile): Future[UserAnswers] =
    for {
      updatedAnswersWithUkims    <-
        Future.fromTry(userAnswers.set(UkimsNumberUpdatePage, traderProfile.ukimsNumber))
      updatedAnswersWithHasNirms <-
        Future.fromTry(updatedAnswersWithUkims.set(HasNirmsUpdatePage, traderProfile.nirmsNumber.isDefined))
      updatedAnswersWithHasNiphl <-
        Future.fromTry(updatedAnswersWithHasNirms.set(HasNiphlUpdatePage, traderProfile.niphlNumber.isDefined))
      updatedAnswers             <-
        Future.fromTry(updatedAnswersWithHasNiphl.set(NiphlNumberUpdatePage, traderProfile.niphlNumber.getOrElse("")))
      _                          <- sessionRepository.set(updatedAnswers)
    } yield updatedAnswers

  def displayView(userAnswers: UserAnswers)(implicit request: Request[_]): Result = {
    val preparedForm = userAnswers.get(UkimsNumberUpdatePage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, routes.UkimsNumberController.onSubmitUpdate))

  }

  def onSubmitUpdate: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, routes.UkimsNumberController.onSubmitUpdate))),
        value =>
          request.userAnswers.set(UkimsNumberUpdatePage, value) match {
            case Success(answers) =>
              sessionRepository.set(answers).flatMap { _ =>
                TraderProfile.buildUpdate(answers, request.eori) match {
                  case Right(model) =>
                    for {
                      _                             <- traderProfileConnector.submitTraderProfile(model, request.eori)
                      updatedAnswersRemovedUkims    <-
                        Future.fromTry(answers.remove(UkimsNumberUpdatePage))
                      updatedAnswersRemovedHasNirms <-
                        Future.fromTry(updatedAnswersRemovedUkims.remove(HasNirmsUpdatePage))
                      updatedAnswersRemovedNirms    <-
                        Future.fromTry(updatedAnswersRemovedHasNirms.remove(NirmsNumberUpdatePage))
                      updatedAnswersRemovedHasNiphl <-
                        Future.fromTry(updatedAnswersRemovedNirms.remove(HasNiphlUpdatePage))
                      updatedAnswers                <-
                        Future.fromTry(updatedAnswersRemovedHasNiphl.remove(NiphlNumberUpdatePage))
                      _                             <- sessionRepository.set(updatedAnswers)
                    } yield Redirect(navigator.nextPage(UkimsNumberUpdatePage, NormalMode, answers))
                  case Left(errors) => Future.successful(logErrorsAndContinue(errors))
                }
              }
          }
      )
  }

  def logErrorsAndContinue(errors: data.NonEmptyChain[ValidationError]): Result = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    val continueUrl = RedirectUrl(routes.ProfileController.onPageLoad().url)

    logger.warn(s"Unable to update Trader profile.  Missing pages: $errorMessages")
    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

}
