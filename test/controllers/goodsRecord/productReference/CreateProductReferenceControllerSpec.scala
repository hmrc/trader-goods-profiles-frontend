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

package controllers.goodsRecord.productReference

import base.SpecBase
import base.TestConstants.userAnswersId
import connectors.GoodsRecordConnector
import forms.goodsRecord.ProductReferenceFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeGoodsRecordNavigator, GoodsRecordNavigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when, reset}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsRecord.ProductReferencePage
import play.api.data.FormError
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.goodsRecord.ProductReferenceView

import scala.concurrent.Future

class CreateProductReferenceControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new ProductReferenceFormProvider()
  private val form = formProvider()
  
  private val mockSessionRepository = mock[SessionRepository]
  private val mockGoodsRecordConnector = mock[GoodsRecordConnector]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionRepository, mockGoodsRecordConnector)
  }

  "productReference Controller" - {
    "for create journey" - {
      lazy val productReferenceRoute = controllers.goodsRecord.productReference.routes.CreateProductReferenceController.onPageLoad(NormalMode).url
      lazy val onSubmitAction        = controllers.goodsRecord.productReference.routes.CreateProductReferenceController.onSubmit(NormalMode)

      "must return OK and the correct view for a GET" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, productReferenceRoute)
          val result = route(application, request).value
          val view = application.injector.instanceOf[ProductReferenceView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, onSubmitAction)(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers = UserAnswers(userAnswersId).set(ProductReferencePage, "answer").success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, productReferenceRoute)
          val view = application.injector.instanceOf[ProductReferenceView]
          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("answer"), onSubmitAction)(request, messages(application)).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(mockGoodsRecordConnector.isProductReferenceUnique(any())(any())) thenReturn Future.successful(true)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

        running(application) {
          val request = FakeRequest(POST, productReferenceRoute).withFormUrlEncodedBody(("value", "answer"))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockSessionRepository).set(any())
          verify(mockGoodsRecordConnector).isProductReferenceUnique(any())(any())
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, productReferenceRoute).withFormUrlEncodedBody(("value", ""))
          val boundForm = form.bind(Map("value" -> ""))
          val view = application.injector.instanceOf[ProductReferenceView]
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, onSubmitAction)(request, messages(application)).toString
        }
      }

      "must return a Bad Request and errors when an existing data is submitted" in {
        when(mockGoodsRecordConnector.isProductReferenceUnique(any())(any())) thenReturn Future.successful(false)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[GoodsRecordNavigator].toInstance(new FakeGoodsRecordNavigator(onwardRoute)),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

        running(application) {
          val request = FakeRequest(POST, productReferenceRoute).withFormUrlEncodedBody(("value", "answer"))
          val boundForm = form.fill("answer").copy(errors = Seq(elems =
                FormError("value", "This product reference is already in your TGP. Enter a unique product reference."))
          )
          val view = application.injector.instanceOf[ProductReferenceView]
          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, onSubmitAction)(request, messages(application)).toString
          verify(mockGoodsRecordConnector).isProductReferenceUnique(any())(any())
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, productReferenceRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, productReferenceRoute).withFormUrlEncodedBody(("value", "answer"))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}