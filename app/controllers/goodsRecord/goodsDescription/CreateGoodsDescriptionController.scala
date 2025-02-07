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

package controllers.goodsRecord.goodsDescription

import controllers.BaseController
import controllers.actions._
import forms.goodsRecord.GoodsDescriptionFormProvider
import models.Mode
import navigation.GoodsRecordNavigator
import pages.goodsRecord.GoodsDescriptionPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import views.html.goodsRecord.GoodsDescriptionView

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class CreateGoodsDescriptionController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: GoodsRecordNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: GoodsDescriptionFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: GoodsDescriptionView
)(implicit @unused ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(GoodsDescriptionPage, form)

      val submitAction = controllers.goodsRecord.goodsDescription.routes.CreateGoodsDescriptionController.onSubmit(mode)

      Ok(view(preparedForm, mode, submitAction))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val submitAction = controllers.goodsRecord.goodsDescription.routes.CreateGoodsDescriptionController.onSubmit(mode)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, submitAction))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(GoodsDescriptionPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(GoodsDescriptionPage, mode, updatedAnswers))
        )
    }

}
