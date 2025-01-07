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

import connectors.TraderProfileConnector
import controllers.BaseController
import controllers.actions._
import forms.profile.NiphlNumberFormProvider
import models.Mode
import navigation.ProfileNavigator
import pages.profile.{HasNiphlUpdatePage, NiphlNumberPage, NiphlNumberUpdatePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.profile.NiphlNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NiphlNumberController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: ProfileNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: NiphlNumberFormProvider,
  traderProfileConnector: TraderProfileConnector,
  val controllerComponents: MessagesControllerComponents,
  view: NiphlNumberView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(NiphlNumberPage, form)
      Ok(view(preparedForm, controllers.profile.routes.NiphlNumberController.onSubmitCreate(mode)))
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] =
    (identify andThen checkProfile andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, controllers.profile.routes.NiphlNumberController.onSubmitCreate(mode)))
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NiphlNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(NiphlNumberPage, mode, updatedAnswers))
        )
    }

  def onPageLoadUpdate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      traderProfileConnector.getTraderProfile.flatMap { traderProfile =>
        request.userAnswers.get(HasNiphlUpdatePage) match {
          case Some(_) =>
            traderProfile.niphlNumber match {
              case None       =>
                Future.successful(Ok(view(form, controllers.profile.routes.NiphlNumberController.onSubmitUpdate(mode))))
              case Some(data) =>
                for {
                  updatedAnswers <-
                    Future.fromTry(request.userAnswers.set(NiphlNumberUpdatePage, data))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Ok(
                  view(form.fill(data), controllers.profile.routes.NiphlNumberController.onSubmitUpdate(mode))
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
                  view(form, controllers.profile.routes.NiphlNumberController.onSubmitUpdate(mode))
                )
              case Some(data) =>
                for {
                  updatedAnswersWithHasNiphl <-
                    Future.fromTry(request.userAnswers.set(HasNiphlUpdatePage, true))
                  updatedAnswers             <-
                    Future.fromTry(updatedAnswersWithHasNiphl.set(NiphlNumberUpdatePage, data))
                  _                          <- sessionRepository.set(updatedAnswers)
                } yield Ok(
                  view(form.fill(data), controllers.profile.routes.NiphlNumberController.onSubmitUpdate(mode))
                )
            }
        }
      }
    }

  def onSubmitUpdate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, controllers.profile.routes.NiphlNumberController.onSubmitUpdate(mode)))
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NiphlNumberUpdatePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(NiphlNumberUpdatePage, mode, updatedAnswers))
        )

    }

}
