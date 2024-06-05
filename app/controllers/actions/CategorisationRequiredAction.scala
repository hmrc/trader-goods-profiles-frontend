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

package controllers.actions

import connectors.{GoodsRecordsConnector, OttConnector}

import javax.inject.Inject
import controllers.routes
import models.RecordCategorisations
import models.ott.CategorisationInfo
import models.requests.{DataRequest, OptionalDataRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import queries.RecordCategorisationsQuery
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CategorisationRequiredActionImpl @Inject() (
 sessionRepository: SessionRepository,
 ottConnector: OttConnector,
 goodsRecordsConnector: GoodsRecordsConnector
) extends CategorisationRequiredAction {

  implicit var executionContext: ExecutionContext = ExecutionContext.global
  implicit var hc: HeaderCarrier = HeaderCarrier()

  var recordId = ""

  def withRecordId(recordId: String): CategorisationRequiredAction = {
    this.recordId = recordId
    this
  }

  override protected def refine[A](request: DataRequest[A]): Future[Either[Result, DataRequest[A]]] = {

    val recordCategorisations = request.userAnswers.get(RecordCategorisationsQuery).getOrElse(RecordCategorisations(Map.empty))

    recordCategorisations.records.get(recordId) match {
      case Some(categorisationInfo) =>
        Future.successful(
          Right(DataRequest(request.request, request.userId, request.eori, request.affinityGroup, request.userAnswers))
        )
      case None =>
        for {
          routerModel <- goodsRecordsConnector.getRecord(eori = request.eori, recordId = recordId)
          goodsNomenclature <- ottConnector.getCategorisationInfo(routerModel.commodityCode)
          categorisationInfo <- Future.fromTry(Try(CategorisationInfo.build(goodsNomenclature).get))
          updatedAnswers <- Future.fromTry(request.userAnswers.set(
            RecordCategorisationsQuery,
            recordCategorisations.copy(records = recordCategorisations.records + (recordId -> categorisationInfo))
          ))
          _ <- sessionRepository.set(updatedAnswers)
        } yield Right(DataRequest(request.request, request.userId, request.eori, request.affinityGroup, updatedAnswers))
    }
  }

}

trait CategorisationRequiredAction extends ActionRefiner[DataRequest, DataRequest]