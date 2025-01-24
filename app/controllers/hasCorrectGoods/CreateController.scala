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

package controllers.hasCorrectGoods

import controllers.BaseController
import controllers.actions._
import forms.HasCorrectGoodsFormProvider
import models.Mode
import navigation.Navigation
import pages._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries._
import repositories.SessionRepository
import services.AuditService
import views.html.HasCorrectGoodsView

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class CreateController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigation,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: HasCorrectGoodsFormProvider,
  auditService: AuditService,
  val controllerComponents: MessagesControllerComponents,
  view: HasCorrectGoodsView
)(implicit @unused ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(HasCorrectGoodsPage, form)
      val submitAction = controllers.hasCorrectGoods.routes.CreateController.onSubmit(mode)
      request.userAnswers.get(CommodityQuery) match {
        case Some(commodity) => Ok(view(preparedForm, commodity, submitAction, mode, recordId = None))
        case None            => navigator.journeyRecovery()
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val submitAction = controllers.hasCorrectGoods.routes.CreateController.onSubmit(mode)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(CommodityQuery) match {
              case Some(commodity) =>
                Future.successful(BadRequest(view(formWithErrors, commodity, submitAction, mode, recordId = None)))
              case None            => Future.successful(navigator.journeyRecovery())
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasCorrectGoodsPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasCorrectGoodsPage, mode, updatedAnswers))
        )
    }

}
