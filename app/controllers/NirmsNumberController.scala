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
import forms.NirmsNumberFormProvider

import javax.inject.Inject
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.{HasNirmsUpdatePage, NirmsNumberPage, NirmsNumberUpdatePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.NirmsNumberView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class NirmsNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: NirmsNumberFormProvider,
  traderProfileConnector: TraderProfileConnector,
  checkProfile: ProfileCheckAction,
  val controllerComponents: MessagesControllerComponents,
  view: NirmsNumberView,
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(NirmsNumberPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, routes.NirmsNumberController.onSubmitCreate(mode)))
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, routes.NirmsNumberController.onSubmitCreate(mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NirmsNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(NirmsNumberPage, mode, updatedAnswers))
        )
  }

  def onPageLoadUpdate: Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
        val nirmsNumberUpdateAnswer: Option[String] = request.userAnswers.get(NirmsNumberUpdatePage)
        request.userAnswers.get(HasNirmsUpdatePage) match {
          case Some(_) =>
            traderProfile.nirmsNumber match {
              case None       =>
                Future.successful(Ok(view(form, routes.NirmsNumberController.onSubmitUpdate)))
              case Some(data) =>
                for {
                  updatedAnswers <-
                    Future.fromTry(request.userAnswers.set(NirmsNumberPage, data))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Ok(
                  view(form.fill(nirmsNumberUpdateAnswer.getOrElse(data)), routes.NirmsNumberController.onSubmitUpdate)
                )
            }
          case None    =>
            traderProfile.nirmsNumber match {
              case None       =>
                for {
                  updatedAnswers <-
                    Future.fromTry(request.userAnswers.set(HasNirmsUpdatePage, false))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Ok(
                  view(form, routes.NirmsNumberController.onSubmitUpdate)
                )
              case Some(data) =>
                for {
                  updatedAnswersWithHasNirms <-
                    Future.fromTry(request.userAnswers.set(HasNirmsUpdatePage, true))
                  updatedAnswers             <-
                    Future.fromTry(updatedAnswersWithHasNirms.set(NirmsNumberPage, data))
                  _                          <- sessionRepository.set(updatedAnswers)
                } yield Ok(
                  view(form.fill(nirmsNumberUpdateAnswer.getOrElse(data)), routes.NirmsNumberController.onSubmitUpdate)
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
          Future.successful(BadRequest(view(formWithErrors, routes.NirmsNumberController.onSubmitUpdate))),
        value =>
          request.userAnswers.set(NirmsNumberUpdatePage, value) match {
            case Success(answers) =>
              sessionRepository.set(answers).flatMap { _ =>
                Future.successful(Redirect(navigator.nextPage(NirmsNumberUpdatePage, NormalMode, answers)))
              }
          }
      )
  }

}
