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

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import logging.Logging
import models.{AssessmentAnswer, NormalMode, UserAnswers}
import pages.AssessmentPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import queries.CategorisationQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.AssessmentsSummary
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

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      // TODO an example of how to build the rows - might help
//      val categorisationRows = request.userAnswers.get(CategorisationQuery) match {
//        case Some(categorisationInfo) =>
//          categorisationInfo.categoryAssessments
//            .flatMap(assessment => AssessmentsSummary.row(request.userAnswers, assessment.id))
//      }

      val categorisationList    = SummaryListViewModel(
        rows = Seq.empty
      )
      val supplementaryUnitList = SummaryListViewModel(
        rows = Seq.empty
      )

      Ok(view(recordId, categorisationList, supplementaryUnitList))
  }

  def onSubmit(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      Redirect(routes.IndexController.onPageLoad)
  }

}
