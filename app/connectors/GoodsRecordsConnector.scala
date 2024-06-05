package connectors

import config.Service
import models.GoodsRecord
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.OK
import play.api.libs.json.JsResult
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoodsRecordsConnector @Inject()(config: Configuration, httpClient: HttpClientV2)
                                     (implicit ec: ExecutionContext){

  private val routerBaseUrl: Service      = config.get[Service]("microservice.services.trader-goods-profiles-router")

  private def getRecordUrl(eori: String, recordId: String) =
    url"$routerBaseUrl/trader-goods-profiles-router/$eori/records/$recordId"

  def getRecord(eori: String, recordId: String)(implicit hc: HeaderCarrier): Future[GoodsRecord] = {
    httpClient
      .get(getRecordUrl(eori, recordId))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            response.json
              .validate[GoodsRecord]
              .map(result => Future.successful(result))
              .recoverTotal(error => Future.failed(JsResult.Exception(error)))
        }
      }

  }

}
