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
import base.TestConstants.{testEori, testRecordId, userAnswersId}
import connectors.GoodsRecordConnector
import models.{NormalMode, UserAnswers}
import models.router.responses.GetGoodsRecordResponse
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{CommodityCodeUpdatePage, CountryOfOriginUpdatePage, GoodsDescriptionUpdatePage, TraderReferenceUpdatePage}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException
import play.api.inject.bind
import repositories.SessionRepository

import java.time.Instant
import scala.concurrent.Future
import viewmodels.govuk.summarylist._
import viewmodels.checkAnswers.{AdviceStatusSummary, CategorySummary, CommodityCodeSummary, CountryOfOriginSummary, GoodsDescriptionSummary, StatusSummary, TraderReferenceSummary}
import views.html.SingleRecordView

class SingleRecordControllerSpec extends SpecBase with MockitoSugar {

  private lazy val singleRecordRoute   = routes.SingleRecordController.onPageLoad(testRecordId).url
  private val mockGoodsRecordConnector = mock[GoodsRecordConnector]
  private val mockSessionRepository    = mock[SessionRepository]

  private val record = GetGoodsRecordResponse(
    testRecordId,
    "10410100",
    "EC",
    "BAN0010011",
    "Organic bananas",
    "Not requested",
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z"),
    "Not ready",
    1
  )

  "SingleRecord Controller" - {

    "must return OK and the correct view for a GET and set up userAnswers" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(TraderReferenceUpdatePage(testRecordId), record.traderRef)
        .success
        .value
        .set(GoodsDescriptionUpdatePage(testRecordId), record.goodsDescription)
        .success
        .value
        .set(CountryOfOriginUpdatePage(testRecordId), record.countryOfOrigin)
        .success
        .value
        .set(CommodityCodeUpdatePage(testRecordId), record.commodityCode)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
        .successful(record)

      when(mockSessionRepository.set(any())) thenReturn Future
        .successful(true)

      implicit val message: Messages = messages(application)

      val detailsList = SummaryListViewModel(
        rows = Seq(
          TraderReferenceSummary.row(record.traderRef, testRecordId, NormalMode),
          GoodsDescriptionSummary.row(record.goodsDescription, testRecordId, NormalMode),
          CountryOfOriginSummary.row(record.countryOfOrigin, testRecordId, NormalMode),
          CommodityCodeSummary.row(record.commodityCode, testRecordId, NormalMode),
          StatusSummary.row(record.declarable)
        )
      )

      val categorisationList = SummaryListViewModel(
        rows = Seq(
          CategorySummary.row(record.category.toString, testRecordId)
        )
      )

      val adviceList = SummaryListViewModel(
        rows = Seq(
          AdviceStatusSummary.row(record.adviceStatus, testRecordId)
        )
      )

      running(application) {
        val request = FakeRequest(GET, singleRecordRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SingleRecordView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(testRecordId, detailsList, categorisationList, adviceList)(
          request,
          messages(application)
        ).toString
        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(uaCaptor.capture)

        uaCaptor.getValue.data mustEqual userAnswers.data
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector)
        )
        .build()

      when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
        .failed(new NotFoundException("Failed to find record"))

      running(application) {
        val request = FakeRequest(GET, singleRecordRoute)

        val result = route(application, request).value

        intercept[Exception] {
          await(result)
        }
      }
    }

  }
}
