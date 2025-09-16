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

import connectors.{GoodsRecordConnector, OttConnector}
import controllers.BaseController
import controllers.actions._
import models.helper._
import models.requests.DataRequest
import models.router.responses.GetGoodsRecordResponse
import models._
import pages.goodsRecord.*
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.CountriesQuery
import repositories.SessionRepository
import services.{AutoCategoriseService, DataCleansingService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.SessionData.*
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.goodsRecord._
import viewmodels.govuk.summarylist.*
import views.html.goodsRecord.SingleRecordView

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class SingleRecordController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  goodsRecordConnector: GoodsRecordConnector,
  sessionRepository: SessionRepository,
  autoCategoriseService: AutoCategoriseService,
  dataCleansingService: DataCleansingService,
  identify: IdentifierAction,
  profileAuth: ProfileAuthenticateAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  ottConnector: OttConnector,
  view: SingleRecordView
)(implicit @unused ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val countryOfOriginUpdated = request.session.get("countryOfOriginChanged").contains("true")
      val showCommodityCodeBanner: Boolean = request.session.get("showCommodityCodeChangeBanner").contains("true")

      goodsRecordConnector
        .getRecord(recordId)
        .flatMap {
          case None =>
            Future.successful(Redirect(controllers.problem.routes.RecordNotFoundController.onPageLoad()))
          case Some(initialRecord) =>
            val backLink = request.headers
              .get("Referer")
              .filter(_.contains("page"))
              .getOrElse(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(1).url)

            val countryFromSession: Option[String] =
              request.userAnswers.get(OriginalCountryOfOriginPage(recordId))

            val recordToDisplay = initialRecord.copy(
              countryOfOrigin = countryFromSession.orElse(Option(initialRecord.countryOfOrigin)).getOrElse("")
            )

            for {
              countries <- retrieveAndStoreCountries
              updatedAnswers <- updateUserAnswers(recordId, recordToDisplay)
              autoCategoriseScenario <- if (shouldAutoCategorise(recordToDisplay)) {
                autoCategoriseService.autoCategoriseRecord(recordToDisplay, updatedAnswers)
              } else {
                Future.successful(None)
              }
              _ <- sessionRepository.set(updatedAnswers)
              finalRecord <- if (autoCategoriseScenario.isDefined) {
                goodsRecordConnector.getRecord(recordId).map {
                  case Some(record) => record
                  case None =>
                    throw new IllegalStateException(s"Record $recordId not found after auto-categorisation")
                }
              } else {
                Future.successful(recordToDisplay)
              }
            } yield renderView(
              recordId,
              finalRecord,
              backLink,
              countries,
              autoCategoriseScenario,
              countryOfOriginUpdated,
              showCommodityCodeBanner
            )
        }
        .recover {
          case e: Exception =>
            logger.error(s"Error loading goods record: ${e.getMessage}")
            Redirect(controllers.problem.routes.JourneyRecoveryController.onPageLoad())
        }
    }

  private def shouldAutoCategorise(record: GetGoodsRecordResponse): Boolean =
    record.category.isEmpty && !record.adviceStatus.isRecordLocked

  private def updateUserAnswers(recordId: String, record: GetGoodsRecordResponse)(implicit
    request: DataRequest[_]
  ): Future[UserAnswers] = {
    val baseTry =
      request.userAnswers
        .set(ProductReferenceUpdatePage(recordId), record.traderRef)
        .flatMap(_.set(GoodsDescriptionUpdatePage(recordId), record.goodsDescription))
        .flatMap(_.set(CountryOfOriginUpdatePage(recordId), record.countryOfOrigin))
        .flatMap(_.set(CommodityCodeUpdatePage(recordId), record.comcode))

    val withOriginal =
      if (!record.adviceStatus.isRecordLocked) {
        baseTry.flatMap(_.set(OriginalCountryOfOriginPage(recordId), record.countryOfOrigin))
      } else {
        baseTry
      }

    Future.fromTry(withOriginal)
  }

  private def retrieveAndStoreCountries(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Seq[Country]] =
    request.userAnswers.get(CountriesQuery) match {
      case Some(countries) =>
        Future.successful(countries)
      case None            =>
        for {
          countries               <- ottConnector.getCountries
          updatedAnswersWithQuery <- Future.fromTry(request.userAnswers.set(CountriesQuery, countries))
          _                       <- sessionRepository.set(updatedAnswersWithQuery)
        } yield countries
    }

  private def renderView(
    recordId: String,
    record: GetGoodsRecordResponse,
    backLink: String,
    countries: Seq[Country],
    autoCategoriseScenario: Option[Scenario],
    countryOfOriginUpdated: Boolean,
    showCommodityCodeBanner: Boolean
  )(implicit request: DataRequest[_]): Result = {
    val recordIsLocked                   = record.adviceStatus.isRecordLocked
    val isCategorised                    = record.category.isDefined
    val isReviewReasonCommodityOrCountry = (record.toReview, record.reviewReason) match {
      case (true, Some(ReviewReason.Commodity)) => true
      case (true, Some(ReviewReason.Country))   => true
      case _                                    => false
    }

    val declarable = if (record.toReview) {
      DeclarableStatus.NotReadyForUse
    } else {
      record.declarable
    }

    val detailsList = SummaryListViewModel(
      rows = Seq(
        ProductReferenceSummary.row(record.traderRef, recordId, NormalMode, recordIsLocked),
        GoodsDescriptionSummary.rowUpdate(record, recordId, NormalMode, recordIsLocked),
        CountryOfOriginSummary.rowUpdate(record, recordId, NormalMode, recordIsLocked, countries, record.reviewReason),
        CommodityCodeSummary.rowUpdate(record, recordId, NormalMode, recordIsLocked, record.reviewReason)
      )
    )

    val categoryValue =
      if (countryOfOriginUpdated && record.category.isEmpty) {
        "singleRecord.categoriseThisGood"
      } else {
        record.category match {
          case None        =>
            if (recordIsLocked) "singleRecord.notCategorised.recordLocked"
            else "singleRecord.categoriseThisGood"
          case Some(value) =>
            value match {
              case 1 => "singleRecord.cat1"
              case 2 => "singleRecord.cat2"
              case 3 => "singleRecord.standardGoods"
            }
        }
      }

    val categorisationList = SummaryListViewModel(
      rows = Seq(
        CategorySummary.row(categoryValue, recordId, recordIsLocked, isCategorised, record.reviewReason)
      )
    )

    val supplementaryUnitList = SummaryListViewModel(
      rows = Seq(
        HasSupplementaryUnitSummary.row(record, recordId, recordIsLocked),
        SupplementaryUnitSummary
          .row(record.category, record.supplementaryUnit, record.measurementUnit, recordId, recordIsLocked)
      ).flatten
    )

    val adviceList = SummaryListViewModel(
      rows = Seq(
        AdviceStatusSummary.row(record.adviceStatus, recordId, recordIsLocked, isReviewReasonCommodityOrCountry)
      )
    )

    val changesMade                      = request.session.get(dataUpdated).contains("true")
    val pageRemoved                      = request.session.get(dataRemoved).contains("true")
    val changedPage                      = request.session.get(pageUpdated).getOrElse("")
    val para                             = AdviceStatusMessage.fromString(record.adviceStatus)
    val isAutoCategorised                = autoCategoriseScenario.isDefined
    val bannerMessageKey: Option[String] =
      if (isAutoCategorised && countryOfOriginUpdated)
        Some("successBanner.countryOfOrigin")
      else if (isAutoCategorised && showCommodityCodeBanner)
        Some("successBanner.commodityCode")
      else
        None

    dataCleansingService.deleteMongoData(request.userAnswers.id, SupplementaryUnitUpdateJourney)
    dataCleansingService.deleteMongoData(request.userAnswers.id, RequestAdviceJourney)
    dataCleansingService.deleteMongoData(request.userAnswers.id, WithdrawAdviceJourney)
    dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney)

    Ok(
      view(
        recordId,
        detailsList,
        categorisationList,
        supplementaryUnitList,
        adviceList,
        changesMade,
        changedPage,
        pageRemoved,
        recordIsLocked,
        para,
        declarable,
        record.toReview,
        isCategorised,
        record.adviceStatus,
        record.reviewReason,
        backLink,
        autoCategoriseScenario,
        countryOfOriginUpdated,
        record.traderRef,
        isAutoCategorised,
        showCommodityCodeBanner,
        bannerMessageKey
      )
    ).removingFromSession(
      initialValueOfHasSuppUnit,
      initialValueOfSuppUnit,
      goodsDescriptionOriginal,
      "countryOfOriginChanged",
      "showCommodityCodeChangeBanner"
    )
  }
}
