class InterventionEngineUrlMappings {

  static mappings = {
    name patientList: "/Group/patientList" {
      controller="Filter"
      action=[POST: "patientList"]
    }

    name counts: "/Group/counts" {
      controller="Filter"
      action=[POST: "counts"]
    }
  }
}
