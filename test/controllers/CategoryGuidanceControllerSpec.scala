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
import base.TestConstants.testEori
import models.helper.CategorisationUpdate
import models.{Commodity, NormalMode}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.CommodityQuery
import repositories.SessionRepository
import services.{AuditService, CategorisationService}
import uk.gov.hmrc.auth.core.AffinityGroup
import views.html.CategoryGuidanceView

import java.time.Instant
import scala.concurrent.Future

class CategoryGuidanceControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val userAnswersWithCommodity = emptyUserAnswers
    .set(
      CommodityQuery,
      Commodity(commodityCode = "123", descriptions = List("test commodity"), Instant.now, None)
    )
    .success
    .value

  private val categorisationService = mock[CategorisationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(categorisationService.requireCategorisation(any(), any())(any())).thenReturn(
      Future.successful(emptyUserAnswers)
    )
  }

  override def afterEach(): Unit = {
    super.afterEach()
    reset(categorisationService)
  }

  "CategoryGuidance Controller" - {

    val recordId = "test-record-id"

    "must call category guidance service to load and save ott info" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCommodity))
        .overrides(
          bind[CategorisationService].toInstance(categorisationService),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {

        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(recordId).url)
        val result  = route(application, request).value

        status(result) mustEqual OK
        verify(categorisationService, times(1)).requireCategorisation(any(), any())(any())

      }
    }

    "must redirect to Journey Recover when no commodity query has been provided" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[CategorisationService].toInstance(categorisationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(recordId).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(categorisationService, never()).requireCategorisation(any(), any())(any())
      }
    }

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCommodity))
        .overrides(
          bind[CategorisationService].toInstance(categorisationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CategoryGuidanceController.onPageLoad(recordId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CategoryGuidanceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(recordId)(request, messages(application)).toString
      }
    }

    "must redirect to the categorisation page when the user click continue button" in {

      val mockAuditService = mock[AuditService]

      when(mockAuditService.auditStartUpdateGoodsRecord(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCommodity))
        .overrides(
          bind[CategorisationService].toInstance(categorisationService),
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CategoryGuidanceController.onSubmit(recordId).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.AssessmentController.onPageLoad(NormalMode, recordId, 0).url

        withClue("must call the audit service with the correct details") {
          verify(mockAuditService, times(1))
            .auditStartUpdateGoodsRecord(
              eqTo(testEori),
              eqTo(AffinityGroup.Individual),
              eqTo(CategorisationUpdate),
              eqTo(recordId)
            )(any())
        }
      }
    }
  }
}
