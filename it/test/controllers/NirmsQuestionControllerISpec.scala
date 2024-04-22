package controllers

import helpers.ItTestBase
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.Helpers.{await, defaultAwaitTimeout}

class NirmsQuestionControllerISpec extends ItTestBase {

  lazy val client: WSClient = app.injector.instanceOf[WSClient]

  private val url = s"http://localhost:$port$appRouteContext/nirms-question"

  "Nirms question controller" should {

    "returns an error when auth fails" in {
      noEnrolment

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe SEE_OTHER

    }

    "loads page" in {
      authorisedUser

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe OK

    }

    "returns redirect when submitting valid data" in {
      authorisedUser

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(Map("value" -> "true")))

      response.status mustBe SEE_OTHER

    }

    "returns bad request when submitting no data" in {
      authorisedUser

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(""))

      response.status mustBe BAD_REQUEST

    }

  }


}
