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

import connectors.OttConnector
import controllers.actions._
import forms.CommodityCodeFormProvider
import models.Mode
import models.helper.{CreateRecordJourney, GoodsDetailsUpdate}
import navigation.Navigator
import pages.{CommodityCodePage, CommodityCodeUpdatePage, CountryOfOriginPage, CountryOfOriginUpdatePage, HasCommodityCodeChangePage}
import play.api.data.FormError
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import queries.{CommodityQuery, CommodityUpdateQuery}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.SessionData._
import views.html.CommodityCodeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommodityCodeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: CommodityCodeFormProvider,
  ottConnector: OttConnector,
  val controllerComponents: MessagesControllerComponents,
  view: CommodityCodeView,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(CommodityCodePage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      val onSubmitAction: Call = routes.CommodityCodeController.onSubmitCreate(mode)

      Ok(view(preparedForm, onSubmitAction))
    }

  def onPageLoadUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(CommodityCodeUpdatePage(recordId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      request.userAnswers.get(HasCommodityCodeChangePage(recordId)) match {
        case None =>
          auditService
            .auditStartUpdateGoodsRecord(
              request.eori,
              request.affinityGroup,
              GoodsDetailsUpdate,
              recordId
            )
        case _    =>
      }

      val onSubmitAction: Call = routes.CommodityCodeController.onSubmitUpdate(mode, recordId)

      Ok(view(preparedForm, onSubmitAction)).removingFromSession(dataRemoved, dataUpdated, pageUpdated)
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction: Call    = routes.CommodityCodeController.onSubmitCreate(mode)
      val countryOfOrigin: String = request.userAnswers.get(CountryOfOriginPage).get
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, onSubmitAction))),
          value =>
            (for {
              commodity               <-
                ottConnector.getCommodityCode(
                  value,
                  request.eori,
                  request.affinityGroup,
                  CreateRecordJourney,
                  countryOfOrigin,
                  None
                )
              updatedAnswers          <- Future.fromTry(request.userAnswers.set(CommodityCodePage, value))
              updatedAnswersWithQuery <-
                Future.fromTry(updatedAnswers.set(CommodityQuery, commodity.copy(commodityCode = value)))
              _                       <- sessionRepository.set(updatedAnswersWithQuery)
            } yield Redirect(navigator.nextPage(CommodityCodePage, mode, updatedAnswersWithQuery))).recover {
              case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
                val formWithApiErrors =
                  form.copy(errors = Seq(elems = FormError("value", getMessage("commodityCode.error.invalid"))))
                BadRequest(view(formWithApiErrors, onSubmitAction))
            }
        )
    }

  def onSubmitUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction: Call    = routes.CommodityCodeController.onSubmitUpdate(mode, recordId)
      val countryOfOrigin: String = request.userAnswers.get(CountryOfOriginUpdatePage(recordId)).get
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, onSubmitAction))),
          value =>
            {
              val oldValueOpt    = request.userAnswers.get(CommodityCodeUpdatePage(recordId))
              val isValueChanged = oldValueOpt.exists(_ != value)
              for {
                commodity               <-
                  ottConnector.getCommodityCode(
                    value,
                    request.eori,
                    request.affinityGroup,
                    CreateRecordJourney,
                    countryOfOrigin,
                    None
                  )
                updatedAnswers          <- Future.fromTry(request.userAnswers.set(CommodityCodeUpdatePage(recordId), value))
                updatedAnswersWithQuery <-
                  Future.fromTry(
                    updatedAnswers.set(CommodityUpdateQuery(recordId), commodity.copy(commodityCode = value))
                  )
                _                       <- sessionRepository.set(updatedAnswersWithQuery)
              } yield Redirect(navigator.nextPage(CommodityCodeUpdatePage(recordId), mode, updatedAnswersWithQuery))
                .addingToSession(dataUpdated -> isValueChanged.toString)
                .addingToSession(pageUpdated -> commodityCode)

            }
              .recover { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
                val formWithApiErrors =
                  form.copy(errors = Seq(elems = FormError("value", getMessage("commodityCode.error.invalid"))))
                BadRequest(view(formWithApiErrors, onSubmitAction))
              }
        )
    }

  private def getMessage(key: String)(implicit messages: Messages): String = messages(key)
}
