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

import controllers.actions._
import forms.HasCorrectGoodsFormProvider
import models.Mode
import navigation.Navigator
import pages.{HasCorrectGoodsCommodityCodeUpdatePage, HasCorrectGoodsPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{CommodityCodeUpdateQuery, CommodityQuery}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.HasCorrectGoodsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasCorrectGoodsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: HasCorrectGoodsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: HasCorrectGoodsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(HasCorrectGoodsPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      val submitAction = routes.HasCorrectGoodsController.onSubmitCreate(mode)
      request.userAnswers.get(CommodityQuery) match {
        case Some(commodity) => Ok(view(preparedForm, commodity, submitAction))
        case None            => Redirect(routes.JourneyRecoveryController.onPageLoad().url)
      }
  }

  def onPageLoadUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(HasCorrectGoodsCommodityCodeUpdatePage(recordId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      val submitAction = routes.HasCorrectGoodsController.onSubmitUpdate(mode, recordId)
      request.userAnswers.get(CommodityCodeUpdateQuery(recordId)) match {
        case Some(commodity) => Ok(view(preparedForm, commodity, submitAction))
        case None            => Redirect(routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

  // TODO - this is still not functional, it is just to create the url. Implement this properly
  def onPageLoadLongerCommodityCode(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(HasCorrectGoodsPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }
      //TODO - use the UpdateCommdityQuery (with recordId) here when available
      val submitAction = routes.HasCorrectGoodsController.onSubmitCreate(mode)
      request.userAnswers.get(CommodityQuery) match {
        case Some(commodity) => Ok(view(preparedForm, commodity, submitAction))
        case None            => Redirect(routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent]                                = (identify andThen getData andThen requireData).async {
    implicit request =>
      val submitAction = routes.HasCorrectGoodsController.onSubmitCreate(mode)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(CommodityQuery) match {
              case Some(commodity) => Future.successful(BadRequest(view(formWithErrors, commodity, submitAction)))
              case None            => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasCorrectGoodsPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasCorrectGoodsPage, mode, updatedAnswers))
        )
  }
  // TODO - this is still not functional, it is just to create the url. Implement this properly
  def onSubmitLongerCommodityCode(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val submitAction = routes.HasCorrectGoodsController.onSubmitCreate(mode)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            //TODO - use the UpdateCommdityQuery (with recordId) here when available
            request.userAnswers.get(CommodityQuery) match {
              case Some(commodity) => Future.successful(BadRequest(view(formWithErrors, commodity, submitAction)))
              case None            => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasCorrectGoodsPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasCorrectGoodsPage, mode, updatedAnswers))
        )
    }

  def onSubmitUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val submitAction = routes.HasCorrectGoodsController.onSubmitUpdate(mode, recordId)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(CommodityCodeUpdateQuery(recordId)) match {
              case Some(commodity) => Future.successful(BadRequest(view(formWithErrors, commodity, submitAction)))
              case None            => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
            },
          value =>
            for {
              updatedAnswers <-
                Future.fromTry(request.userAnswers.set(HasCorrectGoodsCommodityCodeUpdatePage(recordId), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasCorrectGoodsCommodityCodeUpdatePage(recordId), mode, updatedAnswers))
        )
    }
}
