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
import forms.NiphlNumberFormProvider

import javax.inject.Inject
import models.{Mode, NormalMode, TraderProfile}
import navigation.Navigator
import pages.{HasNiphlUpdatePage, NiphlNumberPage, NiphlNumberUpdatePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import views.html.NiphlNumberView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class NiphlNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  formProvider: NiphlNumberFormProvider,
  traderProfileConnector: TraderProfileConnector,
  val controllerComponents: MessagesControllerComponents,
  view: NiphlNumberView,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(NiphlNumberPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, routes.NiphlNumberController.onSubmitCreate(mode)))
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, routes.NiphlNumberController.onSubmitCreate(mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NiphlNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(NiphlNumberPage, mode, updatedAnswers))
        )
  }

  def onPageLoadUpdate: Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
        request.userAnswers.get(HasNiphlUpdatePage) match {
          case Some(_) =>
            traderProfile.niphlNumber match {
              case None       =>
                Future.successful(Ok(view(form, routes.NiphlNumberController.onSubmitUpdate)))
              case Some(data) =>
                for {
                  updatedAnswers <-
                    Future.fromTry(request.userAnswers.set(NiphlNumberPage, data))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Ok(
                  view(form.fill(data), routes.NiphlNumberController.onSubmitUpdate)
                )
            }
          case None    =>
            traderProfile.niphlNumber match {
              case None       =>
                for {
                  updatedAnswers <-
                    Future.fromTry(request.userAnswers.set(HasNiphlUpdatePage, false))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Ok(
                  view(form, routes.NiphlNumberController.onSubmitUpdate)
                )
              case Some(data) =>
                for {
                  updatedAnswersWithHasNiphl <-
                    Future.fromTry(request.userAnswers.set(HasNiphlUpdatePage, true))
                  updatedAnswers             <-
                    Future.fromTry(updatedAnswersWithHasNiphl.set(NiphlNumberPage, data))
                  _                          <- sessionRepository.set(updatedAnswers)
                } yield Ok(
                  view(form.fill(data), routes.NiphlNumberController.onSubmitUpdate)
                )
            }
        }
      }
    }

  def onSubmitUpdate: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, routes.NiphlNumberController.onSubmitUpdate))),
        value =>
          traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
            if (traderProfile.niphlNumber.getOrElse("") == value) {
              Future.successful(Redirect(routes.ProfileController.onPageLoad()))
            } else {
              request.userAnswers.set(NiphlNumberUpdatePage, value) match {
                case Success(answers) =>
                  sessionRepository.set(answers).flatMap { _ =>
                    TraderProfile.buildNiphl(answers, request.eori, traderProfile) match {
                      case Right(model) =>
                        auditService.auditMaintainProfile(traderProfile, model, request.affinityGroup)

                        for {
                          _ <- traderProfileConnector.submitTraderProfile(model, request.eori)
                        } yield Redirect(navigator.nextPage(NiphlNumberUpdatePage, NormalMode, answers))
                      case Left(errors) =>
                        val errorMessage = "Unable to update Trader profile."
                        Future.successful(logErrorsAndContinue(errorMessage, routes.HasNiphlController.onPageLoadUpdate(NormalMode), errors))
                    }
                  }
              }
            }
          }
      )
  }

}
