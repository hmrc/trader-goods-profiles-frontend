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
import forms.RemoveNirmsFormProvider

import javax.inject.Inject
import models.{NormalMode, TraderProfile}
import navigation.Navigator
import pages.{CyaMaintainProfilePage, RemoveNirmsPage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import views.html.RemoveNirmsView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class RemoveNirmsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: RemoveNirmsFormProvider,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  traderProfileConnector: TraderProfileConnector,
  view: RemoveNirmsView,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(RemoveNirmsPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm))
  }

  def onSubmit: Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          value =>
            request.userAnswers.set(RemoveNirmsPage, value) match {
              case Success(answers) =>
                sessionRepository.set(answers).map { value =>
                  if (value) {
//                    traderProfileConnector.getTraderProfile(request.eori).flatMap { traderProfile =>
//                      TraderProfile.buildNirms(answers, request.eori, traderProfile) match {
//                        case Right(model) =>
//                          auditService.auditMaintainProfile(traderProfile, model, request.affinityGroup)
//
//                          for {
//                            _ <- traderProfileConnector.submitTraderProfile(model, request.eori)
//                          } yield Redirect(navigator.nextPage(RemoveNirmsPage, NormalMode, answers))
//                        case Left(errors) =>
//                          val errorMessage = "Unable to update Trader profile."
//                          val continueUrl  = routes.HasNirmsController.onPageLoadUpdate
//                          Future.successful(logErrorsAndContinue(errorMessage, continueUrl, errors))
//                      }
                    Redirect(navigator.nextPage(RemoveNirmsPage, NormalMode, answers))

                  } else {
                    Redirect(navigator.nextPage(RemoveNirmsPage, NormalMode, answers))
                  }
                }
            }
        )
  }

}
