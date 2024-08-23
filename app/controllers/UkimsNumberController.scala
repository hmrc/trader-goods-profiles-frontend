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
import forms.UkimsNumberFormProvider
import models.{Mode, NormalMode, TraderProfile}
import navigation.Navigator
import pages.{UkimsNumberPage, UkimsNumberUpdatePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import views.html.UkimsNumberView

import javax.inject.Inject
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
  view: UkimsNumberView,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(UkimsNumberPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, routes.UkimsNumberController.onSubmitCreate(mode), isCreateJourney = true))
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
      for {
        traderProfile  <- traderProfileConnector.getTraderProfile(request.eori)
        updatedAnswers <-
          Future.fromTry(request.userAnswers.set(UkimsNumberUpdatePage, traderProfile.ukimsNumber))
        _              <- sessionRepository.set(updatedAnswers)
      } yield Ok(view(form.fill(traderProfile.ukimsNumber), routes.UkimsNumberController.onSubmitUpdate))
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
                traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
                  if (traderProfile.ukimsNumber == value) {
                    Future.successful(Redirect(navigator.nextPage(UkimsNumberUpdatePage, NormalMode, answers)))
                  } else {
                    val newTraderProfile =
                      TraderProfile(request.eori, value, traderProfile.nirmsNumber, traderProfile.niphlNumber)

                    auditService.auditMaintainProfile(traderProfile, newTraderProfile, request.affinityGroup)

                    traderProfileConnector.submitTraderProfile(newTraderProfile, request.eori).map { _ =>
                      Redirect(navigator.nextPage(UkimsNumberUpdatePage, NormalMode, answers))
                    }
                  }
                }
              }
          }
      )
  }
}
