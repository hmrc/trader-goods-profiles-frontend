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

package controllers.commodityCodeResult

import base.SpecBase
import config.FrontendAppConfig
import forms.HasCorrectGoodsFormProvider
import models.{Commodity, NormalMode}
import navigation.{FakeNavigation, Navigation}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.CommodityQuery
import repositories.SessionRepository
import views.html.HasCorrectGoodsView

import java.time.Instant
import scala.concurrent.Future

class CreateCommodityCodeResultControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new HasCorrectGoodsFormProvider()
  private val form = formProvider()

  "CreateCommodityCodeResultController" - {

    "For create journey" - {
      lazy val hasCorrectGoodsCreateRoute =
        controllers.commodityCodeResult.routes.CreateCommodityCodeResultController.onPageLoad(NormalMode).url
      lazy val onSubmitAction: Call       =
        controllers.commodityCodeResult.routes.CreateCommodityCodeResultController.onSubmit(NormalMode)
      val page: QuestionPage[Boolean]     = HasCorrectGoodsPage

      "must return OK and the correct view for a GET" in {

        val userAnswers =
          emptyUserAnswers
            .set(CommodityQuery, Commodity("654321", List("Description", "Other"), Instant.now, None))
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request                      = FakeRequest(GET, hasCorrectGoodsCreateRoute)
          val appConfig: FrontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            Commodity("654321", List("Description", "Other"), Instant.now, None),
            onSubmitAction,
            NormalMode,
            None
          )(
            request,
            messages(application),
            appConfig
          ).toString
        }
      }

      "must redirect on GET to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsCreateRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val commodity   = Commodity("654321", List("Description"), Instant.now, None)
        val userAnswers = emptyUserAnswers
          .set(CommodityQuery, commodity)
          .success
          .value
          .set(page, true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsCreateRoute)

          val view                         = application.injector.instanceOf[HasCorrectGoodsView]
          val appConfig: FrontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), commodity, onSubmitAction, NormalMode, None)(
            request,
            messages(application),
            appConfig
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsCreateRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must redirect on POST to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsCreateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val commodity = Commodity("654321", List("Description"), Instant.now, None)

        val userAnswers =
          emptyUserAnswers.set(CommodityQuery, commodity).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsCreateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view                         = application.injector.instanceOf[HasCorrectGoodsView]
          val appConfig: FrontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, commodity, onSubmitAction, NormalMode, None)(
            request,
            messages(application),
            appConfig
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsCreateRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsCreateRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

  }

}
