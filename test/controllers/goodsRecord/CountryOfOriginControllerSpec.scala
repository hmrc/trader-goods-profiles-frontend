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

package controllers.goodsRecord

import base.SpecBase
import base.TestConstants.{testEori, testRecordId, userAnswersId}
import connectors.OttConnector
import controllers.routes
import forms.goodsRecord.CountryOfOriginFormProvider
import models.helper.GoodsDetailsUpdate
import models.{Country, NormalMode, UserAnswers}
import navigation.{FakeNavigation, Navigation}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsRecord.{CountryOfOriginPage, CountryOfOriginUpdatePage, HasCountryOfOriginChangePage}
import play.api.inject.bind
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.CountriesQuery
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.SessionData.{dataUpdated, pageUpdated}
import views.html.CountryOfOriginView

import scala.concurrent.Future

class CountryOfOriginControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")
  private val countries   = Seq(Country("CN", "China"), Country("US", "United States"))

  val formProvider = new CountryOfOriginFormProvider()
  private val form = formProvider(countries)

  "CountryOfOrigin Controller" - {

    "create journey" - {

      lazy val countryOfOriginRoute = routes.CountryOfOriginController.onPageLoadCreate(NormalMode).url
      lazy val onSubmitAction       = routes.CountryOfOriginController.onSubmitCreate(NormalMode)

      "must return OK and the correct view for a GET" in {

        val mockOttConnector = mock[OttConnector]
        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
          countries
        )

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[OttConnector].toInstance(mockOttConnector)
            )
            .build()

        running(application) {
          val request = FakeRequest(GET, countryOfOriginRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryOfOriginView]

          val call = onSubmitAction

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, call, countries)(
            request,
            messages(application)
          ).toString
          verify(mockOttConnector, atLeastOnce()).getCountries(any())
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = UserAnswers(userAnswersId).set(CountryOfOriginPage, "answer").success.value

        val mockOttConnector = mock[OttConnector]
        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
          countries
        )

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[OttConnector].toInstance(mockOttConnector)
            )
            .build()

        running(application) {
          val request = FakeRequest(GET, countryOfOriginRoute)

          val view = application.injector.instanceOf[CountryOfOriginView]

          val result = route(application, request).value

          val call = onSubmitAction

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("answer"), call, countries)(
            request,
            messages(application)
          ).toString
          verify(mockOttConnector, atLeastOnce()).getCountries(any())
        }
      }

      "must populate the view correctly on a GET when countries data is already present" in {

        val userAnswers = UserAnswers(userAnswersId).set(CountriesQuery, countries).success.value

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute))
            )
            .build()

        running(application) {
          val request = FakeRequest(GET, countryOfOriginRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryOfOriginView]

          val call = onSubmitAction

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, call, countries)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val userAnswers = UserAnswers(userAnswersId).set(CountriesQuery, countries).success.value

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, countryOfOriginRoute)
              .withFormUrlEncodedBody(("value", "CN"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must error when data is submitted and countries query is empty" in {

        val userAnswers = UserAnswers(userAnswersId)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute))
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, countryOfOriginRoute)
              .withFormUrlEncodedBody(("value", "CN"))

          intercept[Exception] {
            await(route(application, request).value)
          }
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val userAnswers = UserAnswers(userAnswersId).set(CountriesQuery, countries).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, countryOfOriginRoute)
              .withFormUrlEncodedBody(("value", "TEST"))

          val boundForm = form.bind(Map("value" -> "TEST"))

          val view = application.injector.instanceOf[CountryOfOriginView]

          val result = route(application, request).value

          val call = onSubmitAction

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, call, countries)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request = FakeRequest(GET, countryOfOriginRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request =
            FakeRequest(POST, countryOfOriginRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "update journey" - {

      lazy val countryOfOriginRoute = routes.CountryOfOriginController.onPageLoadUpdate(NormalMode, testRecordId).url
      lazy val onSubmitAction       = routes.CountryOfOriginController.onSubmitUpdate(NormalMode, testRecordId)

      "must return OK and the correct view for a GET" in {

        val mockAuditService = mock[AuditService]

        when(mockAuditService.auditStartUpdateGoodsRecord(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(Done))

        val mockOttConnector = mock[OttConnector]
        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
          countries
        )

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[OttConnector].toInstance(mockOttConnector),
              bind[AuditService].toInstance(mockAuditService)
            )
            .build()

        running(application) {
          val request = FakeRequest(GET, countryOfOriginRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryOfOriginView]

          val call = onSubmitAction

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, call, countries)(
            request,
            messages(application)
          ).toString

          withClue("must call the audit service with the correct details") {
            verify(mockAuditService, atLeastOnce())
              .auditStartUpdateGoodsRecord(
                eqTo(testEori),
                eqTo(AffinityGroup.Individual),
                eqTo(GoodsDetailsUpdate),
                eqTo(testRecordId),
                any()
              )(any())
            verify(mockOttConnector, atLeastOnce()).getCountries(any())
          }

        }
      }

      "must not fire audit event if already fired on last page" in {

        val mockAuditService = mock[AuditService]

        val mockOttConnector = mock[OttConnector]
        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
          countries
        )

        val userAnswers = emptyUserAnswers.set(HasCountryOfOriginChangePage(testRecordId), true).success.value

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[OttConnector].toInstance(mockOttConnector),
              bind[AuditService].toInstance(mockAuditService)
            )
            .build()

        running(application) {
          val request = FakeRequest(GET, countryOfOriginRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryOfOriginView]

          val call = onSubmitAction

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, call, countries)(
            request,
            messages(application)
          ).toString

          withClue("must not call the audit service as this has already been done") {
            verify(mockAuditService, never())
              .auditStartUpdateGoodsRecord(
                any(),
                any(),
                any(),
                any(),
                any()
              )(any())
            verify(mockOttConnector, atLeastOnce()).getCountries(any())
          }

        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers =
          UserAnswers(userAnswersId).set(CountryOfOriginUpdatePage(testRecordId), "answer").success.value

        val mockOttConnector = mock[OttConnector]
        when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
          countries
        )

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[OttConnector].toInstance(mockOttConnector)
            )
            .build()

        running(application) {
          val request = FakeRequest(GET, countryOfOriginRoute)

          val view = application.injector.instanceOf[CountryOfOriginView]

          val result = route(application, request).value

          val call = onSubmitAction

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill("answer"), call, countries)(
            request,
            messages(application)
          ).toString
          verify(mockOttConnector, atLeastOnce()).getCountries(any())
        }
      }

      "must populate the view correctly on a GET when countries data is already present" in {

        val userAnswers = UserAnswers(userAnswersId).set(CountriesQuery, countries).success.value

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute))
            )
            .build()

        running(application) {
          val request = FakeRequest(GET, countryOfOriginRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryOfOriginView]

          val call = onSubmitAction

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, call, countries)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val userAnswers = UserAnswers(userAnswersId).set(CountriesQuery, countries).success.value

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, countryOfOriginRoute)
              .withFormUrlEncodedBody(("value", "CN"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must set changesMade to true if country of origin is updated" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val userAnswers        = UserAnswers(userAnswersId).set(CountriesQuery, countries).success.value
        val updatedUserAnswers = userAnswers.set(CountryOfOriginUpdatePage(testRecordId), "CN").success.value

        val application =
          applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val controller = application.injector.instanceOf[CountryOfOriginController]
          val request    =
            FakeRequest(POST, countryOfOriginRoute)
              .withFormUrlEncodedBody(("value", "US"))

          val result: Future[Result] = controller.onSubmitUpdate(NormalMode, testRecordId)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          session(result).get(dataUpdated) must be(Some("true"))
          session(result).get(pageUpdated) must be(Some("country of origin"))
        }
      }

      "must set changesMade to false if country of origin is not updated" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val userAnswers        = UserAnswers(userAnswersId).set(CountriesQuery, countries).success.value
        val updatedUserAnswers = userAnswers.set(CountryOfOriginUpdatePage(testRecordId), "CN").success.value

        val application =
          applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val controller = application.injector.instanceOf[CountryOfOriginController]
          val request    =
            FakeRequest(POST, countryOfOriginRoute)
              .withFormUrlEncodedBody(("value", "CN"))

          val result: Future[Result] = controller.onSubmitUpdate(NormalMode, testRecordId)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          session(result).get(dataUpdated) must be(Some("false"))
        }
      }

      "must error when data is submitted and countries query is empty" in {

        val userAnswers = UserAnswers(userAnswersId)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute))
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, countryOfOriginRoute)
              .withFormUrlEncodedBody(("value", "CN"))

          intercept[Exception] {
            await(route(application, request).value)
          }
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val userAnswers = UserAnswers(userAnswersId).set(CountriesQuery, countries).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, countryOfOriginRoute)
              .withFormUrlEncodedBody(("value", "TEST"))

          val boundForm = form.bind(Map("value" -> "TEST"))

          val view = application.injector.instanceOf[CountryOfOriginView]

          val result = route(application, request).value

          val call = onSubmitAction

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, call, countries)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request = FakeRequest(GET, countryOfOriginRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .build()

        running(application) {
          val request =
            FakeRequest(POST, countryOfOriginRoute)
              .withFormUrlEncodedBody(("value", "answer"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
