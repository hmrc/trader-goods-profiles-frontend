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

package controllers

import base.SpecBase
import base.TestConstants.testRecordId
import connectors.{GoodsRecordConnector, OttConnector}
import forms.LongerCommodityCodeFormProvider
import models.{Commodity, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.LongerCommodityCodePage
import play.api.data.FormError
import play.api.http.Status.NOT_FOUND
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CategorisationDetailsQuery, LongerCommodityQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.html.LongerCommodityCodeView

import java.time.Instant
import scala.concurrent.Future

class LongerCommodityCodeControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider                        = new LongerCommodityCodeFormProvider()
  private val form                                = formProvider()
  private val shortCommodity                      = "654321"
  private val categorisationDetailsShortCommodity = categorisationInfo.copy(commodityCode = shortCommodity)

  private def onwardRoute = Call("GET", "/foo")

  private lazy val longerCommodityCodeRoute2 =
    routes.LongerCommodityCodeController.onPageLoad(NormalMode, testRecordId).url

  "LongerCommodityCode Controller" - {

    "GET" - {
      "must return OK and the correct view" in {

        val userAnswers =
          emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationDetailsShortCommodity)
            .success
            .value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, longerCommodityCodeRoute2)

          val result = route(application, request).value

          val view = application.injector.instanceOf[LongerCommodityCodeView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, shortCommodity, testRecordId)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to JourneyRecovery Page" - {

        "if user doesn't have categorisation details answer" in {
          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, longerCommodityCodeRoute2)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }

        "if user's commodity code is 10 digits" in {
          val userAnswers =
            emptyUserAnswers.set(CategorisationDetailsQuery(testRecordId), categorisationInfo).success.value
          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, longerCommodityCodeRoute2)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }

        "if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, longerCommodityCodeRoute2)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }

      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationDetailsShortCommodity)
          .success
          .value
          .set(LongerCommodityCodePage(testRecordId), "1234")
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, longerCommodityCodeRoute2)

          val view = application.injector.instanceOf[LongerCommodityCodeView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("1234"), NormalMode, shortCommodity, testRecordId)(
            request,
            messages(application)
          ).toString
        }
      }

    }

    "POST" - {

      "must redirect to the next page when valid data is submitted" in {

        val mockGoodsRecordConnector = mock[GoodsRecordConnector]
        when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
          .thenReturn(Future.successful(goodsRecordResponse(Instant.now, Instant.now)))

        val mockSessionRepository = mock[SessionRepository]
        val mockOttConnector      = mock[OttConnector]
        val userAnswers           = emptyUserAnswers
          .set(CategorisationDetailsQuery(testRecordId), categorisationDetailsShortCommodity)
          .success
          .value
          .set(LongerCommodityCodePage(testRecordId), "8930")
          .success
          .value

        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        when(mockSessionRepository.set(uaCaptor.capture())) thenReturn Future.successful(true)

        val testCommodity = Commodity("6543211200", List("Description"), Instant.now, None)
        when(mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any(), any())(any())) thenReturn Future
          .successful(
            testCommodity
          )

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[OttConnector].toInstance(mockOttConnector),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, longerCommodityCodeRoute2)
              .withFormUrlEncodedBody(("value", "12"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          verify(mockOttConnector, atLeastOnce()).getCommodityCode(any(), any(), any(), any(), any(), any())(any())

          withClue("ensure user answers has set the new commodity query") {
            val finalUserAnswers = uaCaptor.getValue
            finalUserAnswers.get(LongerCommodityCodePage(testRecordId)).get mustBe "12"

            withClue("stored commodity must have the code entered by users with no extra zeroes") {
              finalUserAnswers.get(LongerCommodityQuery(testRecordId)).get mustBe testCommodity.copy(commodityCode =
                "65432112"
              )

            }
          }
        }
      }

      "must return a Bad Request and errors" - {

        "when invalid data is submitted" in {
          val userAnswers = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationDetailsShortCommodity)
            .success
            .value
          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request =
              FakeRequest(POST, longerCommodityCodeRoute2)
                .withFormUrlEncodedBody(("value", ""))

            val boundForm = form.bind(Map("value" -> ""))

            val view = application.injector.instanceOf[LongerCommodityCodeView]

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(boundForm, NormalMode, shortCommodity, testRecordId)(
              request,
              messages(application)
            ).toString
          }
        }

        "when correct data format but not valid commodity code" in {
          val mockGoodsRecordConnector = mock[GoodsRecordConnector]
          when(mockGoodsRecordConnector.getRecord(any(), any())(any()))
            .thenReturn(Future.successful(goodsRecordResponse(Instant.now, Instant.now)))

          val mockOttConnector = mock[OttConnector]
          val userAnswers      = emptyUserAnswers
            .set(CategorisationDetailsQuery(testRecordId), categorisationDetailsShortCommodity)
            .success
            .value

          when(
            mockOttConnector.getCommodityCode(anyString(), any(), any(), any(), any(), any())(any())
          ) thenReturn Future
            .failed(
              UpstreamErrorResponse(" ", NOT_FOUND)
            )

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[OttConnector].toInstance(mockOttConnector),
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
            )
            .build()

          running(application) {
            val request =
              FakeRequest(POST, longerCommodityCodeRoute2)
                .withFormUrlEncodedBody(("value", "1234"))

            val boundForm = form.copy(errors = Seq(elems = FormError("value", "longerCommodityCode.error.invalid")))

            val view = application.injector.instanceOf[LongerCommodityCodeView]

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(boundForm, NormalMode, shortCommodity, testRecordId)(
              request,
              messages(application)
            ).toString

            verify(mockOttConnector, atLeastOnce()).getCommodityCode(any(), any(), any(), any(), any(), any())(any())
          }

        }

      }

      "must redirect to JourneyRecovery Page" - {

        "if user doesn't have categorisation details answer" in {
          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(POST, longerCommodityCodeRoute2)
            val result  = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }

        "if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request =
              FakeRequest(POST, longerCommodityCodeRoute2)
                .withFormUrlEncodedBody(("value", "answer"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER

            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }
      }
    }
  }

}
