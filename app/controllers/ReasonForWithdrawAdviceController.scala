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

import connectors.AccreditationConnector
import controllers.actions._
import forms.ReasonForWithdrawAdviceFormProvider
import models.NormalMode
import models.helper.WithdrawAdviceJourney
import navigation.Navigator
import pages.ReasonForWithdrawAdvicePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DataCleansingService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ReasonForWithdrawAdviceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReasonForWithdrawAdviceController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: ReasonForWithdrawAdviceFormProvider,
  accreditationConnector: AccreditationConnector,
  dataCleansingService: DataCleansingService,
  val controllerComponents: MessagesControllerComponents,
  view: ReasonForWithdrawAdviceView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(ReasonForWithdrawAdvicePage(recordId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

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
            for {
              _ <- accreditationConnector.withdrawRequestAccreditation(request.eori, recordId, withdrawReason)
              _ <- dataCleansingService.deleteMongoData(request.userAnswers.id, WithdrawAdviceJourney)

            } yield Redirect(navigator.nextPage(ReasonForWithdrawAdvicePage(recordId), NormalMode, request.userAnswers))
          }
        )
    }
}
