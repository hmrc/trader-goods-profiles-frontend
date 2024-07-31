package controllers

import connectors.GoodsRecordConnector
import controllers.actions._

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ReviewReasonView

import scala.concurrent.ExecutionContext

class ReviewReasonController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: ReviewReasonView,
                                       goodsRecordConnector: GoodsRecordConnector
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request => {
      goodsRecordConnector.getRecord(request.eori, recordId).map { getGoodsRecordResponse =>
        if (getGoodsRecordResponse. toReview) {
          getGoodsRecordResponse.reviewReason match {
            case Some(reason) => Ok(view(recordId, reason))
            case _ => Redirect(routes.JourneyRecoveryController.onPageLoad ().url)
          }
        } else {
          Redirect(routes.SingleRecordController.onPageLoad (recordId).url)
        }
      }
    }
  }
}
