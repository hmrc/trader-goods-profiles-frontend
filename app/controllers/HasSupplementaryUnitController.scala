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
import forms.HasSupplementaryUnitFormProvider
import models.Mode
import navigation.Navigator
import pages.{HasSupplementaryUnitPage, HasSupplementaryUnitUpdatePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import queries.RecategorisingQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.SessionData.{dataRemoved, dataUpdated, initialValueOfHasSuppUnit, initialValueOfSuppUnit, pageUpdated}
import views.html.HasSupplementaryUnitView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasSupplementaryUnitController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  goodsRecordConnector: GoodsRecordConnector,
  formProvider: HasSupplementaryUnitFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: HasSupplementaryUnitView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad2(mode: Mode, recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(HasSupplementaryUnitPage(recordId)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, recordId))
  }

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData) {
    implicit request =>
      for {
        updatedUA <- Future.fromTry(request.userAnswers.set(RecategorisingQuery(recordId), false))
        _         <- sessionRepository.set(updatedUA)
      } yield updatedUA

      val preparedForm = request.userAnswers.get(HasSupplementaryUnitPage(recordId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      val onSubmitAction: Call = routes.HasSupplementaryUnitController.onSubmit(mode, recordId)

      Ok(view(preparedForm, mode, recordId, onSubmitAction))
    }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction: Call = routes.HasSupplementaryUnitController.onSubmit(mode, recordId)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, recordId, onSubmitAction))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasSupplementaryUnitPage(recordId), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasSupplementaryUnitPage(recordId), mode, updatedAnswers))
        )
    }

  def onPageLoadUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val userAnswerValue = request.userAnswers.get(HasSupplementaryUnitUpdatePage(recordId))

      goodsRecordConnector.getRecord(request.eori, recordId).flatMap { record =>
        val initialValue = record.supplementaryUnit.exists(_ != 0)

        val preparedFormFuture = userAnswerValue match {
          case Some(value) =>
            Future.successful(form.fill(value))
          case None        =>
            Future.successful(form.fill(initialValue))
        }
        preparedFormFuture.map { preparedForm =>
          val onSubmitAction: Call = routes.HasSupplementaryUnitController.onSubmitUpdate(mode, recordId)
          Ok(view(preparedForm, mode, recordId, onSubmitAction))
            .addingToSession(
              initialValueOfHasSuppUnit -> initialValue.toString
            )
            .removingFromSession(dataUpdated, pageUpdated, dataRemoved, initialValueOfSuppUnit)
        }
      }
    }

  def onSubmitUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction: Call = routes.HasSupplementaryUnitController.onSubmitUpdate(mode, recordId)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, recordId, onSubmitAction))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasSupplementaryUnitUpdatePage(recordId), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasSupplementaryUnitUpdatePage(recordId), mode, updatedAnswers))
        )
    }
}
