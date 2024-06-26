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
import forms.HasCorrectGoodsFormProvider
import models.Mode
import navigation.Navigator
import pages.HasCorrectGoodsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.CommodityQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.HasCorrectGoodsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasCorrectGoodsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: HasCorrectGoodsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: HasCorrectGoodsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(HasCorrectGoodsPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    request.userAnswers.get(CommodityQuery) match {
      case Some(commodity) => Ok(view(preparedForm, mode, commodity))
      case None            => Redirect(routes.JourneyRecoveryController.onPageLoad().url)
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(CommodityQuery) match {
              case Some(commodity) => Future.successful(BadRequest(view(formWithErrors, mode, commodity)))
              case None            => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasCorrectGoodsPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasCorrectGoodsPage, mode, updatedAnswers))
        )
  }
}
