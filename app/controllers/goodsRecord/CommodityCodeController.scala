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

import connectors.OttConnector
import controllers.BaseController
import controllers.actions._
import forms.goodsRecord.CommodityCodeFormProvider
import models.helper.{CreateRecordJourney, GoodsDetailsUpdate}
import models.requests.DataRequest
import models.{Commodity, Mode}
import navigation.GoodsRecordNavigator
import pages.goodsRecord._
import play.api.data.{Form, FormError}
import play.api.i18n.MessagesApi
import play.api.mvc._
import queries.{CommodityQuery, CommodityUpdateQuery}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.SessionData._
import views.html.goodsRecord.CommodityCodeView

import java.time.{LocalDate, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommodityCodeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: GoodsRecordNavigator,
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
      val preparedForm = prepareForm(CommodityCodePage, form)

      val onSubmitAction: Call = controllers.goodsRecord.routes.CommodityCodeController.onSubmitCreate(mode)

      Ok(view(preparedForm, onSubmitAction, mode, recordId = None))
    }

  def onPageLoadUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      val preparedForm = prepareForm(CommodityCodeUpdatePage(recordId), form)

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

      val onSubmitAction: Call = controllers.goodsRecord.routes.CommodityCodeController.onSubmitUpdate(mode, recordId)

      Ok(view(preparedForm, onSubmitAction, mode, Some(recordId))).removingFromSession(dataRemoved, dataUpdated, pageUpdated)
    }

  def onSubmitCreate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction          = controllers.goodsRecord.routes.CommodityCodeController.onSubmitCreate(mode)
      val countryOfOrigin: String = request.userAnswers.get(CountryOfOriginPage).get

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, onSubmitAction, mode, recordId = None))),
          value =>
            (for {
              commodity <- fetchCommodity(value, countryOfOrigin)
              result    <- validateAndProcessCommodityCreate(commodity, value, onSubmitAction, mode)
            } yield result).recover { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
              handleFormError(form, "commodityCode.error.invalid", onSubmitAction, mode, recordId = None)
            }
        )
    }

  def onSubmitUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction  = controllers.goodsRecord.routes.CommodityCodeController.onSubmitUpdate(mode, recordId)
      val countryOfOrigin = request.userAnswers.get(CountryOfOriginUpdatePage(recordId)).get
      val oldValueOpt     = request.userAnswers.get(CommodityCodeUpdatePage(recordId))

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, onSubmitAction, mode, Some(recordId)))),
          value => {
            val isValueChanged = oldValueOpt.exists(_ != value)
            (for {
              commodity <- fetchCommodity(value, countryOfOrigin)
              result    <- validateAndProcessCommodityUpdate(
                             commodity,
                             value,
                             recordId,
                             isValueChanged,
                             onSubmitAction,
                             mode
                           )
            } yield result).recover { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
              handleFormError(form, "commodityCode.error.invalid", onSubmitAction, mode, Some(recordId))
            }
          }
        )
    }

  private def fetchCommodity(
    value: String,
    countryOfOrigin: String
  )(implicit request: DataRequest[AnyContent]): Future[Commodity] =
    ottConnector.getCommodityCode(
      value,
      request.eori,
      request.affinityGroup,
      CreateRecordJourney,
      countryOfOrigin,
      None
    )

  private def handleFormError[T](form: Form[T], errorKey: String, onSubmitAction: Call, mode: Mode, recordId: Option[String])(implicit
    request: Request[AnyContent]
  ): Result = {
    val formWithApiErrors = form.copy(errors = Seq(FormError("value", getMessage(errorKey))))
    BadRequest(view(formWithApiErrors, onSubmitAction, mode, recordId))
  }

  private def validateAndProcessCommodityCreate(
    commodity: Commodity,
    value: String,
    onSubmitAction: Call,
    mode: Mode
  )(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val todayInstant = LocalDate.now(ZoneId.of("UTC")).atStartOfDay(ZoneId.of("UTC")).toInstant
    if (
      todayInstant.isBefore(commodity.validityStartDate) ||
      commodity.validityEndDate.exists(todayInstant.isAfter)
    ) {
      val formWithErrors = createFormWithErrors(form, value, "commodityCode.error.expired")
      Future.successful(BadRequest(view(formWithErrors, onSubmitAction, mode, recordId = None)))
    } else {
      for {
        updatedAnswers          <- Future.fromTry(request.userAnswers.set(CommodityCodePage, value))
        updatedAnswersWithQuery <-
          Future.fromTry(updatedAnswers.set(CommodityQuery, commodity.copy(commodityCode = value)))
        _                       <- sessionRepository.set(updatedAnswersWithQuery)
      } yield Redirect(navigator.nextPage(CommodityCodePage, mode, updatedAnswersWithQuery))
    }
  }

  private def validateAndProcessCommodityUpdate(
    commodity: Commodity,
    value: String,
    recordId: String,
    isValueChanged: Boolean,
    onSubmitAction: Call,
    mode: Mode
  )(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val todayInstant = LocalDate.now(ZoneId.of("UTC")).atStartOfDay(ZoneId.of("UTC")).toInstant
    if (
      todayInstant.isBefore(commodity.validityStartDate) ||
      commodity.validityEndDate.exists(todayInstant.isAfter)
    ) {
      val formWithErrors = createFormWithErrors(form, value, "commodityCode.error.expired")
      Future.successful(BadRequest(view(formWithErrors, onSubmitAction, mode, Some(recordId))))
    } else {
      for {
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
  }

}
