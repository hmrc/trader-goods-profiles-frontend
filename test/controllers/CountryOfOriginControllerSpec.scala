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
import base.TestConstants.{newRecordId, userAnswersId}
import connectors.OttConnector
import forms.CountryOfOriginFormProvider
import models.{Country, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.CountryOfOriginPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.CountriesQuery
import repositories.SessionRepository
import views.html.CountryOfOriginView

import scala.concurrent.Future

class CountryOfOriginControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")
  private val countries   = Seq(Country("CN", "China"))

  val formProvider = new CountryOfOriginFormProvider()
  private val form = formProvider(countries)

  private lazy val countryOfOriginRoute = routes.CountryOfOriginController.onPageLoad(NormalMode, newRecordId).url

  "CountryOfOrigin Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockOttConnector = mock[OttConnector]
      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
        countries
      )

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[OttConnector].toInstance(mockOttConnector)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, countryOfOriginRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CountryOfOriginView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, countries, newRecordId)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(CountryOfOriginPage(newRecordId), "answer").success.value

      val mockOttConnector = mock[OttConnector]
      when(mockOttConnector.getCountries(any())) thenReturn Future.successful(
        countries
      )

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[OttConnector].toInstance(mockOttConnector)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, countryOfOriginRoute)

        val view = application.injector.instanceOf[CountryOfOriginView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode, countries, newRecordId)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when countries data is already present" in {

      val userAnswers = UserAnswers(userAnswersId).set(CountriesQuery, countries).success.value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, countryOfOriginRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CountryOfOriginView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, countries, newRecordId)(
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
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
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
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
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

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, countryOfOriginRoute)
            .withFormUrlEncodedBody(("value", "TEST"))

        val boundForm = form.bind(Map("value" -> "TEST"))

        val view = application.injector.instanceOf[CountryOfOriginView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, countries, newRecordId)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, countryOfOriginRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, countryOfOriginRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
