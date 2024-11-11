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

package controllers.categorisation

import connectors.{GoodsRecordConnector, OttConnector}
import controllers.BaseController
import controllers.actions._
import forms.categorisation.LongerCommodityCodeFormProvider
import models.helper.UpdateRecordJourney
import models.requests.DataRequest
import models.{Mode, UserAnswers}
import navigation.CategorisationNavigator
import pages.LongerCommodityCodePage
import play.api.data.FormError
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{CategorisationDetailsQuery, LongerCommodityQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.Constants.minimumLengthOfCommodityCode
import views.html.categorisation.LongerCommodityCodeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LongerCommodityCodeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: CategorisationNavigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: LongerCommodityCodeFormProvider,
  ottConnector: OttConnector,
  val controllerComponents: MessagesControllerComponents,
  view: LongerCommodityCodeView,
  goodsRecordConnector: GoodsRecordConnector
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val form = formProvider()

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val shortComcodeOpt = getShortCommodityCodeOpt(recordId, request.userAnswers)
      val preparedForm    = prepareForm(LongerCommodityCodePage(recordId), form)
      shortComcodeOpt match {
        case Some(shortComcode) if shortComcode.length == minimumLengthOfCommodityCode =>
          Ok(view(preparedForm, mode, shortComcode, recordId))
        case _                                                                         =>
          navigator.journeyRecovery()
      }
  }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val shortComcodeOpt = getShortCommodityCodeOpt(recordId, request.userAnswers)

      shortComcodeOpt match {
        case Some(shortComcode) if shortComcode.length == minimumLengthOfCommodityCode =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, shortComcode, recordId))),
              value =>
                validateAndUpdateAnswer(
                  mode,
                  recordId,
                  value,
                  shortComcode
                )
            )
        case _                                                                         =>
          Future.successful(navigator.journeyRecovery())
      }
    }

  private def getShortCommodityCodeOpt(
    recordId: String,
    userAnswers: UserAnswers
  ): Option[String] =
    userAnswers
      .get(CategorisationDetailsQuery(recordId))
      .map(_.getMinimalCommodityCode)

  private def validateAndUpdateAnswer(
    mode: Mode,
    recordId: String,
    value: String,
    shortCode: String
  )(implicit request: DataRequest[AnyContent]) = {
    val longerCode = shortCode + value
    (for {
      record                  <- goodsRecordConnector.getRecord(request.eori, recordId)
      commodity               <- ottConnector.getCommodityCode(
                                   longerCode,
                                   request.eori,
                                   request.affinityGroup,
                                   UpdateRecordJourney,
                                   record.countryOfOrigin,
                                   Some(recordId)
                                 )
      updatedAnswers          <- Future.fromTry(request.userAnswers.set(LongerCommodityCodePage(recordId), value))
      updatedAnswersWithQuery <-
        Future.fromTry(updatedAnswers.set(LongerCommodityQuery(recordId), commodity.copy(commodityCode = longerCode)))
      _                       <- sessionRepository.set(updatedAnswersWithQuery)
    } yield Redirect(
      navigator.nextPage(
        LongerCommodityCodePage(recordId),
        mode,
        updatedAnswersWithQuery
      )
    )).recover { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
      val formWithApiErrors =
        form.copy(errors = Seq(FormError("value", getMessage("longerCommodityCode.error.invalid"))))
      BadRequest(view(formWithApiErrors, mode, shortCode, recordId))
    }
  }

}
