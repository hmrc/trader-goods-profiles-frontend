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
import base.TestConstants.{testEori, testRecordId}
import models.helper.CategorisationUpdate
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject._
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.CategoryGuidanceView

import scala.concurrent.Future

class CategoryGuidanceControllerSpec extends SpecBase {

  private val onwardRoute = Call("GET", "/foo")

  "CategoryGuidance Controller" - {

    "onPageLoad should display view" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(testRecordId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CategoryGuidanceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(testRecordId)(request, messages(application)).toString
      }
    }

    "onSubmit should call navigator to redirect" in {

      val mockAuditService = mock[AuditService]

      when(mockAuditService.auditStartUpdateGoodsRecord(any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))

      val application = applicationBuilder(userAnswers = Some(userAnswersForCategorisation))
        .overrides(
          bind[AuditService].toInstance(mockAuditService),
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CategoryGuidanceController.onSubmit(testRecordId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        withClue("must call the audit service with the correct details") {
          verify(mockAuditService)
            .auditStartUpdateGoodsRecord(
              eqTo(testEori),
              eqTo(AffinityGroup.Individual),
              eqTo(CategorisationUpdate),
              eqTo(testRecordId),
              eqTo(Some(categorisationInfo))
            )(any())
        }
      }
    }

  }

}
