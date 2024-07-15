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

import controllers.actions._
import forms.TraderReferenceFormProvider

import javax.inject.Inject
import models.Mode
import models.helper.GoodsDetailsUpdate
import navigation.Navigator
import pages.{TraderReferencePage, TraderReferenceUpdatePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TraderReferenceView

import scala.concurrent.{ExecutionContext, Future}

class TraderReferenceController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  auditService: AuditService,
  formProvider: TraderReferenceFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TraderReferenceView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(TraderReferencePage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      val onSubmitAction = routes.TraderReferenceController.onSubmitCreate(mode)

      Ok(view(preparedForm, onSubmitAction))
  }

  def onPageLoadUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(TraderReferenceUpdatePage(recordId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      auditService
        .auditStartUpdateGoodsRecord(
          request.eori,
          request.affinityGroup,
          GoodsDetailsUpdate,
          recordId
        )

      val onSubmitAction = routes.TraderReferenceController.onSubmitUpdate(mode, recordId)
      Ok(view(preparedForm, onSubmitAction))
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val onSubmitAction = routes.TraderReferenceController.onSubmitCreate(mode)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, onSubmitAction))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(TraderReferencePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(TraderReferencePage, mode, updatedAnswers))
        )
  }

  def onSubmitUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction = routes.TraderReferenceController.onSubmitUpdate(mode, recordId)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, onSubmitAction))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(TraderReferenceUpdatePage(recordId), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(TraderReferenceUpdatePage(recordId), mode, updatedAnswers))
        )
    }

}
