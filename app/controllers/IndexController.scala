package controllers

import connectors.OttConnector
import controllers.actions.IdentifierAction

import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.IndexView

class IndexController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  view: IndexView,
  ottConnector: OttConnector
) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = identify { implicit request =>
    ottConnector.getGoodsNomenclatures("8471300000") // For purposes of development
    Ok(view())
  }
}
