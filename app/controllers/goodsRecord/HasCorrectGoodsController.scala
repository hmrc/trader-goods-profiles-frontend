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

package controllers.goodsRecord

import controllers.BaseController
import controllers.actions._
import forms.goodsRecord.HasCorrectGoodsFormProvider
import models.Mode
import navigation.Navigator
import pages._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries._
import repositories.SessionRepository
import views.html.goodsRecord.HasCorrectGoodsView

import javax.inject.Inject
import scala.annotation.unused
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
)(implicit @unused ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = prepareForm(HasCorrectGoodsPage, form)
      val submitAction = controllers.goodsRecord.routes.HasCorrectGoodsController.onSubmitCreate(mode)
      request.userAnswers.get(CommodityQuery) match {
        case Some(commodity) => Ok(view(preparedForm, commodity, submitAction))
        case None            => navigator.journeyRecovery()
      }
  }

  def onPageLoadUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(HasCorrectGoodsCommodityCodeUpdatePage(recordId), form)
      val submitAction = controllers.goodsRecord.routes.HasCorrectGoodsController.onSubmitUpdate(mode, recordId)
      request.userAnswers.get(CommodityUpdateQuery(recordId)) match {
        case Some(commodity) => Ok(view(preparedForm, commodity, submitAction))
        case None            => navigator.journeyRecovery()
      }
    }

  def onPageLoadLongerCommodityCode(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(HasCorrectGoodsLongerCommodityCodePage(recordId), form)
      val submitAction = controllers.goodsRecord.routes.HasCorrectGoodsController.onSubmitLongerCommodityCode(mode, recordId)
      request.userAnswers.get(LongerCommodityQuery(recordId)) match {
        case Some(commodity) =>
          Ok(
            view(preparedForm, commodity, submitAction)
          )
        case None            => navigator.journeyRecovery()
      }
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val submitAction = controllers.goodsRecord.routes.HasCorrectGoodsController.onSubmitCreate(mode)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(CommodityQuery) match {
              case Some(commodity) => Future.successful(BadRequest(view(formWithErrors, commodity, submitAction)))
              case None            => Future.successful(navigator.journeyRecovery())
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasCorrectGoodsPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasCorrectGoodsPage, mode, updatedAnswers))
        )
  }

  def onSubmitLongerCommodityCode(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val submitAction = controllers.goodsRecord.routes.HasCorrectGoodsController.onSubmitLongerCommodityCode(mode, recordId)

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(LongerCommodityQuery(recordId)) match {
              case Some(commodity) =>
                Future.successful(
                  BadRequest(
                    view(formWithErrors, commodity, submitAction)
                  )
                )
              case None            => Future.successful(navigator.journeyRecovery())
            },
          value =>
            for {
              updatedAnswers <-
                Future.fromTry(request.userAnswers.set(HasCorrectGoodsLongerCommodityCodePage(recordId), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(HasCorrectGoodsLongerCommodityCodePage(recordId), mode, updatedAnswers)
            )
        )
    }

  def onSubmitUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val submitAction = controllers.goodsRecord.routes.HasCorrectGoodsController.onSubmitUpdate(mode, recordId)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(CommodityUpdateQuery(recordId)) match {
              case Some(commodity) => Future.successful(BadRequest(view(formWithErrors, commodity, submitAction)))
              case None            => Future.successful(navigator.journeyRecovery())
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
