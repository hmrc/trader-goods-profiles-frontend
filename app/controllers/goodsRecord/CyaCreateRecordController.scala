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

import cats.data.NonEmptyChain
import com.google.inject.Inject
import connectors.{GoodsRecordConnector, OttConnector}
import controllers.BaseController
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import models.helper.CreateRecordJourney
import models.requests.DataRequest
import models.{Country, GoodsRecord, NormalMode, UserAnswers, ValidationError}
import navigation.GoodsRecordNavigator
import pages.goodsRecord.CyaCreateRecordPage
import play.api.i18n.MessagesApi
import play.api.mvc.*
import queries.CountriesQuery
import repositories.SessionRepository
import services.{AuditService, DataCleansingService}
import viewmodels.checkAnswers.goodsRecord.{CommodityCodeSummary, CountryOfOriginSummary, GoodsDescriptionSummary, ProductReferenceSummary}
import viewmodels.govuk.summarylist.*
import views.html.goodsRecord.CyaCreateRecordView

import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class CyaCreateRecordController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaCreateRecordView,
  goodsRecordConnector: GoodsRecordConnector,
  ottConnector: OttConnector,
  dataCleansingService: DataCleansingService,
  sessionRepository: SessionRepository,
  auditService: AuditService,
  navigator: GoodsRecordNavigator
)(implicit @unused ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String = "Unable to create Goods Record."

  def onPageLoad(): Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData).async {
    implicit request =>
      GoodsRecord.build(request.userAnswers, request.eori) match {
        case Right(_)     =>
          request.userAnswers.get(CountriesQuery) match {
            case Some(countries) => Future.successful(displayView(request.userAnswers, countries))
            case None            =>
              for {
                countries               <- ottConnector.getCountries
                updatedAnswersWithQuery <- Future.fromTry(request.userAnswers.set(CountriesQuery, countries))
                _                       <- sessionRepository.set(updatedAnswersWithQuery)
              } yield displayView(updatedAnswersWithQuery, countries)
          }
        case Left(errors) =>
          handleBuildErrors(request, errors)
      }
  }

  private def displayView(userAnswers: UserAnswers, countries: Seq[Country])(implicit request: Request[_]): Result = {
    val list = SummaryListViewModel(
      rows = Seq(
        ProductReferenceSummary.row(userAnswers),
        GoodsDescriptionSummary.row(userAnswers),
        CountryOfOriginSummary.row(userAnswers, countries),
        CommodityCodeSummary.row(userAnswers)
      ).flatten
    )
    Ok(view(list))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData).async {
    implicit request =>
      GoodsRecord.build(request.userAnswers, request.eori) match {
        case Right(model) =>
          auditService.auditFinishCreateGoodsRecord(request.eori, request.affinityGroup, request.userAnswers)
          for {
            recordId <- goodsRecordConnector.submitGoodsRecord(model)
            _        <- dataCleansingService.deleteMongoData(request.userAnswers.id, CreateRecordJourney)
          } yield Redirect(navigator.nextPage(CyaCreateRecordPage(recordId), NormalMode, request.userAnswers))
        case Left(errors) =>
          handleBuildErrors(request, errors)
      }
  }

  private def handleBuildErrors(request: DataRequest[AnyContent], errors: NonEmptyChain[ValidationError]) = {
    dataCleansingService.deleteMongoData(request.userAnswers.id, CreateRecordJourney)
    Future.successful(
      logErrorsAndContinue(
        errorMessage,
        controllers.goodsRecord.routes.CreateRecordStartController.onPageLoad(),
        errors
      )
    )
  }

}
