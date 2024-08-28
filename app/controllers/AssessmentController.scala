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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.AssessmentFormProvider
import models.Mode
import navigation.Navigator
import pages.{AssessmentPage, ReassessmentPage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery}
import repositories.SessionRepository
import views.html.AssessmentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AssessmentController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AssessmentFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: AssessmentView
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      request.userAnswers
        .get(CategorisationDetailsQuery(recordId))
        .flatMap { categorisationInfo =>
          categorisationInfo.getAssessmentFromIndex(index).map { assessment =>
            val listItems = assessment.getExemptionListItems
            val form      = formProvider(listItems.size)

            val preparedForm = request.userAnswers.get(AssessmentPage(recordId, index)) match {
              case Some(value) => form.fill(value)
              case None        => form
            }

            val submitAction = routes.AssessmentController.onSubmit(mode, recordId, index)
            Ok(view(preparedForm, mode, recordId, index, listItems, categorisationInfo.commodityCode, submitAction))
          }
        }
        .getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }

  def onPageLoadReassessment(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      request.userAnswers
        .get(LongerCategorisationDetailsQuery(recordId))
        .flatMap { categorisationInfo =>
          categorisationInfo.getAssessmentFromIndex(index).map { assessment =>
            val listItems = assessment.getExemptionListItems
            val form      = formProvider(listItems.size)

            val preparedForm = request.userAnswers.get(ReassessmentPage(recordId, index)) match {
              case Some(value) => form.fill(value)
              case None        => form
            }

            val submitAction = routes.AssessmentController.onSubmitReassessment(mode, recordId, index)

            Ok(view(preparedForm, mode, recordId, index, listItems, categorisationInfo.commodityCode, submitAction))
          }
        }
        .getOrElse(navigator.journeyRecovery())
    }

  def onSubmit(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers
        .get(CategorisationDetailsQuery(recordId))
        .flatMap { categorisationInfo =>
          categorisationInfo.getAssessmentFromIndex(index).map { assessment =>
            val listItems    = assessment.getExemptionListItems
            val form         = formProvider(listItems.size)
            val submitAction = routes.AssessmentController.onSubmit(mode, recordId, index)

            form
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      view(
                        formWithErrors,
                        mode,
                        recordId,
                        index,
                        listItems,
                        categorisationInfo.commodityCode,
                        submitAction
                      )
                    )
                  ),
                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(AssessmentPage(recordId, index), value))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(navigator.nextPage(AssessmentPage(recordId, index), mode, updatedAnswers))
              )
          }
        }
        .getOrElse(Future.successful(navigator.journeyRecovery())
        .recover(_ => navigator.journeyRecovery())
    }

  def onSubmitReassessment(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers
        .get(LongerCategorisationDetailsQuery(recordId))
        .flatMap { categorisationInfo =>
          categorisationInfo.getAssessmentFromIndex(index).map { assessment =>
            val listItems    = assessment.getExemptionListItems
            val form         = formProvider(listItems.size)
            val submitAction = routes.AssessmentController.onSubmitReassessment(mode, recordId, index)

            form
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      view(
                        formWithErrors,
                        mode,
                        recordId,
                        index,
                        listItems,
                        categorisationInfo.commodityCode,
                        submitAction
                      )
                    )
                  ),
                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(ReassessmentPage(recordId, index), value))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(navigator.nextPage(ReassessmentPage(recordId, index), mode, updatedAnswers))
              )
          }
        }
        .getOrElse(Future.successful(navigator.journeyRecovery()))
        .recover(_ => navigator.journeyRecovery())
    }

}
