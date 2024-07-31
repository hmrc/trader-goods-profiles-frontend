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
import controllers.actions._
import forms.SupplementaryUnitFormProvider

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.{SupplementaryUnitPage, SupplementaryUnitUpdatePage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import queries.{MeasurementQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import services.OttService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SupplementaryUnitView

import scala.concurrent.{ExecutionContext, Future}

class SupplementaryUnitController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  goodsRecordConnector: GoodsRecordConnector,
  profileAuth: ProfileAuthenticateAction,
  formProvider: SupplementaryUnitFormProvider,
  ottService: OttService,
  val controllerComponents: MessagesControllerComponents,
  view: SupplementaryUnitView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form = formProvider()

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(SupplementaryUnitPage(recordId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      val result = for {
        query              <- request.userAnswers.get(RecordCategorisationsQuery)
        categorisationInfo <- query.records.get(recordId)
      } yield {
        val measurementUnit = categorisationInfo.measurementUnit.getOrElse("")
        val submitAction    = routes.SupplementaryUnitController.onSubmit(mode, recordId)
        Ok(view(preparedForm, mode, recordId, measurementUnit, submitAction))
      }

      result.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
    }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction: Call = routes.SupplementaryUnitController.onSubmit(mode, recordId)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val result = for {
              query              <- request.userAnswers.get(RecordCategorisationsQuery)
              categorisationInfo <- query.records.get(recordId)
            } yield {
              val measurementUnit = categorisationInfo.measurementUnit.getOrElse("")
              Future.successful(BadRequest(view(formWithErrors, mode, recordId, measurementUnit, onSubmitAction)))
            }
            result.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url)))
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(SupplementaryUnitPage(recordId), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(SupplementaryUnitPage(recordId), mode, updatedAnswers))
        )
    }

  def onPageLoadUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val userAnswerValue = request.userAnswers.get(SupplementaryUnitUpdatePage(recordId))

      val preparedFormFuture: Future[(Form[String], String)] = userAnswerValue match {
        case Some(value) =>
          Future.successful((form.fill(value), ""))
        case None        =>
          goodsRecordConnector.getRecord(request.eori, recordId).map { record =>
            val formValue: String = record.supplementaryUnit match {
              case Some(value) if value != 0 => value.toString
              case _                         => ""
            }
            val measurementUnit   = record.measurementUnit.getOrElse("")
            (form.fill(formValue), measurementUnit)
          }
      }

      preparedFormFuture
        .map { case (preparedForm, measurementUnit) =>
          val onSubmitAction: Call = routes.SupplementaryUnitController.onSubmitUpdate(mode, recordId)
          Ok(view(preparedForm, mode, recordId, measurementUnit, onSubmitAction))
        }
        .recover { case ex: Exception =>
          logger.error(s"Error occurred while fetching record for recordId: $recordId", ex)
          Redirect(routes.JourneyRecoveryController.onPageLoad().url)
        }
    }

  def onSubmitUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction: Call = routes.SupplementaryUnitController.onSubmitUpdate(mode, recordId)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val result = for {
              value <- ottService.getMeasurementUnit(request, recordId)
            } yield {
              val measurementUnit = value.getOrElse("")
              BadRequest(view(formWithErrors, mode, recordId, measurementUnit, onSubmitAction))
            }
            result.recover { case ex: Exception =>
              logger.error(s"Error occurred while fetching measurement unit for recordId: $recordId", ex)
              Redirect(routes.JourneyRecoveryController.onPageLoad().url)
            }
          },
          value =>
            for {
              measurementUnit               <- ottService.getMeasurementUnit(request, recordId)
              updatedAnswers                <- Future.fromTry(request.userAnswers.set(SupplementaryUnitUpdatePage(recordId), value))
              updatedAnswersWithMeasurement <-
                Future.fromTry(updatedAnswers.set(MeasurementQuery, measurementUnit.getOrElse("")))
              _                             <- sessionRepository.set(updatedAnswersWithMeasurement)
            } yield Redirect(
              navigator.nextPage(SupplementaryUnitUpdatePage(recordId), mode, updatedAnswersWithMeasurement)
            )
        )
    }
}
