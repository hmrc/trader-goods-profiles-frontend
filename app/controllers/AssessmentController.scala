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

import connectors.GoodsRecordConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.AssessmentFormProvider
import models.helper.CategorisationJourney
import models.{Mode, ReassessmentAnswer}
import navigation.Navigator
import pages.{AssessmentPage, ReassessmentPage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery, LongerCommodityQuery}
import repositories.SessionRepository
import services.{CategorisationService, DataCleansingService}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants
import views.html.AssessmentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AssessmentController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AssessmentFormProvider,
  dataCleansingService: DataCleansingService,
  categorisationService: CategorisationService,
  goodsRecordsConnector: GoodsRecordConnector,
  val controllerComponents: MessagesControllerComponents,
  view: AssessmentView
)(implicit ec: ExecutionContext)
    extends BaseController {

  private def getIndex(number: Int, userAnswerId: String, recordId: String): Either[Result, Int] =
    if (number < Constants.firstAssessmentNumber) {
      Left(handleDataCleansingAndRecovery(userAnswerId, recordId))
    } else {
      Right(number - 1)
    }

  def onPageLoad(mode: Mode, recordId: String, number: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      getIndex(number, request.userAnswers.id, recordId) match {
        case Right(index) =>
          request.userAnswers
            .get(CategorisationDetailsQuery(recordId))
            .flatMap { categorisationInfo =>
              categorisationInfo.getAssessmentFromIndex(index).map { assessment =>
                val codesAndDescriptions = assessment.getCodesZippedWithDescriptions
                val preparedForm         = prepareForm(AssessmentPage(recordId, index), formProvider())
                val submitAction         = routes.AssessmentController.onSubmit(mode, recordId, number)
                Ok(
                  view(
                    preparedForm,
                    mode,
                    recordId,
                    number,
                    codesAndDescriptions,
                    categorisationInfo.commodityCode,
                    submitAction,
                    assessment.themeDescription,
                    categorisationInfo.categoryAssessmentsThatNeedAnswers.size
                  )
                )
              }
            }
            .getOrElse(handleDataCleansingAndRecovery(request.userAnswers.id, recordId))
        case Left(result) => result
      }
    }

  def onPageLoadReassessment(mode: Mode, recordId: String, number: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      getIndex(number, request.userAnswers.id, recordId) match {
        case Right(index) =>
          request.userAnswers
            .get(LongerCategorisationDetailsQuery(recordId))
            .flatMap { categorisationInfo =>
              categorisationInfo.getAssessmentFromIndex(index).map { assessment =>
                val codesAndDescriptions = assessment.getCodesZippedWithDescriptions
                val preparedForm         = prepareForm(ReassessmentPage(recordId, index), formProvider())

                val submitAction = routes.AssessmentController.onSubmitReassessment(mode, recordId, number)

                Ok(
                  view(
                    preparedForm,
                    mode,
                    recordId,
                    number,
                    codesAndDescriptions,
                    categorisationInfo.commodityCode,
                    submitAction,
                    assessment.themeDescription,
                    categorisationInfo.categoryAssessmentsThatNeedAnswers.size
                  )
                )
              }
            }
            .getOrElse(handleDataCleansingAndRecovery(request.userAnswers.id, recordId))
        case Left(result) => result
      }
    }

  def onSubmit(mode: Mode, recordId: String, number: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      getIndex(number, request.userAnswers.id, recordId) match {
        case Right(index) =>
          request.userAnswers
            .get(CategorisationDetailsQuery(recordId))
            .flatMap { categorisationInfo =>
              categorisationInfo.getAssessmentFromIndex(index).map { assessment =>
                val codesAndDescriptions = assessment.getCodesZippedWithDescriptions
                val form                 = formProvider()
                val submitAction         = routes.AssessmentController.onSubmit(mode, recordId, number)

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
                            number,
                            codesAndDescriptions,
                            categorisationInfo.commodityCode,
                            submitAction,
                            assessment.themeDescription,
                            categorisationInfo.categoryAssessmentsThatNeedAnswers.size
                          )
                        )
                      ),
                    value =>
                      for {
                        updatedAnswers <-
                          Future.fromTry(request.userAnswers.set(AssessmentPage(recordId, index), value))
                        _              <- sessionRepository.set(updatedAnswers)
                      } yield Redirect(
                        navigator.nextPage(AssessmentPage(recordId, index), mode, updatedAnswers)
                      )
                  )
              }
            }
            .getOrElse(
              Future.successful(handleDataCleansingAndRecovery(request.userAnswers.id, recordId))
            )
            .recover(_ => handleDataCleansingAndRecovery(request.userAnswers.id, recordId))
        case Left(result) => Future.successful(result)
      }
    }

  def onSubmitReassessment(mode: Mode, recordId: String, number: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      getIndex(number, request.userAnswers.id, recordId) match {
        case Right(index) =>
          request.userAnswers
            .get(LongerCategorisationDetailsQuery(recordId))
            .flatMap { categorisationInfo =>
              categorisationInfo.getAssessmentFromIndex(index).map { assessment =>
                val codesAndDescriptions = assessment.getCodesZippedWithDescriptions
                val form                 = formProvider()
                val submitAction         = routes.AssessmentController.onSubmitReassessment(mode, recordId, number)

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
                            number,
                            codesAndDescriptions,
                            categorisationInfo.commodityCode,
                            submitAction,
                            assessment.themeDescription,
                            categorisationInfo.categoryAssessmentsThatNeedAnswers.size
                          )
                        )
                      ),
                    value =>
                      for {
                        goodsRecord                 <- goodsRecordsConnector.getRecord(request.eori, recordId)
                        longerComCode               <-
                          Future.fromTry(Try(request.userAnswers.get(LongerCommodityQuery(recordId)).get))
                        newLongerCategorisationInfo <-
                          categorisationService
                            .getCategorisationInfo(
                              request,
                              longerComCode.commodityCode,
                              goodsRecord.countryOfOrigin,
                              recordId,
                              longerCode = true
                            )
                        updatedAnswers              <-
                          Future
                            .fromTry(
                              request.userAnswers
                                .set(ReassessmentPage(recordId, index), ReassessmentAnswer(value))
                            )
                        updatedUACatInfo            <-
                          Future.fromTry(
                            updatedAnswers.set(LongerCategorisationDetailsQuery(recordId), newLongerCategorisationInfo)
                          )
                        _                           <- sessionRepository.set(updatedUACatInfo)
                      } yield Redirect(
                        navigator.nextPage(ReassessmentPage(recordId, index), mode, updatedUACatInfo)
                      )
                  )
              }
            }
            .getOrElse(Future.successful(handleDataCleansingAndRecovery(request.userAnswers.id, recordId)))
            .recover(_ => handleDataCleansingAndRecovery(request.userAnswers.id, recordId))
        case Left(result) => Future.successful(result)
      }
    }

  private def handleDataCleansingAndRecovery(userAnswersId: String, recordId: String) = {
    dataCleansingService.deleteMongoData(userAnswersId, CategorisationJourney)
    navigator.journeyRecovery(
      Some(RedirectUrl(routes.CategorisationPreparationController.startCategorisation(recordId).url))
    )
  }
}
