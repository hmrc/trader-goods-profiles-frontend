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
import base.TestConstants.{testRecordId, userAnswersId}
import connectors.{GoodsRecordConnector, OttConnector, TraderProfileConnector}
import models.AdviceStatus.Requested
import models.AdviceStatusMessage.{NotRequestedParagraph, RequestedParagraph}
import models.DeclarableStatus.NotReadyForUse
import models.helper.SupplementaryUnitUpdateJourney
import models.router.responses.GetGoodsRecordResponse
import models.{Country, NormalMode, ReviewReason, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Inspectors.forAll
import org.scalatestplus.mockito.MockitoSugar
import pages.goodsRecord.{CommodityCodeUpdatePage, CountryOfOriginUpdatePage, GoodsDescriptionUpdatePage, ProductReferenceUpdatePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.AutoCategoriseService
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.http.{NotFoundException, UpstreamErrorResponse}
import utils.SessionData.{dataRemoved, dataUpdated, pageUpdated}
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.goodsRecord.{CommodityCodeSummary, CountryOfOriginSummary, GoodsDescriptionSummary, ProductReferenceSummary}
import viewmodels.govuk.summarylist.*
import views.html.goodsRecord.SingleRecordView

import java.time.Instant
import scala.concurrent.Future

class SingleRecordControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private lazy val singleRecordRoute         =
    controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url
  private lazy val singleRecordRouteLocked   =
    controllers.goodsRecord.routes.SingleRecordController.onPageLoad(lockedRecord.recordId).url
  private val mockGoodsRecordConnector       = mock[GoodsRecordConnector]
  private val mockSessionRepository          = mock[SessionRepository]
  private val mockOttConnector: OttConnector = mock[OttConnector]
  private val recordIsLocked                 = false
  private val countries                      = Seq(Country("CN", "China"), Country("US", "United States"))

  private val notCategorisedRecord = goodsRecordResponse(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId).copy(category = None)

  private val recordWithSupplementaryUnit = goodsRecordResponseWithSupplementaryUnit(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId).copy(category = Some(2))

  val mockTraderProfileConnector: TraderProfileConnector = mock[TraderProfileConnector]
  val mockAutoCategoriseService: AutoCategoriseService   = mock[AutoCategoriseService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockGoodsRecordConnector,
      mockOttConnector,
      mockSessionRepository,
      mockTraderProfileConnector,
      mockAutoCategoriseService
    )
    when(mockTraderProfileConnector.checkTraderProfile(any())(any())) thenReturn Future.successful(true)
    when(
      mockAutoCategoriseService.autoCategoriseRecord(any[GetGoodsRecordResponse](), any())(any(), any())
    ) thenReturn Future.successful(None)
    when(mockOttConnector.getCountries(any())).thenReturn(Future.successful(countries))
  }

  "SingleRecord Controller" - {

    "must return OK and the correct view for a GET and set up userAnswers when record is categorised" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(ProductReferenceUpdatePage(testRecordId), recordForTestingSummaryRows.traderRef)
        .success
        .value
        .set(GoodsDescriptionUpdatePage(testRecordId), recordForTestingSummaryRows.goodsDescription)
        .success
        .value
        .set(CountryOfOriginUpdatePage(testRecordId), recordForTestingSummaryRows.countryOfOrigin)
        .success
        .value
        .set(CommodityCodeUpdatePage(testRecordId), recordForTestingSummaryRows.comcode)
        .success
        .value

      when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future
        .successful(recordForTestingSummaryRows)

      when(mockSessionRepository.set(any())) thenReturn Future
        .successful(true)

      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[OttConnector].toInstance(mockOttConnector),
          bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
        )
        .build()

      implicit val message: Messages = messages(application)

      val detailsList = SummaryListViewModel(
        rows = Seq(
          ProductReferenceSummary.row(recordForTestingSummaryRows.traderRef, testRecordId, NormalMode, recordIsLocked),
          GoodsDescriptionSummary.rowUpdate(recordForTestingSummaryRows, testRecordId, NormalMode, recordIsLocked),
          CountryOfOriginSummary
            .rowUpdate(recordForTestingSummaryRows, testRecordId, NormalMode, recordIsLocked, countries),
          CommodityCodeSummary.rowUpdate(
            recordForTestingSummaryRows,
            testRecordId,
            NormalMode,
            recordIsLocked,
            isReviewReasonCommodity = false
          )
        )
      )

      val categorisationList = SummaryListViewModel(
        rows = Seq(
          CategorySummary
            .row(
              "singleRecord.cat1",
              testRecordId,
              recordIsLocked,
              recordForTestingSummaryRows.category.isDefined,
              recordForTestingSummaryRows.reviewReason
            )
        )
      )

      val supplementaryUnitList = SummaryListViewModel(
        rows = Seq(
          HasSupplementaryUnitSummary
            .row(
              recordForTestingSummaryRows,
              testRecordId,
              recordIsLocked
            ),
          SupplementaryUnitSummary
            .row(
              Some(2),
              recordForTestingSummaryRows.supplementaryUnit,
              recordForTestingSummaryRows.measurementUnit,
              testRecordId,
              recordIsLocked
            )
        ).flatten
      )

      val adviceList = SummaryListViewModel(
        rows = Seq(
          AdviceStatusSummary.row(
            recordForTestingSummaryRows.adviceStatus,
            testRecordId,
            recordIsLocked,
            isReviewReasonCommodity = false
          )
        )
      )

      running(application) {
        val request = FakeRequest(GET, singleRecordRoute)

        val result                                = route(application, request).value
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
          recordIsLocked,
          Some(NotRequestedParagraph),
          NotReadyForUse,
          toReview = false,
          isCategorised = recordForTestingSummaryRows.category.isDefined,
          recordForTestingSummaryRows.adviceStatus,
          None,
          request.headers
            .get("Referer")
            .getOrElse(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(1).url),
          None
        )(
          request,
          messages(application)
        ).toString
        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, times(2)).set(uaCaptor.capture)

        uaCaptor.getValue.data mustEqual userAnswers.data

        withClue("must cleanse the user answers data") {
          verify(mockSessionRepository).clearData(eqTo(userAnswers.id), eqTo(SupplementaryUnitUpdateJourney))
        }
      }
    }
    "must return OK and the correct view for a GET and set up userAnswers when record is categorised and is locked" in {

      val recordIsLocked = true
      val userAnswers    = UserAnswers(userAnswersId)
        .set(ProductReferenceUpdatePage(lockedRecord.recordId), lockedRecord.traderRef)
        .success
        .value
        .set(GoodsDescriptionUpdatePage(lockedRecord.recordId), lockedRecord.goodsDescription)
        .success
        .value
        .set(CountryOfOriginUpdatePage(lockedRecord.recordId), lockedRecord.countryOfOrigin)
        .success
        .value
        .set(CommodityCodeUpdatePage(lockedRecord.recordId), lockedRecord.comcode)
        .success
        .value

      when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future
        .successful(lockedRecord)

      when(mockSessionRepository.set(any())) thenReturn Future
        .successful(true)

      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[OttConnector].toInstance(mockOttConnector),
          bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
        )
        .build()

      implicit val message: Messages = messages(application)

      val detailsList = SummaryListViewModel(
        rows = Seq(
          ProductReferenceSummary.row(lockedRecord.traderRef, lockedRecord.recordId, NormalMode, recordIsLocked),
          GoodsDescriptionSummary.rowUpdate(lockedRecord, lockedRecord.recordId, NormalMode, recordIsLocked),
          CountryOfOriginSummary
            .rowUpdate(lockedRecord, lockedRecord.recordId, NormalMode, recordIsLocked, countries),
          CommodityCodeSummary
            .rowUpdate(lockedRecord, lockedRecord.recordId, NormalMode, recordIsLocked, isReviewReasonCommodity = false)
        )
      )

      val categorisationList = SummaryListViewModel(
        rows = Seq(
          CategorySummary
            .row(
              "singleRecord.cat1",
              testRecordId,
              recordIsLocked,
              recordForTestingSummaryRows.category.isDefined,
              recordForTestingSummaryRows.reviewReason
            )
        )
      )

      val supplementaryUnitList = SummaryListViewModel(
        rows = Seq(
          HasSupplementaryUnitSummary
            .row(
              lockedRecord,
              lockedRecord.recordId,
              recordIsLocked
            ),
          SupplementaryUnitSummary
            .row(
              Some(2),
              lockedRecord.supplementaryUnit,
              lockedRecord.measurementUnit,
              lockedRecord.recordId,
              recordIsLocked
            )
        ).flatten
      )

      val adviceList = SummaryListViewModel(
        rows = Seq(
          AdviceStatusSummary
            .row(lockedRecord.adviceStatus, lockedRecord.recordId, recordIsLocked, isReviewReasonCommodity = false)
        )
      )

      running(application) {
        val request = FakeRequest(GET, singleRecordRouteLocked)

        val result = route(application, request).value

        val view                                  = application.injector.instanceOf[SingleRecordView]
        val changesMade                           = request.session.get(dataUpdated).contains("true")
        val changedPage                           = request.session.get(pageUpdated).getOrElse("")
        val pageRemoved                           = request.session.get(dataRemoved).contains("true")
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          lockedRecord.recordId,
          detailsList,
          categorisationList,
          supplementaryUnitList,
          adviceList,
          changesMade,
          changedPage,
          pageRemoved,
          recordIsLocked,
          Some(RequestedParagraph),
          NotReadyForUse,
          toReview = false,
          isCategorised = recordForTestingSummaryRows.category.isDefined,
          recordForTestingSummaryRows.adviceStatus,
          None,
          request.headers
            .get("Referer")
            .getOrElse(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(1).url),
          None
        )(
          request,
          messages(application)
        ).toString
        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, times(2)).set(uaCaptor.capture)

        uaCaptor.getValue.data mustEqual userAnswers.data

        withClue("must cleanse the user answers data") {
          verify(mockSessionRepository).clearData(eqTo(userAnswers.id), eqTo(SupplementaryUnitUpdateJourney))
        }
      }
    }

    "must return OK and the correct view for a GET and set up userAnswers when record is not categorised" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(ProductReferenceUpdatePage(testRecordId), notCategorisedRecord.traderRef)
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

      when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future
        .successful(notCategorisedRecord)

      when(mockSessionRepository.set(any())) thenReturn Future
        .successful(true)

      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[OttConnector].toInstance(mockOttConnector),
          bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
        )
        .build()

      implicit val message: Messages = messages(application)

      val detailsList = SummaryListViewModel(
        rows = Seq(
          ProductReferenceSummary.row(notCategorisedRecord.traderRef, testRecordId, NormalMode, recordIsLocked),
          GoodsDescriptionSummary.rowUpdate(notCategorisedRecord, testRecordId, NormalMode, recordIsLocked),
          CountryOfOriginSummary
            .rowUpdate(
              notCategorisedRecord,
              testRecordId,
              NormalMode,
              recordIsLocked,
              countries
            ),
          CommodityCodeSummary
            .rowUpdate(
              notCategorisedRecord,
              testRecordId,
              NormalMode,
              recordIsLocked,
              isReviewReasonCommodity = false
            )
        )
      )

      val categorisationList = SummaryListViewModel(
        rows = Seq(
          CategorySummary.row(
            "singleRecord.categoriseThisGood",
            testRecordId,
            recordIsLocked,
            notCategorisedRecord.category.isDefined,
            recordForTestingSummaryRows.reviewReason
          )
        )
      )

      val supplementaryUnitList = SummaryListViewModel(
        rows = Seq(
          HasSupplementaryUnitSummary
            .row(
              notCategorisedRecord,
              testRecordId,
              recordIsLocked
            ),
          SupplementaryUnitSummary
            .row(
              Some(2),
              notCategorisedRecord.supplementaryUnit,
              notCategorisedRecord.measurementUnit,
              testRecordId,
              recordIsLocked
            )
        ).flatten
      )

      val adviceList = SummaryListViewModel(
        rows = Seq(
          AdviceStatusSummary
            .row(notCategorisedRecord.adviceStatus, testRecordId, recordIsLocked, isReviewReasonCommodity = false)
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
          recordIsLocked,
          Some(NotRequestedParagraph),
          NotReadyForUse,
          toReview = false,
          isCategorised = notCategorisedRecord.category.isDefined,
          notCategorisedRecord.adviceStatus,
          None,
          request.headers
            .get("Referer")
            .getOrElse(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(1).url),
          None
        )(
          request,
          messages(application)
        ).toString
        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, times(2)).set(uaCaptor.capture)

        uaCaptor.getValue.data mustEqual userAnswers.data

        withClue("must cleanse the user answers data") {
          verify(mockSessionRepository).clearData(eqTo(userAnswers.id), eqTo(SupplementaryUnitUpdateJourney))
        }
      }
    }

    "must return OK and the correct view for a GET and set up userAnswers when record is not categorised and is locked" in {

      val notCategorisedLockedRecord = goodsRecordResponse(
        Instant.parse("2022-11-18T23:20:19Z"),
        Instant.parse("2022-11-18T23:20:19Z")
      ).copy(adviceStatus = Requested).copy(category = None)

      val recordIsLocked = true
      val userAnswers    = UserAnswers(userAnswersId)
        .set(ProductReferenceUpdatePage(notCategorisedLockedRecord.recordId), notCategorisedLockedRecord.traderRef)
        .success
        .value
        .set(
          GoodsDescriptionUpdatePage(notCategorisedLockedRecord.recordId),
          notCategorisedLockedRecord.goodsDescription
        )
        .success
        .value
        .set(CountryOfOriginUpdatePage(notCategorisedLockedRecord.recordId), notCategorisedLockedRecord.countryOfOrigin)
        .success
        .value
        .set(CommodityCodeUpdatePage(notCategorisedLockedRecord.recordId), notCategorisedLockedRecord.comcode)
        .success
        .value

      when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future
        .successful(notCategorisedLockedRecord)

      when(mockSessionRepository.set(any())) thenReturn Future
        .successful(true)

      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[OttConnector].toInstance(mockOttConnector),
          bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
        )
        .build()

      implicit val message: Messages = messages(application)

      val detailsList = SummaryListViewModel(
        rows = Seq(
          ProductReferenceSummary
            .row(notCategorisedLockedRecord.traderRef, notCategorisedLockedRecord.recordId, NormalMode, recordIsLocked),
          GoodsDescriptionSummary
            .rowUpdate(notCategorisedLockedRecord, notCategorisedLockedRecord.recordId, NormalMode, recordIsLocked),
          CountryOfOriginSummary
            .rowUpdate(
              notCategorisedLockedRecord,
              notCategorisedLockedRecord.recordId,
              NormalMode,
              recordIsLocked,
              countries
            ),
          CommodityCodeSummary
            .rowUpdate(
              notCategorisedLockedRecord,
              notCategorisedLockedRecord.recordId,
              NormalMode,
              recordIsLocked,
              isReviewReasonCommodity = false
            )
        )
      )

      val categorisationList = SummaryListViewModel(
        rows = Seq(
          CategorySummary
            .row(
              "singleRecord.notCategorised.recordLocked",
              notCategorisedLockedRecord.recordId,
              recordIsLocked,
              notCategorisedLockedRecord.category.isDefined,
              recordForTestingSummaryRows.reviewReason
            )
        )
      )

      val supplementaryUnitList = SummaryListViewModel(
        rows = Seq(
          HasSupplementaryUnitSummary
            .row(
              notCategorisedLockedRecord,
              notCategorisedLockedRecord.recordId,
              recordIsLocked
            ),
          SupplementaryUnitSummary
            .row(
              None,
              notCategorisedLockedRecord.supplementaryUnit,
              notCategorisedLockedRecord.measurementUnit,
              notCategorisedLockedRecord.recordId,
              recordIsLocked
            )
        ).flatten
      )

      val adviceList = SummaryListViewModel(
        rows = Seq(
          AdviceStatusSummary
            .row(
              notCategorisedLockedRecord.adviceStatus,
              notCategorisedLockedRecord.recordId,
              recordIsLocked,
              isReviewReasonCommodity = false
            )
        )
      )

      running(application) {
        val request = FakeRequest(GET, singleRecordRouteLocked)

        val result = route(application, request).value

        val view                                  = application.injector.instanceOf[SingleRecordView]
        val changesMade                           = request.session.get(dataUpdated).contains("true")
        val changedPage                           = request.session.get(pageUpdated).getOrElse("")
        val pageRemoved                           = request.session.get(dataRemoved).contains("true")
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          notCategorisedLockedRecord.recordId,
          detailsList,
          categorisationList,
          supplementaryUnitList,
          adviceList,
          changesMade,
          changedPage,
          pageRemoved,
          recordIsLocked,
          Some(RequestedParagraph),
          NotReadyForUse,
          toReview = false,
          isCategorised = notCategorisedLockedRecord.category.isDefined,
          recordForTestingSummaryRows.adviceStatus,
          None,
          request.headers
            .get("Referer")
            .getOrElse(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(1).url),
          None
        )(
          request,
          messages(application)
        ).toString
        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository, times(2)).set(uaCaptor.capture)

        uaCaptor.getValue.data mustEqual userAnswers.data

        withClue("must cleanse the user answers data") {
          verify(mockSessionRepository).clearData(eqTo(userAnswers.id), eqTo(SupplementaryUnitUpdateJourney))
        }
      }
    }

    "must redirect to journey recovery for a GET when ott connectors fails" in {

      when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future
        .successful(notCategorisedRecord)

      when(mockSessionRepository.set(any())) thenReturn Future
        .successful(true)
      when(mockOttConnector.getCountries(any())) thenReturn Future.failed(new RuntimeException("Ott connector failed"))

      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[OttConnector].toInstance(mockOttConnector),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, singleRecordRoute)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.problem.routes.JourneyRecoveryController.onPageLoad().url)

        verify(mockGoodsRecordConnector, times(1)).getRecord(any())(any())
        verify(mockOttConnector, times(1)).getCountries(any())

      }
    }

    "must return a SummaryListRow with the correct supplementary unit and measurement unit appended" in {

      val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
        )
        .build()
      implicit val localMessages: Messages = messages(application)

      running(application) {
        val row = SupplementaryUnitSummary
          .row(
            Some(2),
            recordWithSupplementaryUnit.supplementaryUnit,
            recordWithSupplementaryUnit.measurementUnit,
            testRecordId,
            recordIsLocked
          )
          .value

        val supplementaryValue = row.value.content match {
          case Text(innerContent) => innerContent
          case _                  => ""
        }

        supplementaryValue must equal("1234567890.123456 grams")

      }

    }

    "must show hasSupplementaryUnit when measurement unit is not empty and supplementary unit is No" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
        )
        .build()

      implicit val localMessages: Messages = messages(application)

      val record = recordWithSupplementaryUnit.copy(supplementaryUnit = None)

      running(application) {
        val row                  = HasSupplementaryUnitSummary
          .row(record, testRecordId, recordIsLocked)
          .value
        val hasSupplementaryUnit = row.value.content match {
          case Text(innerContent) => innerContent
          case _                  => ""
        }

        hasSupplementaryUnit contains "Do you want to add the supplementary unit?"

      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
          bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
        )
        .build()

      when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future
        .failed(new NotFoundException("Failed to find record"))

      running(application) {
        val request = FakeRequest(GET, singleRecordRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.problem.routes.JourneyRecoveryController.onPageLoad().url)

        verify(mockGoodsRecordConnector, times(1)).getRecord(any())(any())
      }
    }

    "CategorySummary.row" - {

      "must return a SummaryListRow without change links when record is locked" in {

        val recordLocked = true

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = CategorySummary.row(
            recordForTestingSummaryRows.category.toString,
            testRecordId,
            recordLocked,
            isCategorised = true,
            recordForTestingSummaryRows.reviewReason
          )

          row.actions mustBe Some(Actions("", List()))
        }
      }

      "must return a SummaryListRow with change links when record is not locked" in {

        val recordLocked = false

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = CategorySummary.row(
            recordForTestingSummaryRows.category.toString,
            testRecordId,
            recordLocked,
            isCategorised = true,
            recordForTestingSummaryRows.reviewReason
          )

          row.actions mustBe defined
          row.actions.value.items.head.href mustEqual controllers.routes.ValidateCommodityCodeController
            .changeCategory(testRecordId)
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
            Some(2),
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
            Some(2),
            recordWithSupplementaryUnit.supplementaryUnit,
            recordWithSupplementaryUnit.measurementUnit,
            testRecordId,
            recordLocked
          )

          row.get.actions mustBe defined
          row.get.actions.value.items.head.href mustEqual controllers.categorisation.routes.SupplementaryUnitController
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
          val row = AdviceStatusSummary.row(
            recordForTestingSummaryRows.adviceStatus,
            testRecordId,
            recordLocked,
            isReviewReasonCommodity = false
          )

          row.actions mustBe defined
          row.actions.value.items.head.href mustEqual controllers.advice.routes.WithdrawAdviceStartController
            .onPageLoad(testRecordId)
            .url
        }
      }

      "must return a SummaryListRow with change link action as advice when record is locked" in {

        val recordLocked = false

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = AdviceStatusSummary.row(
            recordForTestingSummaryRows.adviceStatus,
            testRecordId,
            recordLocked,
            isReviewReasonCommodity = false
          )

          row.actions mustBe defined
          row.actions.value.items.head.href mustEqual controllers.advice.routes.AdviceStartController
            .onPageLoad(testRecordId)
            .url
        }
      }

      "must return a SummaryListRow without change link action when advice status is Advice Received " in {

        val recordLocked = false

        val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()
        implicit val localMessages: Messages = messages(application)
        running(application) {
          val row = AdviceStatusSummary.row(
            recordForTestingSummaryRowsWithAdviceProvided.adviceStatus,
            testRecordId,
            recordLocked,
            isReviewReasonCommodity = false
          )

          row.actions mustBe Some(Actions("", List()))
        }
      }
    }

    "must return OK and the correct view for a GET when review reason" - {

      forAll(ReviewReason.values) { reviewReason =>
        s"is $reviewReason" in {

          val isReviewReasonCommodity = reviewReason == ReviewReason.Commodity

          val record   = goodsRecordResponseWithReviewReason(reviewReason = reviewReason)
          val toReview = true

          val userAnswers = UserAnswers(userAnswersId)
            .set(ProductReferenceUpdatePage(record.recordId), record.traderRef)
            .success
            .value
            .set(GoodsDescriptionUpdatePage(record.recordId), record.goodsDescription)
            .success
            .value
            .set(CountryOfOriginUpdatePage(record.recordId), record.countryOfOrigin)
            .success
            .value
            .set(CommodityCodeUpdatePage(record.recordId), record.comcode)
            .success
            .value

          when(mockGoodsRecordConnector.getRecord(any())(any())) thenReturn Future
            .successful(record)

          when(mockSessionRepository.set(any())) thenReturn Future
            .successful(true)

          when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[TraderProfileConnector].toInstance(mockTraderProfileConnector),
              bind[OttConnector].toInstance(mockOttConnector),
              bind[AutoCategoriseService].toInstance(mockAutoCategoriseService)
            )
            .build()

          implicit val message: Messages = messages(application)

          val detailsList = SummaryListViewModel(
            rows = Seq(
              ProductReferenceSummary.row(record.traderRef, record.recordId, NormalMode, recordIsLocked),
              GoodsDescriptionSummary.rowUpdate(record, record.recordId, NormalMode, recordIsLocked),
              CountryOfOriginSummary
                .rowUpdate(record, record.recordId, NormalMode, recordIsLocked, countries),
              CommodityCodeSummary
                .rowUpdate(record, record.recordId, NormalMode, recordIsLocked, isReviewReasonCommodity)
            )
          )

          val categorisationList = SummaryListViewModel(
            rows = Seq(
              CategorySummary
                .row(
                  "singleRecord.cat1",
                  record.recordId,
                  recordIsLocked,
                  record.category.isDefined,
                  Some(reviewReason)
                )
            )
          )

          val supplementaryUnitList = SummaryListViewModel(
            rows = Seq(
              HasSupplementaryUnitSummary
                .row(
                  record,
                  record.recordId,
                  recordIsLocked
                ),
              SupplementaryUnitSummary
                .row(
                  Some(2),
                  record.supplementaryUnit,
                  record.measurementUnit,
                  record.recordId,
                  recordIsLocked
                )
            ).flatten
          )

          val adviceList = SummaryListViewModel(
            rows = Seq(
              AdviceStatusSummary.row(record.adviceStatus, record.recordId, recordIsLocked, isReviewReasonCommodity)
            )
          )

          running(application) {
            val request = FakeRequest(GET, singleRecordRoute)

            val result                                = route(application, request).value
            val view                                  = application.injector.instanceOf[SingleRecordView]
            val changesMade                           = request.session.get(dataUpdated).contains("true")
            val changedPage                           = request.session.get(pageUpdated).getOrElse("")
            val pageRemoved                           = request.session.get(dataRemoved).contains("true")
            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              record.recordId,
              detailsList,
              categorisationList,
              supplementaryUnitList,
              adviceList,
              changesMade,
              changedPage,
              pageRemoved,
              recordIsLocked,
              Some(NotRequestedParagraph),
              NotReadyForUse,
              toReview,
              recordForTestingSummaryRows.category.isDefined,
              recordForTestingSummaryRows.adviceStatus,
              Some(reviewReason),
              request.headers
                .get("Referer")
                .getOrElse(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(1).url),
              None
            )(
              request,
              messages(application)
            ).toString
            val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(mockSessionRepository, times(2)).set(uaCaptor.capture)

            uaCaptor.getValue.data mustEqual userAnswers.data

            withClue("must cleanse the user answers data") {
              verify(mockSessionRepository).clearData(eqTo(userAnswers.id), eqTo(SupplementaryUnitUpdateJourney))
            }
          }
        }
      }
    }
    "redirect to RecordNotFound page when record does not exist (404)" in {
      when(mockGoodsRecordConnector.getRecord(eqTo(testRecordId))(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Record not found", NOT_FOUND, NOT_FOUND)))

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockSessionRepository.clearData(any(), any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[OttConnector].toInstance(mockOttConnector),
          bind[TraderProfileConnector].toInstance(mockTraderProfileConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, singleRecordRoute)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.problem.routes.RecordNotFoundController.onPageLoad().url)
      }
    }
  }
}
