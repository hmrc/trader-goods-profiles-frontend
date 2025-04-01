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

package services

import connectors.{GoodsRecordConnector, OttConnector}
import models.Commodity
import models.helper.ValidateCommodityCode
import models.ott.response.ProductlineSuffix
import models.requests.DataRequest
import play.api.Logging
import play.api.http.Status.NOT_FOUND
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommodityService @Inject() (
  ottConnector: OttConnector,
  goodsRecordsConnector: GoodsRecordConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  def isCommodityCodeValid(
    recordId: String
  )(implicit request: DataRequest[AnyContent], hc: HeaderCarrier): Future[Boolean] =
    fetchRecordValues(recordId).flatMap { (commodityCode, countryOfOrigin) =>
      fetchCommodity(commodityCode, countryOfOrigin).map {
        case Some(commodity) => commodity.isValid
        case _               => false
      }
    }

  def isCommodityCodeValid(commodityCode: String, countryOfOrigin: String)(implicit
    request: DataRequest[AnyContent],
    hc: HeaderCarrier
  ): Future[Boolean] =
    fetchCommodity(commodityCode, countryOfOrigin).map {
      case Some(commodity) => commodity.isValid
      case _               => false
    }

  def fetchRecordValues(
    recordId: String
  )(implicit request: DataRequest[AnyContent], hc: HeaderCarrier): Future[(String, String)] =
    goodsRecordsConnector.getRecord(recordId).map { x =>
      (x.comcode, x.countryOfOrigin)
    }

  def fetchCommodity(
    commodityCode: String,
    countryOfOrigin: String
  )(implicit request: DataRequest[AnyContent], hc: HeaderCarrier): Future[Option[Commodity]] =
    ottConnector
      .getCommodityCode(
        commodityCode,
        request.eori,
        request.affinityGroup,
        ValidateCommodityCode,
        countryOfOrigin,
        None
      )
      .map(response => Some(response))
      .recover { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
        None
      }

  def commodityURL(commodityCode: String, countryOfOrigin: String)(implicit request: DataRequest[AnyContent], hc: HeaderCarrier): Future[String] = {
    val productlineSuffix: Future[Option[ProductlineSuffix]] = fetchCommodityProductlineSuffix(commodityCode, countryOfOrigin)

    val paddedCommodityCode: String = commodityCode.padTo(10, "0").mkString
    
    productlineSuffix.map {
      case Some(suffix) => s"$paddedCommodityCode-${suffix.productlineSuffix}"
      case None         => commodityCode
    }
  }

  def fetchCommodityProductlineSuffix(commodityCode: String, countryOfOrigin: String)(implicit
    hc: HeaderCarrier
  ): Future[Option[ProductlineSuffix]] = if (commodityCode.length == 10) {
    ottConnector.isCommodityAnEndNode(commodityCode).flatMap {
      case true  => Future.successful(None)
      case false => ottConnector.getProductlineSuffix(commodityCode, countryOfOrigin).map(Some(_))
    }
  } else {
    ottConnector.getProductlineSuffix(commodityCode, countryOfOrigin).map(Some(_))
  }
}
