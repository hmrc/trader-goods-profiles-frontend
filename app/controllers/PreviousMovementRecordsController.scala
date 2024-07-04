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

import connectors.GoodsRecordConnector
import controllers.actions._
import pages.PreviousMovementRecordsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.GetGoodsRecordsQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{GoodsRecordsEmptyView, PreviousMovementRecordsView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviousMovementRecordsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: PreviousMovementRecordsView,
  emptyView: GoodsRecordsEmptyView,
  getGoodsRecordConnector: GoodsRecordConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    getGoodsRecordConnector.doRecordsExist(request.eori).map {
      case Some(getGoodsRecordResponse) if getGoodsRecordResponse.goodsItemRecords.nonEmpty =>
        Ok(view())

      case _ =>
        Redirect(routes.GoodsRecordsController.onPageLoadNoRecords())
    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      getGoodsRecordResponse  <- getGoodsRecordConnector.getAllRecords(request.eori)
      updatedAnswersWithQuery <- Future.fromTry(request.userAnswers.set(GetGoodsRecordsQuery, getGoodsRecordResponse))
      updatedAnswersWithFlag  <-
        Future.fromTry(updatedAnswersWithQuery.set(PreviousMovementRecordsPage, "hasLoadedRecords"))
      _                       <- sessionRepository.set(updatedAnswersWithFlag)

    } yield Redirect(routes.GoodsRecordsController.onPageLoad(1))

  }
}
