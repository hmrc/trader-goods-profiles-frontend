package controllers

import base.SpecBase
import controllers.actions.FakeAuthoriseAction
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.NiphlsNumberView
import forms.NiphlsNumberFormProvider

class NiphlsNumberControllerSpec extends SpecBase {

  private val formProvider = new NiphlsNumberFormProvider()

  private val niphlsNumberView = app.injector.instanceOf[NiphlsNumberView]

  private val niphlsNumberController = new NiphlsNumberController(
    stubMessagesControllerComponents(),
    new FakeAuthoriseAction(defaultBodyParser),
    niphlsNumberView,
    formProvider
  )

  "NiphlsNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val result = niphlsNumberController.onPageLoad(fakeRequest)

      status(result) mustEqual OK

      contentAsString(result) mustEqual niphlsNumberView(formProvider())(fakeRequest, stubMessages()).toString

    }

    "must redirect on Submit when user enters valid NIPHL number" in {

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "AB12345")

      val result = niphlsNumberController.onSubmit(fakeRequestWithData)

      status(result) mustEqual SEE_OTHER

      //TODO point to real next page
      redirectLocation(result) shouldBe Some(routes.DummyController.onPageLoad.url)

    }

    "must send bad request on Submit when user doesn't enter anything" in {

      val formWithErrors = formProvider().bind(Map.empty[String, String])

      val fakeRequestWithData = FakeRequest()

      val result = niphlsNumberController.onSubmit(fakeRequestWithData)

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) mustEqual niphlsNumberView(formWithErrors)(fakeRequestWithData, stubMessages()).toString

    }

    "must send bad request on Submit when user entry is invalid format" in {

      val formWithErrors = formProvider().bind(Map("value" -> "A123"))

      val fakeRequestWithData = FakeRequest().withFormUrlEncodedBody("value" -> "A123")

      val result = niphlsNumberController.onSubmit(fakeRequestWithData)

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) mustEqual niphlsNumberView(formWithErrors)(fakeRequest, stubMessages()).toString

    }
  }
}
