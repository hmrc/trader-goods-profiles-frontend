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

import connectors.{GoodsRecordConnector, OttConnector}
import controllers.BaseController
import controllers.actions.*
import models.Commodity
import models.helper.ValidateCommodityCode
import models.requests.DataRequest
import play.api.i18n.MessagesApi
import play.api.mvc.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidateCommodityCodeController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  ottConnector: OttConnector,
  goodsRecordConnector: GoodsRecordConnector,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController {

  def changeCategory(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      isCommodityCodeValid(recordId).map {
        case true  =>
          Redirect(controllers.categorisation.routes.CategorisationPreparationController.startCategorisation(recordId))
        case false =>
          Redirect(controllers.goodsRecord.commodityCode.routes.InvalidCommodityCodeController.onPageLoad(recordId))
      }
    }

  private def isCommodityCodeValid(recordId: String)(implicit request: DataRequest[AnyContent]): Future[Boolean] =
    fetchRecordValues(recordId).flatMap { (commodityCode, countryOfOrigin) =>
      fetchCommodity(commodityCode, countryOfOrigin).map(_.isValid)
    }

  private def fetchRecordValues(recordId: String)(implicit request: DataRequest[AnyContent]): Future[(String, String)] =
    goodsRecordConnector.getRecord(recordId).map { x =>
      (x.comcode, x.countryOfOrigin)
    }

  private def fetchCommodity(
    commodityCode: String,
    countryOfOrigin: String
  )(implicit request: DataRequest[AnyContent]): Future[Commodity] =
    ottConnector.getCommodityCode(
      commodityCode,
      request.eori,
      request.affinityGroup,
      ValidateCommodityCode,
      countryOfOrigin,
      None
    )

}
