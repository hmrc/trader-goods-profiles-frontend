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
import cats.data.NonEmptyChain
import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import logging.Logging
import models.AssessmentAnswer.NoExemption
import models.{AssessmentAnswer, CategorisationAnswers, NormalMode, UserAnswers, ValidationError}
import pages.{AssessmentPage, HasSupplementaryUnitPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import queries.CategorisationQuery
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{AssessmentsSummary, HasSupplementaryUnitSummary, SupplementaryUnitSummary}
import viewmodels.govuk.summarylist._
import views.html.CyaCategorisationView

class CyaCategorisationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaCategorisationView
) extends FrontendBaseController
    with I18nSupport
    with Logging {


  //TODO test cases to retest
  // No hasSuppUnit but suppUnit
  // hasSuppUnit false but SuppUnit
  // assessment1 = ex, 2 = ex, 3 = None, 4 = ex - error
  // assessment1 = ex, 2 = None, 4 = ex - error
  // assessment1 = ex, 2 = (not answered) - error

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      CategorisationAnswers.build(request.userAnswers) match {
        case Right(_) =>

//TODO move all this validation into the CategorisationAnswers build
          val categoriesThatAreValid = request.userAnswers.get(CategorisationQuery) match {
            case Some(categorisationInfo) =>
              categorisationInfo.categoryAssessments.flatMap(
                assessment => request.userAnswers.get(AssessmentPage(assessment.id)).map(ass => categorisationInfo.categoryAssessments.indexOf(assessment) + 1)
              )

          }

          val noNoneBeforeTheEnd = request.userAnswers.get(CategorisationQuery) match {
            case Some(categorisationInfo) =>
              val values = categorisationInfo.categoryAssessments.flatMap(
                assessment => request.userAnswers.get(AssessmentPage(assessment.id)).map(value => request.userAnswers.get(AssessmentPage(assessment.id)))
              ).flatten

              !values.reverse.tail.contains(NoExemption)
          }


              val categorisationRows = request.userAnswers.get(CategorisationQuery) match {
            case Some(categorisationInfo) =>
              categorisationInfo.categoryAssessments
                .flatMap(assessment => AssessmentsSummary.row(
                  request.userAnswers,
                  assessment.id,
                  categorisationInfo.categoryAssessments.indexOf(assessment) + 1,
                  categorisationInfo.categoryAssessments.size,
                  assessment.exemptions
                ))
          }

          if (noNoneBeforeTheEnd && categoriesThatAreValid.zipWithIndex.takeWhile(x => x._1 == x._2 + 1).size == categorisationRows.size) {
            //happy

            val categorisationList = SummaryListViewModel(
              rows = categorisationRows
            )
            val supplementaryUnitList = SummaryListViewModel(
              rows = Seq(
                HasSupplementaryUnitSummary.row(request.userAnswers, recordId),
                SupplementaryUnitSummary.row(request.userAnswers)
              ).flatten
            )

            Ok(view(recordId, categorisationList, supplementaryUnitList))
          } else {
            logErrorsAndContinue(NonEmptyChain.fromSeq(Seq.empty[ValidationError]).get)
          }
        case Left(errors) =>
          logErrorsAndContinue(errors)

      }
  }

  def onSubmit(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      Redirect(routes.IndexController.onPageLoad)
  }

  def logErrorsAndContinue(errors: data.NonEmptyChain[ValidationError]): Result = {
  //def logErrorsAndContinue(): Result = {

    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    val continueUrl = RedirectUrl(routes.ProfileSetupController.onPageLoad().url)

    logger.warn(s"Unable to create Trader profile.  Missing pages: $errorMessages")
    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }

}
