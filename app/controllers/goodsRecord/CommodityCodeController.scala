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
import controllers.actions._
import controllers.{BaseController, routes}
import forms.CommodityCodeFormProvider
import models.helper.{CreateRecordJourney, GoodsDetailsUpdate}
import models.requests.DataRequest
import models.{Commodity, Mode}
import navigation.Navigator
import pages._
import play.api.data.{Form, FormError}
import play.api.i18n.MessagesApi
import play.api.mvc._
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
      val preparedForm = prepareForm(CommodityCodePage, form)

      val onSubmitAction: Call = routes.CommodityCodeController.onSubmitCreate(mode)

      Ok(view(preparedForm, onSubmitAction))
    }

  def onPageLoadUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
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
              commodity               <- fetchCommodity(value, countryOfOrigin)
              updatedAnswers          <- Future.fromTry(request.userAnswers.set(CommodityCodePage, value))
              updatedAnswersWithQuery <-
                Future.fromTry(updatedAnswers.set(CommodityQuery, commodity.copy(commodityCode = value)))
              _                       <- sessionRepository.set(updatedAnswersWithQuery)
            } yield Redirect(navigator.nextPage(CommodityCodePage, mode, updatedAnswersWithQuery))).recover {
              case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
                handleFormError(form, "commodityCode.error.invalid", onSubmitAction)
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
                commodity               <- fetchCommodity(value, countryOfOrigin)
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
                handleFormError(form, "commodityCode.error.invalid", onSubmitAction)
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
  private def handleFormError[T](form: Form[T], errorKey: String, onSubmitAction: Call)(implicit
    request: Request[AnyContent]
  ): Result = {
    val formWithApiErrors = form.copy(errors = Seq(FormError("value", getMessage(errorKey))))
    BadRequest(view(formWithApiErrors, onSubmitAction))
  }

}
