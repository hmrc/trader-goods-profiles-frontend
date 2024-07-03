package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form

class HasGoodDescriptionChangeFormProvider @Inject() extends Mappings {

  def apply(): Form[Boolean] =
    Form(
      "value" -> boolean("hasGoodDescriptionChange.error.required")
    )
}
