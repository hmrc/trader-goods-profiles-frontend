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

import connectors.{GoodsRecordConnector, OttConnector}
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import models.{Country, NormalMode, UserAnswers}
import models.helper.{CategorisationJourney, RequestAdviceJourney, SupplementaryUnitUpdateJourney, WithdrawAdviceJourney}
import models.requests.DataRequest
import pages.{CommodityCodeUpdatePage, CountryOfOriginUpdatePage, GoodsDescriptionUpdatePage, TraderReferenceUpdatePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.CountriesQuery
import repositories.SessionRepository
import services.DataCleansingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.SessionData._
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.SingleRecordView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
class SingleRecordController @Inject() (
  override val messagesApi: MessagesApi,
  goodsRecordConnector: GoodsRecordConnector,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  profileAuth: ProfileAuthenticateAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  dataCleansingService: DataCleansingService,
  ottConnector: OttConnector,
  val controllerComponents: MessagesControllerComponents,
  view: SingleRecordView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(recordId: String): Action[AnyContent]                                                             =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      for {
        record                             <- goodsRecordConnector.getRecord(request.eori, recordId)
        recordIsLocked                      = record.adviceStatus match {
                                                case status
                                                    if status.equalsIgnoreCase("Requested") ||
                                                      status.equalsIgnoreCase("In progress") ||
                                                      status.equalsIgnoreCase("Information Requested") =>
                                                  true
                                                case _ => false
                                              }
        updatedAnswersWithTraderReference  <-
          Future.fromTry(request.userAnswers.set(TraderReferenceUpdatePage(recordId), record.traderRef))
        updatedAnswersWithGoodsDescription <-
          Future.fromTry(
            updatedAnswersWithTraderReference.set(GoodsDescriptionUpdatePage(recordId), record.goodsDescription)
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
        val isCategorised = record.category.isDefined
        val countries     = retrieveAndStoreCountries
        val detailsList   = SummaryListViewModel(
          rows = Seq(
            TraderReferenceSummary.row(record.traderRef, recordId, NormalMode, recordIsLocked),
            GoodsDescriptionSummary.rowUpdate(record, recordId, NormalMode, recordIsLocked),
            CountryOfOriginSummary.rowUpdate(record, recordId, NormalMode, recordIsLocked, countries),
            CommodityCodeSummary.rowUpdate(record, recordId, NormalMode, recordIsLocked),
            StatusSummary.row(record.declarable)
          )
        )

        val categoryValue         = record.category match {
          case None        => "singleRecord.categoriseThisGood"
          case Some(value) =>
            value match {
              case 1 => "singleRecord.cat1"
              case 2 => "singleRecord.cat2"
              case 3 => "singleRecord.standardGoods"
            }
        }
        val categorisationList    = SummaryListViewModel(
          rows = Seq(
            CategorySummary.row(categoryValue, record.recordId, recordIsLocked, isCategorised)
          )
        )
        val supplementaryUnitList = SummaryListViewModel(
          rows = Seq(
            HasSupplementaryUnitSummary.row(record, recordId, recordIsLocked),
            SupplementaryUnitSummary
              .row(record.supplementaryUnit, record.measurementUnit, recordId, recordIsLocked)
          ).flatten
        )
        val adviceList            = SummaryListViewModel(
          rows = Seq(
            AdviceStatusSummary.row(record.adviceStatus, record.recordId, recordIsLocked)
          )
        )
        val changesMade           = request.session.get(dataUpdated).contains("true")
        val pageRemoved           = request.session.get(dataRemoved).contains("true")
        val changedPage           = request.session.get(pageUpdated).getOrElse("")

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
            recordIsLocked
          )
        ).removingFromSession(initialValueOfHasSuppUnit, initialValueOfSuppUnit)
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
