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

package controllers.advice

import connectors.AccreditationConnector
import controllers.BaseController
import controllers.actions._
import forms.advice.ReasonForWithdrawAdviceFormProvider
import models.NormalMode
import models.helper.WithdrawAdviceJourney
import navigation.AdviceNavigator
import pages.advice.ReasonForWithdrawAdvicePage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditService, DataCleansingService}
import views.html.advice.ReasonForWithdrawAdviceView

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class ReasonForWithdrawAdviceController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: AdviceNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: ReasonForWithdrawAdviceFormProvider,
  auditService: AuditService,
  accreditationConnector: AccreditationConnector,
  dataCleansingService: DataCleansingService,
  val controllerComponents: MessagesControllerComponents,
  view: ReasonForWithdrawAdviceView
)(implicit @unused ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(ReasonForWithdrawAdvicePage(recordId), form)
      Ok(view(preparedForm, recordId))
    }

  def onSubmit(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, recordId))),
          value => {
            val withdrawReason: Option[String] = if (value.trim.nonEmpty) Some(value) else None
            auditService.auditWithdrawAdvice(request.affinityGroup, request.eori, recordId, withdrawReason)
            for {
              _ <- accreditationConnector.withdrawRequestAccreditation(recordId, withdrawReason)
              _ <- dataCleansingService.deleteMongoData(request.userAnswers.id, WithdrawAdviceJourney)

            } yield Redirect(navigator.nextPage(ReasonForWithdrawAdvicePage(recordId), NormalMode, request.userAnswers))
          }
        )
    }
}
