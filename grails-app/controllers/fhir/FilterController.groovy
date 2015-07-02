package fhir

class FilterController {

  static scope = "singleton"
  FilterService filterService

  def patientList() {
    def group = request.reader.text.decodeFhirJson()
    render filterService.patientList(group)
  }

  def counts() {
    def group = request.reader.text.decodeFhirJson()
    render filterService.counts(group)
  }

}
