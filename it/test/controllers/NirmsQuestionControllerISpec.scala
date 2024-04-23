package controllers

import helpers.ItTestBase
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.Helpers.{await, defaultAwaitTimeout}

class NirmsQuestionControllerISpec extends ItTestBase {

  lazy val client: WSClient = app.injector.instanceOf[WSClient]

  private val url = s"http://localhost:$port$appRouteContext/nirms-question"

  "NIRMS question controller" should {

    "redirects you to unauthorised page when auth fails" in {
      noEnrolment

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
    }

    "loads page" in {
      authorisedUser

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.get())

      response.status mustBe OK
    }

    "redirects to dummy controller when submitting valid data" in {
      authorisedUser

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(Map("value" -> "true")))

      response.status mustBe SEE_OTHER

      redirectUrl(response) mustBe Some(routes.DummyController.onPageLoad.url)
    }

    "returns bad request when submitting no data" in {
      authorisedUser

      val request: WSRequest = client.url(url).withFollowRedirects(false)

      val response = await(request.post(""))

      response.status mustBe BAD_REQUEST
    }

  }
}
