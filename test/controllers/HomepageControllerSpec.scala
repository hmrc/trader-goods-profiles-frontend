package controllers

import base.SpecBase
import connectors.RouterConnector
import controllers.actions.FakeAuthoriseAction
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.OK
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status, stubMessages}
import views.html.HomepageView

import scala.concurrent.ExecutionContext.Implicits.global

class HomepageControllerSpec extends SpecBase {

  val homepageView: HomepageView = app.injector.instanceOf[HomepageView]

  //TODO remove
  val routerConnector = mock[RouterConnector]

  val homepageController = new HomepageController(
    messageComponentControllers,
    new FakeAuthoriseAction(defaultBodyParser),
    homepageView,
    routerConnector
  )

  "Homepage Controller" - {

    "must return OK and the correct view for a GET" in {

      val result = homepageController.onPageLoad()(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual homepageView()(fakeRequest, messages).toString()

    }
  }
}
