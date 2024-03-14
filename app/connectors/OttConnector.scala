package connectors

import config.FrontendAppConfig
import models.ott.OttResponse
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.Results.BadRequest
import play.api.mvc.{Result, Results}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

@Singleton
class OttConnector @Inject()(http: HttpClient, appConfig: FrontendAppConfig)(implicit ec: ExecutionContext) {

  private def setHeaders(): Seq[(String, String)] = Seq(
    "Authorization" -> "Token REPLACE-ME-WITH-TOKEN-SOMEHOW"
  )

  def getGoodsNomenclatures(comcode: String)(implicit hc: HeaderCarrier): Future[Either[Result, OttResponse]] = {
    val url = s"${appConfig.ottBaseUrl}${appConfig.ottGreenLanePath}/green_lanes/goods_nomenclatures/${comcode}"
    val responseFuture: Future[HttpResponse] = http.GET[HttpResponse](url = url, headers = setHeaders())

    responseFuture.map { httpResponse =>
      httpResponse.status match {
        case OK =>
          val json = Json.parse(httpResponse.body)
          json.validate[OttResponse] match {
            case JsSuccess(ottResponse, _) =>
              Right(ottResponse)
            case JsError(errors) =>
              Left(BadRequest("Failed to parse response: " + errors.mkString(", ")))
          }
        case _ =>
          // Handle status codes in this match... At the moment just uses OTT status to respond.
          Left(Results.Status(httpResponse.status)(httpResponse.body))
      }
    }.recover {
      case exception: Exception =>
        // Handle exceptions like timeouts or whatever.
        Left(Results.InternalServerError("An error occurred: " + exception.getMessage))
    }
  }
}