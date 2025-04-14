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
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import models.helper.{CategorisationJourney, RequestAdviceJourney, SupplementaryUnitUpdateJourney, WithdrawAdviceJourney}
import models.requests.DataRequest
import models.{AdviceStatusMessage, Country, NormalMode, ReviewReason}
import pages.goodsRecord.{CommodityCodeUpdatePage, CountryOfOriginUpdatePage, GoodsDescriptionUpdatePage, ProductReferenceUpdatePage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.CountriesQuery
import repositories.SessionRepository
import services.DataCleansingService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.SessionData._
import viewmodels.checkAnswers._
import viewmodels.checkAnswers.goodsRecord.{CommodityCodeSummary, CountryOfOriginSummary, GoodsDescriptionSummary, ProductReferenceSummary}
import viewmodels.govuk.summarylist._
import views.html.goodsRecord.SingleRecordView

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class SingleRecordController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  goodsRecordConnector: GoodsRecordConnector,
  sessionRepository: SessionRepository,
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
      goodsRecordConnector.getRecord(recordId).flatMap { record =>
        val referer  = request.headers.get("Referer")
        val backLink = referer
          .filter(_.contains("page"))
          .getOrElse(controllers.goodsProfile.routes.GoodsRecordsController.onPageLoad(1).url)
        for {
          recordIsLocked                     <- Future.successful(record.adviceStatus.isRecordLocked)
          countries                          <- retrieveAndStoreCountries
          updatedAnswersWithproductReference <-
            Future.fromTry(request.userAnswers.set(ProductReferenceUpdatePage(recordId), record.traderRef))
          updatedAnswersWithGoodsDescription <-
            Future.fromTry(
              updatedAnswersWithproductReference.set(GoodsDescriptionUpdatePage(recordId), record.goodsDescription)
            )
          updatedAnswersWithCountryOfOrigin  <-
            Future.fromTry(
              updatedAnswersWithGoodsDescription.set(CountryOfOriginUpdatePage(recordId), record.countryOfOrigin)
            )
          updatedAnswersWithAll              <-
            Future.fromTry(
              updatedAnswersWithCountryOfOrigin.set(CommodityCodeUpdatePage(recordId), record.comcode)
            )

          _ <- sessionRepository.set(updatedAnswersWithAll)

        } yield {
          val isCategorised           = record.category.isDefined
          val isReviewReasonCommodity = (record.toReview, record.reviewReason) match {
            case (true, Some(ReviewReason.Commodity)) => true
            case _                                    => false
          }

          val detailsList = SummaryListViewModel(
            rows = Seq(
              ProductReferenceSummary.row(record.traderRef, recordId, NormalMode, recordIsLocked),
              GoodsDescriptionSummary.rowUpdate(record, recordId, NormalMode, recordIsLocked),
              CountryOfOriginSummary.rowUpdate(record, recordId, NormalMode, recordIsLocked, countries),
              CommodityCodeSummary.rowUpdate(
                record,
                recordId,
                NormalMode,
                recordLocked = recordIsLocked,
                isReviewReasonCommodity = isReviewReasonCommodity
              )
            )
          )

          val categoryValue                     = record.category match {
            case None        =>
              if (recordIsLocked) "singleRecord.notCategorised.recordLocked" else "singleRecord.categoriseThisGood"
            case Some(value) =>
              value match {
                case 1 => "singleRecord.cat1"
                case 2 => "singleRecord.cat2"
                case 3 => "singleRecord.standardGoods"
              }
          }
          val categorisationList                = SummaryListViewModel(
            rows = Seq(
              CategorySummary.row(categoryValue, record.recordId, recordIsLocked, isCategorised, record.reviewReason)
            )
          )
          val supplementaryUnitList             = SummaryListViewModel(
            rows = Seq(
              HasSupplementaryUnitSummary.row(record, recordId, recordIsLocked),
              SupplementaryUnitSummary
                .row(record.category, record.supplementaryUnit, record.measurementUnit, recordId, recordIsLocked)
            ).flatten
          )
          val adviceList                        = SummaryListViewModel(
            rows = Seq(
              AdviceStatusSummary.row(
                record.adviceStatus,
                record.recordId,
                recordLocked = recordIsLocked,
                isReviewReasonCommodity = isReviewReasonCommodity
              )
            )
          )
          val changesMade                       = request.session.get(dataUpdated).contains("true")
          val pageRemoved                       = request.session.get(dataRemoved).contains("true")
          val changedPage                       = request.session.get(pageUpdated).getOrElse("")
          val para: Option[AdviceStatusMessage] = AdviceStatusMessage.fromString(record.adviceStatus)

          dataCleansing(request)
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
              record.declarable,
              record.toReview,
              record.category.isDefined,
              record.adviceStatus,
              record.reviewReason,
              backLink
            )
          ).removingFromSession(initialValueOfHasSuppUnit, initialValueOfSuppUnit)
        }
      }.recover {
          case e: UpstreamErrorResponse if e.statusCode == 404 =>
            Redirect(controllers.problem.routes.RecordNotFoundController.onPageLoad())
          case e: Exception =>
            logger.error(s"Error: ${e.getMessage}")
            Redirect(controllers.problem.routes.JourneyRecoveryController.onPageLoad())
        }
      }

  private def dataCleansing(request: DataRequest[AnyContent]) = {
    //at this point we should delete all journey data as the user might comeback using backlink & click change link again
    dataCleansingService.deleteMongoData(request.userAnswers.id, SupplementaryUnitUpdateJourney)
    dataCleansingService.deleteMongoData(request.userAnswers.id, RequestAdviceJourney)
    dataCleansingService.deleteMongoData(request.userAnswers.id, WithdrawAdviceJourney)
    dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney)
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
}
