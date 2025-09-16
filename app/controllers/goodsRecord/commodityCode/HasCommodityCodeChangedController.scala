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

package controllers.goodsRecord.commodityCode

import connectors.GoodsRecordConnector
import controllers.BaseController
import controllers.actions.*
import forms.goodsRecord.HasCommodityCodeChangeFormProvider
import models.AdviceStatus.AdviceReceived
import models.Mode
import models.helper.GoodsDetailsUpdate
import navigation.GoodsRecordNavigator
import pages.goodsRecord.{CommodityCodePage, CommodityCodeUpdatePage, HasCommodityCodeChangePage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import utils.SessionData.{dataRemoved, dataUpdated, pageUpdated}
import views.html.goodsRecord.HasCommodityCodeChangeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasCommodityCodeChangedController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: GoodsRecordNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: HasCommodityCodeChangeFormProvider,
  auditService: AuditService,
  val controllerComponents: MessagesControllerComponents,
  view: HasCommodityCodeChangeView,
  goodsRecordConnector: GoodsRecordConnector
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val record = goodsRecordConnector.getRecord(recordId)
      record.map {
          case Some(goodsRecord) =>
            auditService.auditStartUpdateGoodsRecord(
              request.eori,
              request.affinityGroup,
              GoodsDetailsUpdate,
              recordId
            )
            val preparedForm = prepareForm(HasCommodityCodeChangePage(recordId), form)
            val needCategorisingWarning = goodsRecord.category.isDefined
            val needAdviceWarning = goodsRecord.adviceStatus == AdviceReceived
            Ok(view(preparedForm, mode, recordId, needAdviceWarning, needCategorisingWarning))
              .removingFromSession(dataRemoved, dataUpdated, pageUpdated)
          case None =>
            navigator.journeyRecovery()
        }.recover { _ =>
          navigator.journeyRecovery()
        }
    }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      goodsRecordConnector.getRecord(recordId).flatMap {
          case Some(goodsRecord) =>
            val needCategorisingWarning = goodsRecord.category.isDefined
            val needAdviceWarning = goodsRecord.adviceStatus == AdviceReceived
            val oldAnswer: String =
              request.userAnswers.get(CommodityCodeUpdatePage(recordId)).getOrElse(CommodityCodePage)
            form
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(view(formWithErrors, mode, recordId, needAdviceWarning, needCategorisingWarning))
                  ),
                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(HasCommodityCodeChangePage(recordId), value))
                    _ <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(navigator.nextPage(HasCommodityCodeChangePage(recordId), mode, updatedAnswers))
                    .addingToSession("oldAnswer" -> oldAnswer)
              )
          case None =>
            Future.successful(navigator.journeyRecovery())
        }.recover { _ =>
          navigator.journeyRecovery()
        }
    }
}
