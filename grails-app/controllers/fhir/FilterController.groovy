package fhir

class FilterController {

  static scope = "singleton"
  FilterService filterService

  def patientList() {
    def group = request.reader.text.decodeFhirJson()
    render filterService.patientList(group)
  }

  def savedPatientList() {
    def group = applicationContext.getBean("sqlService").getLatestByFhirId("Group", params.id)
    render filterService.patientList(group.content.decodeFhirJson())
  }

  def counts() {
    def group = request.reader.text.decodeFhirJson()
    render filterService.counts(group)
  }

  def savedCounts() {
    def group = applicationContext.getBean("sqlService").getLatestByFhirId("Group", params.id)
    render filterService.counts(group.content.decodeFhirJson())
  }


}
