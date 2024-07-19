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
import models.Scenario

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.RecategorisingQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CategorisationResultView

import scala.concurrent.{ExecutionContext, Future}

class CategorisationResultController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CategorisationResultView,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(recordId: String, scenario: Scenario): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      if (request.userAnswers.get(RecategorisingQuery(recordId)).isDefined) {
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.remove(RecategorisingQuery(recordId)))
          _              <- sessionRepository.set(updatedAnswers)
        } yield updatedAnswers
      }
      Ok(view(recordId, scenario))
    }
}
