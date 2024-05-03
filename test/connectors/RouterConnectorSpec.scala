package connectors

import base.SpecBase
import config.FrontendAppConfig
import models.router.responses.SetUpProfileResponse
import models.{Eori, ServiceDetails}
import org.mockito.ArgumentMatchers.{any, anyInt}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout, status}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future

class RouterConnectorSpec extends SpecBase{
  private val appConfig = mock[FrontendAppConfig]
  private val httpClient = mock[HttpClientV2]
  private val requestBuilder = mock[RequestBuilder]

  private val routerConnector = new RouterConnector(appConfig,httpClient)

  "Router Connector" - {
    "setUpProfile" - {
      "must return OK" in {
        when(appConfig.tgpRouter).thenReturn(ServiceDetails("http", "host", 1))
        when(httpClient.put(any())(any())).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(HttpResponse(OK,
          """
        {
         "EORI": "eori",
         "ukimsNumber": "ukims",
         "nirmsNumber": "nirms",
         "niphlNumber": "niphl"
        }
        """)))
        val result = await(routerConnector.setUpProfile(Eori("eori")))
        val expectedResponse = SetUpProfileResponse("eori", "ukims", "nirms", "niphl")

        result mustBe expectedResponse

      }
    }

  }

}
