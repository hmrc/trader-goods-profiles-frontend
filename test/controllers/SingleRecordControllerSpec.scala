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
import connectors.{GoodsRecordConnector, TraderProfileConnector}
import models.helper.SupplementaryUnitUpdateJourney
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{CommodityCodeUpdatePage, CountryOfOriginUpdatePage, GoodsDescriptionUpdatePage, TraderReferenceUpdatePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.http.NotFoundException
import utils.SessionData.{dataRemoved, dataUpdated, pageUpdated}
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.SingleRecordView

import java.time.Instant
import scala.concurrent.Future

class SingleRecordControllerSpec extends SpecBase with MockitoSugar {

  private lazy val singleRecordRoute   = routes.SingleRecordController.onPageLoad(testRecordId).url
  private val mockGoodsRecordConnector = mock[GoodsRecordConnector]
  private val recordIsLocked           = false

  private val record = goodsRecordResponse(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId)

  private val notCategorisedRecord = goodsRecordResponse(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId).copy(category = None)

  private val recordWithSupplementaryUnit = goodsRecordResponseWithSupplementaryUnit(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId)

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
  when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)

  "SingleRecord Controller" - {

    "must return OK and the correct view for a GET and set up userAnswers when record is categorised" in {

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

      when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
        .successful(record)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future
        .successful(true)

      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      implicit val message: Messages = messages(application)

      val detailsList = SummaryListViewModel(
        rows = Seq(
          TraderReferenceSummary.row(record.traderRef, testRecordId, NormalMode, recordIsLocked),
          GoodsDescriptionSummary.row(record.goodsDescription, testRecordId, NormalMode, recordIsLocked),
          CountryOfOriginSummary.row(record.countryOfOrigin, testRecordId, NormalMode, recordIsLocked, record.category.isDefined),
          CommodityCodeSummary.row(record.comcode, testRecordId, NormalMode, recordIsLocked, record.category.isDefined),
          StatusSummary.row(record.declarable)
        )
      )

      val categorisationList = SummaryListViewModel(
        rows = Seq(
          CategorySummary.row("singleRecord.cat1", testRecordId, recordIsLocked, record.category.isDefined)
        )
      )

      val supplementaryUnitList = SummaryListViewModel(
        rows = Seq(
          HasSupplementaryUnitSummary
            .row(record.supplementaryUnit, record.measurementUnit, testRecordId, recordIsLocked),
          SupplementaryUnitSummary
            .row(record.supplementaryUnit, record.measurementUnit, testRecordId, recordIsLocked)
        ).flatten
      )

      val adviceList = SummaryListViewModel(
        rows = Seq(
          AdviceStatusSummary.row(record.adviceStatus, testRecordId, recordIsLocked)
        )
      )

      running(application) {
        val request = FakeRequest(GET, singleRecordRoute)

        val result = route(application, request).value

        val view                                  = application.injector.instanceOf[SingleRecordView]
        val changesMade                           = request.session.get(dataUpdated).contains("true")
        val changedPage                           = request.session.get(pageUpdated).getOrElse("")
        val pageRemoved                           = request.session.get(dataRemoved).contains("true")
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          testRecordId,
          detailsList,
          categorisationList,
          supplementaryUnitList,
          adviceList,
          changesMade,
          changedPage,
          pageRemoved,
          recordIsLocked
        )(
          request,
          messages(application)
        ).toString
        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(uaCaptor.capture)

        uaCaptor.getValue.data mustEqual userAnswers.data

        withClue("must cleanse the user answers data") {
          verify(mockSessionRepository).clearData(eqTo(userAnswers.id), eqTo(SupplementaryUnitUpdateJourney))
        }
      }
    }

    "must return OK and the correct view for a GET and set up userAnswers when record is not categorised" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(TraderReferenceUpdatePage(testRecordId), notCategorisedRecord.traderRef)
        .success
        .value
        .set(GoodsDescriptionUpdatePage(testRecordId), notCategorisedRecord.goodsDescription)
        .success
        .value
        .set(CountryOfOriginUpdatePage(testRecordId), notCategorisedRecord.countryOfOrigin)
        .success
        .value
        .set(CommodityCodeUpdatePage(testRecordId), notCategorisedRecord.comcode)
        .success
        .value

      val mockSessionRepository = mock[SessionRepository]

      when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
        .successful(notCategorisedRecord)

      when(mockSessionRepository.set(any())) thenReturn Future
        .successful(true)

      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      implicit val message: Messages = messages(application)

      val detailsList = SummaryListViewModel(
        rows = Seq(
          TraderReferenceSummary.row(notCategorisedRecord.traderRef, testRecordId, NormalMode,recordIsLocked),
          GoodsDescriptionSummary.row(notCategorisedRecord.goodsDescription, testRecordId, NormalMode,recordIsLocked),
          CountryOfOriginSummary
            .row(
              notCategorisedRecord.countryOfOrigin,
              testRecordId,
              NormalMode,
              recordIsLocked,
              notCategorisedRecord.category.isDefined
            ),
          CommodityCodeSummary
            .row(
              notCategorisedRecord.comcode,
              testRecordId,
              NormalMode,
              recordIsLocked,
              notCategorisedRecord.category.isDefined
            ),
          StatusSummary.row(notCategorisedRecord.declarable)
        )
      )

      val categorisationList = SummaryListViewModel(
        rows = Seq(
          CategorySummary.row("singleRecord.categoriseThisGood", testRecordId, notCategorisedRecord.category.isDefined)
        )
      )

      val supplementaryUnitList = SummaryListViewModel(
        rows = Seq(
          HasSupplementaryUnitSummary
            .row(notCategorisedRecord.supplementaryUnit, notCategorisedRecord.measurementUnit, testRecordId),
          SupplementaryUnitSummary
            .row(notCategorisedRecord.supplementaryUnit, notCategorisedRecord.measurementUnit, testRecordId)
        ).flatten
      )

      val adviceList = SummaryListViewModel(
        rows = Seq(
          AdviceStatusSummary.row(notCategorisedRecord.adviceStatus, testRecordId)
        )
      )

      running(application) {
        val request = FakeRequest(GET, singleRecordRoute)

        val result = route(application, request).value

        val view                                  = application.injector.instanceOf[SingleRecordView]
        val changesMade                           = request.session.get(dataUpdated).contains("true")
        val changedPage                           = request.session.get(pageUpdated).getOrElse("")
        val pageRemoved                           = request.session.get(dataRemoved).contains("true")
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          testRecordId,
          detailsList,
          categorisationList,
          supplementaryUnitList,
          adviceList,
          changesMade,
          changedPage,
          pageRemoved,
          recordIsLocked
        )(
          request,
          messages(application)
        ).toString
        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(uaCaptor.capture)

        uaCaptor.getValue.data mustEqual userAnswers.data

        withClue("must cleanse the user answers data") {
          verify(mockSessionRepository).clearData(eqTo(userAnswers.id), eqTo(SupplementaryUnitUpdateJourney))
        }
      }
    }

    "must return a SummaryListRow with the correct supplementary unit and measurement unit appended" in {

      val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()
      implicit val localMessages: Messages = messages(application)

      running(application) {
        val row = SupplementaryUnitSummary
          .row(
            recordWithSupplementaryUnit.supplementaryUnit,
            recordWithSupplementaryUnit.measurementUnit,
            testRecordId,
            recordIsLocked
          )
          .value

        val supplementaryValue = row.value.content match {
          case Text(innerContent) => innerContent
        }

        supplementaryValue must equal("1234567890.123456 grams")

      }
    }

    "must return none when measurement unit is empty" in {

      val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()
      implicit val localMessages: Messages = messages(application)

      running(application) {
        val row = HasSupplementaryUnitSummary
          .row(None, None, testRecordId, recordIsLocked)
        row mustBe None

      }
    }

    "must show hasSupplementaryUnit two when measurement unit is not empty and supplementary unit is No" in {

      val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
        .build()
      implicit val localMessages: Messages = messages(application)

      running(application) {
        val row                  = HasSupplementaryUnitSummary
          .row(None, recordWithSupplementaryUnit.measurementUnit, testRecordId, recordIsLocked)
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
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
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

    "TraderReferenceSummary.row" - {

      "must return a SummaryListRow without change links when record is locked" in {

        val recordLocked = true

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = TraderReferenceSummary.row(record.traderRef, testRecordId, NormalMode, recordLocked)

          row.actions mustBe Some(Actions("", List()))
        }
      }

      "must return a SummaryListRow with change links when record is not locked" in {

        val recordLocked = false

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[TraderProfileConnector].toInstance(mockTraderProfileConnector))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = TraderReferenceSummary.row(record.traderRef, testRecordId, NormalMode, recordLocked)

          row.actions mustBe defined
          row.actions.value.items.head.href mustEqual routes.TraderReferenceController
            .onPageLoadUpdate(NormalMode, testRecordId)
            .url
        }
      }
    }

    "GoodsDescriptionSummary.row" - {

      "must return a SummaryListRow without change links when record is locked" in {

        val recordLocked = true

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = GoodsDescriptionSummary.row(record.goodsDescription, testRecordId, NormalMode, recordLocked)

          row.actions mustBe Some(Actions("", List()))
        }
      }

      "must return a SummaryListRow with change links when record is not locked" in {

        val recordLocked = false

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = GoodsDescriptionSummary.row(record.goodsDescription, testRecordId, NormalMode, recordLocked)

          row.actions mustBe defined
          row.actions.value.items.head.href mustEqual routes.GoodsDescriptionController
            .onPageLoadUpdate(NormalMode, testRecordId)
            .url
        }
      }
    }

    "CountryOfOriginSummary.row" - {

      "must return a SummaryListRow without change links when record is locked" in {

        val recordLocked = true

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = CountryOfOriginSummary.row(record.countryOfOrigin, testRecordId, NormalMode, recordLocked)

          row.actions mustBe Some(Actions("", List()))
        }
      }

      "must return a SummaryListRow with change links when record is not locked" in {

        val recordLocked = false

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = CountryOfOriginSummary.row(record.countryOfOrigin, testRecordId, NormalMode, recordLocked)

          row.actions mustBe defined
          row.actions.value.items.head.href mustEqual routes.HasCountryOfOriginChangeController
            .onPageLoad(NormalMode, testRecordId)
            .url
        }
      }
    }

    "CommodityCodeSummary.row" - {

      "must return a SummaryListRow without change links when record is locked" in {

        val recordLocked = true

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = CommodityCodeSummary.row(record.comcode, testRecordId, NormalMode, recordLocked)

          row.actions mustBe Some(Actions("", List()))
        }
      }

      "must return a SummaryListRow with change links when record is not locked" in {

        val recordLocked = false

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = CommodityCodeSummary.row(record.comcode, testRecordId, NormalMode, recordLocked)

          row.actions mustBe defined
          row.actions.value.items.head.href mustEqual routes.HasCommodityCodeChangeController
            .onPageLoad(NormalMode, testRecordId)
            .url
        }
      }
    }

    "CategorySummary.row" - {

      "must return a SummaryListRow without change links when record is locked" in {

        val recordLocked = true

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = CategorySummary.row(record.category.toString, testRecordId, recordLocked)

          row.actions mustBe Some(Actions("", List()))
        }
      }

      "must return a SummaryListRow with change links when record is not locked" in {

        val recordLocked = false

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = CategorySummary.row(record.category.toString, testRecordId, recordLocked)

          row.actions mustBe defined
          row.actions.value.items.head.href mustEqual routes.CategoryGuidanceController
            .onPageLoad(testRecordId)
            .url
        }
      }
    }

    "HasSupplementaryUnitSummary.row" - {

      "must return a SummaryListRow without change links when record is locked" in {

        val recordLocked = true

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = HasSupplementaryUnitSummary.row(
            recordWithSupplementaryUnit.supplementaryUnit,
            recordWithSupplementaryUnit.measurementUnit,
            testRecordId,
            recordLocked
          )

          row.get.actions mustBe Some(Actions("", List()))
        }
      }

      "must return a SummaryListRow with change links when record is not locked" in {

        val recordLocked = false

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = HasSupplementaryUnitSummary.row(
            recordWithSupplementaryUnit.supplementaryUnit,
            recordWithSupplementaryUnit.measurementUnit,
            testRecordId,
            recordLocked
          )

          row.get.actions mustBe defined
          row.get.actions.value.items.head.href mustEqual routes.HasSupplementaryUnitController
            .onPageLoadUpdate(NormalMode, testRecordId)
            .url
        }
      }
    }

    "SupplementaryUnitSummary.row" - {

      "must return a SummaryListRow without change links when record is locked" in {

        val recordLocked = true

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = SupplementaryUnitSummary.row(
            recordWithSupplementaryUnit.supplementaryUnit,
            recordWithSupplementaryUnit.measurementUnit,
            testRecordId,
            recordLocked
          )

          row.get.actions mustBe Some(Actions("", List()))
        }
      }

      "must return a SummaryListRow with change links when record is not locked" in {

        val recordLocked = false

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = SupplementaryUnitSummary.row(
            recordWithSupplementaryUnit.supplementaryUnit,
            recordWithSupplementaryUnit.measurementUnit,
            testRecordId,
            recordLocked
          )

          row.get.actions mustBe defined
          row.get.actions.value.items.head.href mustEqual routes.SupplementaryUnitController
            .onPageLoadUpdate(NormalMode, testRecordId)
            .url
        }
      }
    }

    "AdviceStatusSummary.row" - {

      "must return a SummaryListRow with change link action as withdraw advice when record is locked" in {

        val recordLocked = true

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = AdviceStatusSummary.row(record.adviceStatus, testRecordId, recordLocked)

          row.actions mustBe defined
          row.actions.value.items.head.href mustEqual routes.WithdrawAdviceStartController.onPageLoad(testRecordId).url
        }
      }

      "must return a SummaryListRow with change link action as advice when record is locked" in {

        val recordLocked = false

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = AdviceStatusSummary.row(record.adviceStatus, testRecordId, recordLocked)

          row.actions mustBe defined
          row.actions.value.items.head.href mustEqual routes.AdviceStartController.onPageLoad(testRecordId).url
        }
      }
    }

  }
}
