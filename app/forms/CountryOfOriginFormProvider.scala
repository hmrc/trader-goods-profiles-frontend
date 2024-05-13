package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form

class CountryOfOriginFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("countryOfOrigin.error.required")
        .verifying(maxLength(100, "countryOfOrigin.error.length"))
    )
}
