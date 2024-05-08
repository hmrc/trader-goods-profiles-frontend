import models.{Mode, NormalMode}
import org.scalacheck.Arbitrary
import play.api.data.Forms._
import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.scalatestaccessibilitylinter.views.AutomaticAccessibilitySpec
import views.html._
import views.html.templates.Layout
import viewmodels.govuk.summarylist._
import views.html.auth.SignedOutView
class FrontendAccessibilitySpec extends AutomaticAccessibilitySpec {

  val viewPackageName                                   = "views"
  val layoutClasses: Seq[Class[Layout]]                 = Seq(classOf[Layout])
  val list: SummaryList                                 = SummaryListViewModel(rows = Seq.empty)
  implicit val arbSummaryList: Arbitrary[SummaryList]   = fixed(list)
  val mode: Mode                                        = NormalMode
  implicit val arbMode: Arbitrary[Mode]                 = fixed(mode)
  private val booleanForm: Form[Boolean]                = Form("value" -> boolean)
  implicit val arbForm: Arbitrary[Form[_]]              = fixed(booleanForm)
  implicit val arbBooleanForm: Arbitrary[Form[Boolean]] = fixed(booleanForm)

  override def renderViewByClass: PartialFunction[Any, Html] = {
    case categoryGuidanceView: CategoryGuidanceView                   => render(categoryGuidanceView)
    case errorTemplate: ErrorTemplate                                 => render(errorTemplate)
    case journeyRecoveryStartAgainView: JourneyRecoveryStartAgainView => render(journeyRecoveryStartAgainView)
    case journeyRecoveryContinueView: JourneyRecoveryContinueView     => render(journeyRecoveryContinueView)
    case unauthorisedView: UnauthorisedView                           => render(unauthorisedView)
    case signedOutView: SignedOutView                                 => render(signedOutView)
    case profileSetupView: ProfileSetupView                           => render(profileSetupView)
    case ukimsNumberView: UkimsNumberView                             => render(ukimsNumberView)
    case nirmsQuestionView: NirmsQuestionView                         => render(nirmsQuestionView)
    case nirmsNumberView: NirmsNumberView                             => render(nirmsNumberView)
    case niphlQuestionView: NiphlQuestionView                         => render(niphlQuestionView)
    case niphlNumberView: NiphlNumberView                             => render(niphlNumberView)
    case checkYourAnswersView: CheckYourAnswersView                   => render(checkYourAnswersView)
    case homepageView: HomepageView                                   => render(homepageView)
    case dummyView: DummyView                                         => render(dummyView)
  }
  runAccessibilityTests()
}
