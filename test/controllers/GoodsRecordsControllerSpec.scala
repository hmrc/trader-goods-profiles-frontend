package controllers

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.GoodsRecordsView

class GoodsRecordsControllerSpec extends SpecBase {

  "GoodsRecords Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.GoodsRecordsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GoodsRecordsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }
  }
}
