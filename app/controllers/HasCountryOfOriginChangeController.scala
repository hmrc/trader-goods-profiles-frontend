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
import forms.HasCountryOfOriginChangeFormProvider

import javax.inject.Inject
import models.Mode
import models.helper.GoodsDetailsUpdate
import navigation.Navigator
import pages.HasCountryOfOriginChangePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.SessionData.{dataRemoved, dataUpdated, pageUpdated}
import views.html.HasCountryOfOriginChangeView

import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class HasCountryOfOriginChangeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  auditService: AuditService,
  formProvider: HasCountryOfOriginChangeFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: HasCountryOfOriginChangeView
)(implicit @unused ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(HasCountryOfOriginChangePage(recordId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, recordId)).removingFromSession(dataRemoved, dataUpdated, pageUpdated)
    }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, recordId))),
          value => {
            if (value) {
              auditService
                .auditStartUpdateGoodsRecord(
                  request.eori,
                  request.affinityGroup,
                  GoodsDetailsUpdate,
                  recordId
                )
            }

            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasCountryOfOriginChangePage(recordId), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasCountryOfOriginChangePage(recordId), mode, updatedAnswers))
          }
        )
    }
}
