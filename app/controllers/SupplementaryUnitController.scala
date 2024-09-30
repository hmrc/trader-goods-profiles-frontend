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
import models.Mode
import models.helper.SupplementaryUnitUpdate
import navigation.Navigator
import pages.{HasSupplementaryUnitUpdatePage, SupplementaryUnitPage, SupplementaryUnitUpdatePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery, MeasurementQuery}
import repositories.SessionRepository
import services.{AuditService, OttService}
import utils.SessionData.{dataRemoved, dataUpdated, initialValueOfSuppUnit, pageUpdated}
import views.html.SupplementaryUnitView

import javax.inject.Inject
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
  view: SupplementaryUnitView,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(SupplementaryUnitPage(recordId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }
      val catInfo      = request.userAnswers.get(LongerCategorisationDetailsQuery(recordId)) match {
        case Some(catInfo) => Some(catInfo)
        case _             => request.userAnswers.get(CategorisationDetailsQuery(recordId))
      }
      catInfo
        .map { categorisationInfo =>
          val measurementUnit = categorisationInfo.measurementUnit
          val submitAction    = routes.SupplementaryUnitController.onSubmit(mode, recordId)
          Ok(view(preparedForm, mode, recordId, measurementUnit, submitAction))
        }
        .getOrElse(navigator.journeyRecovery())
    }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val catInfo = request.userAnswers.get(LongerCategorisationDetailsQuery(recordId)) match {
              case Some(catInfo) => Some(catInfo)
              case _             => request.userAnswers.get(CategorisationDetailsQuery(recordId))
            }

            catInfo
              .map { categorisationInfo =>
                val measurementUnit = categorisationInfo.measurementUnit
                val submitAction    = routes.SupplementaryUnitController.onSubmit(mode, recordId)
                Future.successful(BadRequest(view(formWithErrors, mode, recordId, measurementUnit, submitAction)))
              }
              .getOrElse(Future.successful(navigator.journeyRecovery()))
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
      request.userAnswers.get(HasSupplementaryUnitUpdatePage(recordId)) match {
        case None =>
          auditService
            .auditStartUpdateGoodsRecord(
              request.eori,
              request.affinityGroup,
              SupplementaryUnitUpdate,
              recordId
            )
        case _    =>
      }

      val userAnswerValue = request.userAnswers.get(SupplementaryUnitUpdatePage(recordId))

      goodsRecordConnector
        .getRecord(request.eori, recordId)
        .flatMap { record =>
          val initialValue: String = record.supplementaryUnit match {
            case Some(value) if value != 0 => value.toString
            case _                         => ""
          }
          val measurementUnit      = record.measurementUnit

          val preparedFormFuture = userAnswerValue match {
            case Some(value) =>
              Future.successful((form.fill(value), measurementUnit))
            case None        =>
              Future.successful((form.fill(initialValue), measurementUnit))
          }

          preparedFormFuture.map { case (preparedForm, measurementUnit) =>
            val onSubmitAction: Call = routes.SupplementaryUnitController.onSubmitUpdate(mode, recordId)
            Ok(view(preparedForm, mode, recordId, measurementUnit, onSubmitAction))
              .addingToSession(
                initialValueOfSuppUnit -> initialValue
              )
              .removingFromSession(dataUpdated, pageUpdated, dataRemoved)
          }
        }
        .recover { case ex: Exception =>
          logger.error(s"Error occurred while fetching record for recordId: $recordId", ex)
          navigator.journeyRecovery()
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
              val measurementUnit = value
              BadRequest(view(formWithErrors, mode, recordId, measurementUnit, onSubmitAction))
            }
            result.recover { case ex: Exception =>
              logger.error(s"Error occurred while fetching measurement unit for recordId: $recordId", ex)
              navigator.journeyRecovery()
            }
          },
          value =>
            for {
              measurementUnit               <- ottService.getMeasurementUnit(request, recordId)
              updatedAnswers                <- Future.fromTry(request.userAnswers.set(SupplementaryUnitUpdatePage(recordId), value))
              updatedAnswersWithMeasurement <-
                Future.fromTry(updatedAnswers.set(MeasurementQuery(recordId), measurementUnit.getOrElse("")))
              _                             <- sessionRepository.set(updatedAnswersWithMeasurement)
            } yield Redirect(
              navigator.nextPage(SupplementaryUnitUpdatePage(recordId), mode, updatedAnswersWithMeasurement)
            )
        )
    }
}
