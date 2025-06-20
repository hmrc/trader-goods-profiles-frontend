/*
 * Copyright 2025 HM Revenue & Customs
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
import queries.CommodityUpdateQuery
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.SessionData._
import views.html.goodsRecord.CommodityCodeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateCommodityCodeController @Inject() (
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

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val preparedForm = prepareForm(CommodityCodeUpdatePage(recordId), form)

      // Audit only if the HasCommodityCodeChangePage is not set
      if (request.userAnswers.get(HasCommodityCodeChangePage(recordId)).isEmpty) {
        auditService.auditStartUpdateGoodsRecord(
          request.eori,
          request.affinityGroup,
          GoodsDetailsUpdate,
          recordId
        )
      }

      // Remove banner keys on page load to avoid stale banner showing when no change happens
      sessionRepository
        .set(request.userAnswers)
        .map { _ =>
          Ok(
            view(
              preparedForm,
              controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController.onSubmit(mode, recordId),
              mode,
              Some(recordId)
            )
          ).removingFromSession(dataRemoved, dataUpdated, pageUpdated)
        }
    }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val onSubmitAction =
        controllers.goodsRecord.commodityCode.routes.UpdateCommodityCodeController.onSubmit(mode, recordId)

      val countryOfOrigin = request.userAnswers.get(CountryOfOriginUpdatePage(recordId)).get
      val oldValueOpt     = request.userAnswers.get(CommodityCodeUpdatePage(recordId))

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, onSubmitAction, mode, Some(recordId)))),
          value =>
            fetchCommodity(value, countryOfOrigin)
              .flatMap {
                case commodity if commodity.isValid =>
                  val oldValue   = oldValueOpt.getOrElse("").trim.toUpperCase
                  val newValue   = value.trim.toUpperCase
                  val hasChanged = newValue.nonEmpty && newValue != oldValue

                  for {
                    updatedAnswers          <- Future.fromTry(request.userAnswers.set(CommodityCodeUpdatePage(recordId), value))
                    updatedAnswersWithQuery <-
                      Future.fromTry(
                        updatedAnswers.set(CommodityUpdateQuery(recordId), commodity.copy(commodityCode = value))
                      )
                    _                       <- sessionRepository.set(updatedAnswersWithQuery)
                  } yield {
                    val redirect =
                      Redirect(navigator.nextPage(CommodityCodeUpdatePage(recordId), mode, updatedAnswersWithQuery))

                    if (hasChanged) {
                      redirect.addingToSession(
                        "commodityCodeChanged" -> "true",
                        dataUpdated            -> "true",
                        pageUpdated            -> "commodityCode"
                      )
                    } else {
                      redirect.removingFromSession(dataUpdated, pageUpdated)
                    }
                  }

                case _ =>
                  val formWithErrors = createFormWithErrors(form, value, "commodityCode.error.expired")
                  Future.successful(BadRequest(view(formWithErrors, onSubmitAction, mode, Some(recordId))))
              }
              .recover { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
                handleFormError(form, "commodityCode.error.invalid", onSubmitAction, mode, Some(recordId))
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

  private def handleFormError[T](
    form: Form[T],
    errorKey: String,
    onSubmitAction: Call,
    mode: Mode,
    recordId: Option[String]
  )(implicit request: Request[AnyContent]): Result = {
    val formWithApiErrors = form.copy(errors = Seq(FormError("value", getMessage(errorKey))))
    BadRequest(view(formWithApiErrors, onSubmitAction, mode, recordId))
  }
}
