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
import models.helper.GoodsDetailsUpdate
import navigation.GoodsRecordNavigator
import pages.goodsRecord.{GoodsDescriptionUpdatePage, HasGoodsDescriptionChangePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import utils.SessionData._
import views.html.goodsRecord.GoodsDescriptionView

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class UpdateGoodsDescriptionController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: GoodsRecordNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: GoodsDescriptionFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: GoodsDescriptionView,
  auditService: AuditService
)(implicit @unused ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(GoodsDescriptionUpdatePage(recordId), form)

      request.userAnswers.get(HasGoodsDescriptionChangePage(recordId)) match {
        case None =>
          auditService
            .auditStartUpdateGoodsRecord(
              request.eori,
              request.affinityGroup,
              GoodsDetailsUpdate,
              recordId
            )
        case _    =>
      }

      val submitAction =
        controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController.onSubmit(mode, recordId)

      Ok(view(preparedForm, mode, submitAction)).removingFromSession(dataRemoved, dataUpdated, pageUpdated)
    }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val submitAction =
        controllers.goodsRecord.goodsDescription.routes.UpdateGoodsDescriptionController.onSubmit(mode, recordId)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, submitAction))),
          value => {
            val oldValueOpt    = request.userAnswers.get(GoodsDescriptionUpdatePage(recordId))
            val isValueChanged = oldValueOpt.exists(_ != value)
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(GoodsDescriptionUpdatePage(recordId), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(GoodsDescriptionUpdatePage(recordId), mode, updatedAnswers))
              .addingToSession(dataUpdated -> isValueChanged.toString)
              .addingToSession(pageUpdated -> goodsDescription)
          }
        )
    }

}
