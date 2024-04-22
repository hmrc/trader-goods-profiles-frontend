package controllers

import helpers.ItTestBase
import play.api.http.Status.SEE_OTHER
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.Helpers.{await, defaultAwaitTimeout}

class NirmsQuestionControllerISpec extends ItTestBase {

  lazy val client: WSClient = app.injector.instanceOf[WSClient]

  "Nirms question controller" should {

    //TODO test if auth fails?

    "returns redirect when submitting valid data" in {

      val url = s"http://localhost:$port$appRouteContext/nirms-question"
      val request: WSRequest = client.url(url)

      val response = await(request.post(""))

      response.status mustBe SEE_OTHER

    }

  }


}
