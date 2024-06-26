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

import cats.data
import com.google.inject.Inject
import connectors.GoodsRecordConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import logging.Logging
import models.{CategorisationAnswers, CategoryRecord, NormalMode, Scenario, ValidationError}
import navigation.Navigator
import pages.CyaCategorisationPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.RecordCategorisationsQuery
import services.AuditService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{AssessmentsSummary, HasSupplementaryUnitSummary, SupplementaryUnitSummary}
import viewmodels.govuk.summarylist._
import views.html.CyaCategorisationView

import scala.concurrent.{ExecutionContext, Future}

class CyaCategorisationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaCategorisationView,
  goodsRecordConnector: GoodsRecordConnector,
  auditService: AuditService,
  navigator: Navigator
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      CategorisationAnswers.build(request.userAnswers, recordId) match {
        case Right(_) =>
          val categorisationAnswers = for {
            recordCategorisations <- request.userAnswers.get(RecordCategorisationsQuery)
            categorisationAnswers <- recordCategorisations.records.get(recordId)
          } yield categorisationAnswers

          val categorisationRows = categorisationAnswers match {
            case Some(categorisationInfo) =>
              categorisationInfo.categoryAssessments
                .flatMap(assessment =>
                  AssessmentsSummary.row(
                    recordId,
                    request.userAnswers,
                    assessment,
                    categorisationInfo.categoryAssessments.indexOf(assessment),
                    categorisationInfo.categoryAssessments.size
                  )
                )
            case None                     => Seq.empty
          }

          val categorisationList = SummaryListViewModel(
            rows = categorisationRows
          )

          val supplementaryUnitList = SummaryListViewModel(
            rows = Seq(
              HasSupplementaryUnitSummary.row(request.userAnswers, recordId),
              SupplementaryUnitSummary.row(request.userAnswers, recordId)
            ).flatten
          )

          Ok(view(recordId, categorisationList, supplementaryUnitList))

        case Left(errors) =>
          logErrorsAndContinue(errors, recordId)

      }
  }

  def onSubmit(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      CategoryRecord.build(request.userAnswers, request.eori, recordId) match {
        case Right(model) =>
          auditService.auditFinishCategorisation(
            request.eori,
            request.affinityGroup,
            recordId,
            model.categoryAssessmentsWithExemptions,
            model.category
          )

          goodsRecordConnector.updateGoodsRecord(request.eori, recordId, model).map { _ =>
            Redirect(
              navigator.nextPage(
                CyaCategorisationPage(recordId, model, Scenario.getScenario(model)),
                NormalMode,
                request.userAnswers
              )
            )
          }
        case Left(errors) => Future.successful(logErrorsAndContinue(errors, recordId))
      }
  }

  def logErrorsAndContinue(errors: data.NonEmptyChain[ValidationError], recordId: String): Result = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    val continueUrl = RedirectUrl(routes.CategoryGuidanceController.onPageLoad(recordId).url)

    logger.warn(s"Unable to update Goods Profile.  Missing pages: $errorMessages")
    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }
}
