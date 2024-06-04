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

import connectors.OttConnector
import controllers.actions._
import forms.CommodityCodeFormProvider
import models.Mode
import navigation.Navigator
import pages.CommodityCodePage
import play.api.data.FormError
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.CommodityQuery
import repositories.SessionRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CommodityCodeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommodityCodeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: CommodityCodeFormProvider,
  ottConnector: OttConnector,
  val controllerComponents: MessagesControllerComponents,
  view: CommodityCodeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(CommodityCodePage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            (for {
              commodity               <- ottConnector.getCommodityCode(value)
              updatedAnswers          <- Future.fromTry(request.userAnswers.set(CommodityCodePage, value))
              updatedAnswersWithQuery <- Future.fromTry(updatedAnswers.set(CommodityQuery, commodity))
              _                       <- sessionRepository.set(updatedAnswersWithQuery)
            } yield Redirect(navigator.nextPage(CommodityCodePage, mode, updatedAnswersWithQuery))).recover {
              case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
                val formWithApiErrors =
                  form.copy(errors = Seq(elems = FormError("value", getMessage("commodityCode.error.invalid"))))
                BadRequest(view(formWithApiErrors, mode))
            }
        )
    }

  private def getMessage(key: String)(implicit messages: Messages): String = messages(key)
}
