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
import models.ott.CategorisationInfo

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{CategorisationQuery, CommodityQuery}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CategoryGuidanceView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CategoryGuidanceController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CategoryGuidanceView,
  ottConnector: OttConnector,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(CommodityQuery) match {
        case Some(commodity) =>
          for {
            goodsNomenclature  <- ottConnector.getCategorisationInfo(commodity.commodityCode)
            categorisationInfo <- Future.fromTry(Try(CategorisationInfo.build(goodsNomenclature).get))
            updatedAnswers     <- Future.fromTry(request.userAnswers.set(CategorisationQuery, categorisationInfo))
            _                  <- sessionRepository.set(updatedAnswers)
          } yield Ok(view())
        case None            =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
      }
  }

  // TODO replace index route
  def onSubmit: Action[AnyContent] = (identify andThen getData) { implicit request =>
    Redirect(routes.IndexController.onPageLoad.url)
  }
}
