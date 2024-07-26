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
import base.TestConstants.{testRecordId, userAnswersId}
import connectors.GoodsRecordConnector
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{CommodityCodeUpdatePage, CountryOfOriginUpdatePage, GoodsDescriptionUpdatePage, TraderReferenceUpdatePage}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException
import play.api.inject.bind
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import utils.SessionData.{dataUpdated, pageUpdated}

import java.time.Instant
import scala.concurrent.Future
import viewmodels.govuk.summarylist._
import viewmodels.checkAnswers.{AdviceStatusSummary, CategorySummary, CommodityCodeSummary, CountryOfOriginSummary, GoodsDescriptionSummary, HasSupplementaryUnitSummary, StatusSummary, SupplementaryUnitSummary, TraderReferenceSummary}
import views.html.SingleRecordView

class SingleRecordControllerSpec extends SpecBase with MockitoSugar {

  private lazy val singleRecordRoute   = routes.SingleRecordController.onPageLoad(testRecordId).url
  private val mockGoodsRecordConnector = mock[GoodsRecordConnector]
  private val mockSessionRepository    = mock[SessionRepository]

  private val record = goodsRecordResponse(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId)

  private val recordWithSupplementaryUnit = goodsRecordResponseWithSupplementaryUnit(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId)

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
        .set(CommodityCodeUpdatePage(testRecordId), record.comcode)
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
          CommodityCodeSummary.row(record.comcode, testRecordId, NormalMode),
          StatusSummary.row(record.declarable)
        )
      )

      val categorisationList = SummaryListViewModel(
        rows = Seq(
          CategorySummary.row(record.category.toString, testRecordId)
        )
      )

      val supplementaryUnitList = SummaryListViewModel(
        rows = Seq(
          HasSupplementaryUnitSummary.row(record.supplementaryUnit, record.measurementUnit, testRecordId),
          SupplementaryUnitSummary
            .row(record.supplementaryUnit, record.measurementUnit, testRecordId)
        ).flatten
      )

      val adviceList = SummaryListViewModel(
        rows = Seq(
          AdviceStatusSummary.row(record.adviceStatus, testRecordId)
        )
      )

      running(application) {
        val request = FakeRequest(GET, singleRecordRoute)

        val result = route(application, request).value

        val view                                  = application.injector.instanceOf[SingleRecordView]
        val changesMade                           = request.session.get(dataUpdated).contains("true")
        val changedPage                           = request.session.get(pageUpdated).getOrElse("")
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          testRecordId,
          detailsList,
          categorisationList,
          supplementaryUnitList,
          adviceList,
          changesMade,
          changedPage
        )(
          request,
          messages(application)
        ).toString
        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(uaCaptor.capture)

        uaCaptor.getValue.data mustEqual userAnswers.data
      }
    }

    "must return a SummaryListRow with the correct supplementary unit and measurement unit appended" in {

      val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      implicit val localMessages: Messages = messages(application)

      running(application) {
        val row = SupplementaryUnitSummary
          .row(recordWithSupplementaryUnit.supplementaryUnit, recordWithSupplementaryUnit.measurementUnit, testRecordId)
          .value

        val supplementaryValue = row.value.content match {
          case Text(innerContent) => innerContent
        }

        supplementaryValue must equal("1234.0 grams")

      }
    }

    "must return none when measurement unit is empty" in {

      val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      implicit val localMessages: Messages = messages(application)

      running(application) {
        val row = HasSupplementaryUnitSummary
          .row(None, None, testRecordId)
        row mustBe None

      }
    }

    "must show hasSupplementaryUnit two when measurement unit is not empty and supplementary unit is No" in {

      val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      implicit val localMessages: Messages = messages(application)

      running(application) {
        val row                  = HasSupplementaryUnitSummary
          .row(None, recordWithSupplementaryUnit.measurementUnit, testRecordId)
          .value
        val hasSupplementaryUnit = row.value.content match {
          case Text(innerContent) => innerContent
        }

        hasSupplementaryUnit contains "Do you want to add the supplementary unit?"

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
