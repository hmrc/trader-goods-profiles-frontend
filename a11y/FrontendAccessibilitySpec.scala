import config.FrontendAppConfig
import org.scalacheck.Arbitrary
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.scalatestaccessibilitylinter.views.AutomaticAccessibilitySpec
import views.html._
import views.html.templates.Layout
import viewmodels.govuk.summarylist._
class FrontendAccessibilitySpec extends AutomaticAccessibilitySpec {

  // If you wish to override the GuiceApplicationBuilder to provide additional
  // config for your service, you can do that by overriding fakeApplication
  // example
//  override def fakeApplication(): Application =
//    new GuiceApplicationBuilder()
//      .configure()
//      .build()

  // Some view template parameters can't be completely arbitrary,
  // but need to have sane values for pages to render properly.
  // eg. if there is validation or conditional logic in the twirl template.
  // These can be provided by calling `fixed()` to wrap an existing concrete value.
  // example
  val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  //implicit val arbConfig: Arbitrary[FrontendAppConfig] = fixed(appConfig)

  // Another limitation of the framework is that it can generate Arbitrary[T] but not Arbitrary[T[_]],
  // so any nested types (like a Play `Form[]`) must similarly be provided by wrapping
  // a concrete value using `fixed()`.  Usually, you'll have a value you can use somewhere else
  // in your codebase - either in your production code or another test.
  // Note - these values are declared as `implicit` to simplify calls to `render()` below
  // e.g implicit val arbReportProblemPage: Arbitrary[Form[ReportProblemForm]] = fixed(reportProblemForm)

  // This is the package where the page templates are located in your service
  val viewPackageName = "views/html"

  // This is the layout class or classes which are injected into all full pages in your service.
  // This might be `HmrcLayout` or some custom class(es) that your service uses as base page templates.
  val layoutClasses = Seq(classOf[Layout])

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
  }

  runAccessibilityTests()
}
