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

package controllers.categorisation

import cats.data
import com.google.inject.Inject
import connectors.GoodsRecordConnector
import controllers.BaseController
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import models.helper.CategorisationJourney
import models.ott.CategorisationInfo
import models.requests.DataRequest
import models.{CategorisationAnswers, CategoryRecord, NormalMode, UserAnswers, ValidationError}
import navigation.CategorisationNavigator
import pages.categorisation.CyaCategorisationPage
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import services.{AuditService, CategorisationService, DataCleansingService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.checkAnswers.{AssessmentsSummary, HasSupplementaryUnitSummary, LongerCommodityCodeSummary, SupplementaryUnitSummary}
import viewmodels.govuk.summarylist._
import views.html.categorisation.CyaCategorisationView

import scala.concurrent.{ExecutionContext, Future}

class CyaCategorisationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaCategorisationView,
  goodsRecordConnector: GoodsRecordConnector,
  dataCleansingService: DataCleansingService,
  auditService: AuditService,
  navigator: CategorisationNavigator,
  categorisationService: CategorisationService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String = "Unable to update Goods Profile."

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val longerCategorisationInfo = request.userAnswers.get(LongerCategorisationDetailsQuery(recordId))

      longerCategorisationInfo match {
        case Some(categorisationInfo) =>
          showCyaPage(request, recordId, categorisationInfo)
        case _                        =>
          val categorisationInfo = request.userAnswers.get(CategorisationDetailsQuery(recordId))

          categorisationInfo
            .map { info =>
              showCyaPage(request, recordId, info)
            }
            .getOrElse {
              dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney)
              logErrorsAndContinue(
                "Failed to get categorisation details",
                controllers.categorisation.routes.CategorisationPreparationController.startCategorisation(recordId)
              )
            }
      }
    }

  private def showCyaPage(
    request: DataRequest[_],
    recordId: String,
    categoryInfo: CategorisationInfo
  )(implicit messages: Messages) = {
    val userAnswers = request.userAnswers

    CategorisationAnswers.build(userAnswers, recordId) match {
      case Right(_) =>
        val (categorisationList, supplementaryUnitList, longerCommodityCodeList) =
          buildSummaryLists(userAnswers, recordId, categoryInfo)

        Ok(
          view(
            recordId,
            categoryInfo.commodityCode,
            categorisationList,
            supplementaryUnitList,
            longerCommodityCodeList
          )(request, messages)
        )

      case Left(errors) =>
        handleError(request, recordId, errors)
    }
  }

  private def buildSummaryLists(
    userAnswers: UserAnswers,
    recordId: String,
    categoryInfo: CategorisationInfo
  )(implicit messages: Messages): (SummaryList, SummaryList, SummaryList) = {

    val categorisationRows = categoryInfo.categoryAssessmentsThatNeedAnswers.flatMap { assessment =>
      AssessmentsSummary.row(
        recordId,
        userAnswers,
        assessment,
        categoryInfo.categoryAssessmentsThatNeedAnswers.indexOf(assessment),
        categoryInfo.longerCode
      )
    }

    val categorisationList = SummaryListViewModel(rows = categorisationRows)

    val supplementaryUnitList = SummaryListViewModel(
      rows = Seq(
        HasSupplementaryUnitSummary.row(userAnswers, recordId),
        SupplementaryUnitSummary.row(userAnswers, recordId)
      ).flatten
    )

    val longerCommodityCodeList = SummaryListViewModel(
      rows = Seq(
        LongerCommodityCodeSummary.row(userAnswers, recordId)
      ).flatten
    )

    (categorisationList, supplementaryUnitList, longerCommodityCodeList)
  }

  private def handleError(
    request: DataRequest[_],
    recordId: String,
    errors: data.NonEmptyChain[ValidationError]
  ) = {
    dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney)
    logErrorsAndContinue(
      errorMessage,
      controllers.categorisation.routes.CategorisationPreparationController.startCategorisation(recordId),
      errors
    )
  }

  def onSubmit(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      CategoryRecord
        .build(
          request.userAnswers,
          request.eori,
          recordId,
          categorisationService
        ) match {
        case Right(categoryRecord) =>
          auditService.auditFinishCategorisation(request.eori, request.affinityGroup, recordId, categoryRecord)
          for {
            oldRecord <- goodsRecordConnector.getRecord(recordId)
            _         <-
              goodsRecordConnector
                .updateCategoryAndComcodeForGoodsRecord(recordId, categoryRecord, oldRecord)
            _         <- dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney)
          } yield Redirect(
            navigator.nextPage(
              CyaCategorisationPage(recordId),
              NormalMode,
              request.userAnswers
            )
          )
        case Left(errors)          =>
          dataCleansingService.deleteMongoData(request.userAnswers.id, CategorisationJourney)
          Future.successful(
            logErrorsAndContinue(
              errorMessage,
              controllers.categorisation.routes.CategorisationPreparationController.startCategorisation(recordId),
              errors
            )
          )
      }
    }
}
