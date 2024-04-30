import config.FrontendAppConfig
import org.scalacheck.Arbitrary
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.scalatestaccessibilitylinter.views.AutomaticAccessibilitySpec
import views.html._
import views.html.templates.Layout
import viewmodels.govuk.summarylist._
import views.html.auth.SignedOutView
class FrontendAccessibilitySpec extends AutomaticAccessibilitySpec {

  val viewPackageName                                 = "views"
  val layoutClasses                                   = Seq(classOf[Layout])
  val appConfig: FrontendAppConfig                    = app.injector.instanceOf[FrontendAppConfig]
  //implicit val arbConfig: Arbitrary[FrontendAppConfig] = fixed(appConfig)
  val list: SummaryList                               = SummaryListViewModel(rows = Seq.empty)
  implicit val arbSummaryList: Arbitrary[SummaryList] = fixed(list)

  override def renderViewByClass: PartialFunction[Any, Html] = {
    case profileSetupView: ProfileSetupView                           => render(profileSetupView)
    case categoryGuidanceView: CategoryGuidanceView                   => render(categoryGuidanceView)
    case checkYourAnswersView: CheckYourAnswersView                   => render(checkYourAnswersView)
    case errorTemplate: ErrorTemplate                                 => render(errorTemplate)
    case journeyRecoveryStartAgainView: JourneyRecoveryStartAgainView => render(journeyRecoveryStartAgainView)
    case journeyRecoveryContinueView: JourneyRecoveryContinueView     => render(journeyRecoveryContinueView)
    case unauthorisedView: UnauthorisedView                           => render(unauthorisedView)
    case dummyView: DummyView                                         => render(dummyView)
    case signedOutView: SignedOutView                                 => render(signedOutView)
  }
  runAccessibilityTests()
}
