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

    name savedPatientList: "/Group/$id/patientList" {
      controller="Filter"
      action=[GET: "savedPatientList"]
    }

    name savedCounts: "/Group/$id/counts" {
      controller="Filter"
      action=[GET: "savedCounts"]
    }
  }
}
