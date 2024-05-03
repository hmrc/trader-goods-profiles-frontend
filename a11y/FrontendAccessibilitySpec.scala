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
  val layoutClasses                                     = Seq(classOf[Layout])
  val list: SummaryList                                 = SummaryListViewModel(rows = Seq.empty)
  implicit val arbSummaryList: Arbitrary[SummaryList]   = fixed(list)
  private val booleanForm: Form[Boolean]                = Form("value" -> boolean)
  implicit val arbForm: Arbitrary[Form[_]]              = fixed(booleanForm)
  implicit val arbBooleanForm: Arbitrary[Form[Boolean]] = fixed(booleanForm)

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
    case nirmsQuestionView: NirmsQuestionView                         => render(nirmsQuestionView)
    case ukimsNumberView: UkimsNumberView                             => render(ukimsNumberView)
    case niphlQuestionView: NiphlQuestionView                         => render(niphlQuestionView)
    case nirmsNumberView: NirmsNumberView                             => render(nirmsNumberView)
    case niphlNumberView: NiphlNumberView                             => render(niphlNumberView)
    case commodityCodeView: CommodityCodeView                         => render(commodityCodeView)
    case countryOfOriginView: CountryOfOriginView                     => render(countryOfOriginView)
  }
  runAccessibilityTests()
}
