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
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.ProfileSetupView

class ProfileSetupControllerSpec extends SpecBase with MockitoSugar {

  "ProfileSetup Controller" - {

    "for a GET" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.ProfileSetupController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ProfileSetupView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }
    }

    "for a POST" - {

      val onwardRoute = Call("", "")

      "must redirect to the next page when the user already has user answers" in {

        val mockSessionRepository = mock[SessionRepository]

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
            )
            .build()

        running(application) {
          val request = FakeRequest(POST, routes.ProfileSetupController.onSubmit().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          verify(mockSessionRepository, never()).set(any())
        }
      }

//      "must create user answers then redirect to the next page when the user does not have answers" in {
//
//        val mockSessionRepository = mock[SessionRepository]
//        val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
//        when(mockSessionRepository.set(captor.capture())).thenReturn(Future.successful(true))
//
//        val application =
//          applicationBuilder(userAnswers = None)
//            .overrides(
//              bind[SessionRepository].toInstance(mockSessionRepository),
//              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
//      bind[DataRetrievalOrCreateAction].toInstance(new FakeDataRetrievalOrCreateAction(userAnswers.value)),
//
//                  )
//            .build()
//
//        running(application) {
//          val request = FakeRequest(POST, routes.ProfileSetupController.onSubmit().url)
//
//          val result = route(application, request).value
//
//          status(result) mustEqual SEE_OTHER
//          redirectLocation(result).value mustEqual onwardRoute.url
//
//          verify(mockSessionRepository, times(1)).set(any())
//
//          val savedAnswers = captor.getValue
//          savedAnswers.id mustEqual "id"
//          savedAnswers.data mustEqual Json.obj()
//        }
//      }
    }
  }
}
