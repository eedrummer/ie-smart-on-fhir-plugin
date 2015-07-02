package fhir

import groovy.sql.Sql

class WhereClause {
  String searchParam
  String column
  String comparator
  String value
  String system
}

class QueryWithParams {
  StringBuilder query = new StringBuilder()
  def params = []

  def appendSQL(sql) {
    this.query.append(sql)
  }

  def appendParam(param) {
    this.params << param
  }
}

class FilterService {
  def dataSource

  def patientList(group) {
    def clauses = groupToWhereClauses(group)
    def query = generateBaseSQL("fhir_id", clauses)
    def sql = new Sql(dataSource)
    sql.rows(query.query.toString(), query.params).collect {i -> i.fhir_id}
  }

  def counts(group) {
    def clauses = groupToWhereClauses(group)
    def sql = new Sql(dataSource)
    def countQueries = ["patients": generateBaseSQL("count(*) as c", clauses),
                        "conditions": generateConditionCountSQL(clauses),
                        "encounters": generateEncounterCountSQL(clauses)]
    def allCounts = [:]
    countQueries.each {thing, query ->
      allCounts[thing] = sql.firstRow(query.query.toString(), query.params)["c"]
    }
    allCounts
  }

  def generatePatientAttributeCountSQL(attribute, clauses) {
    def inQuery = generateBaseSQL("fhir_id", clauses)
    def query = new QueryWithParams()
    query.appendSQL("select count(*) as c from (select fhir_id from resource_index_term ")
    query.appendSQL("where fhir_type = '$attribute' AND search_param = 'patient' AND reference_id IN (")
    query.appendSQL(inQuery.query.toString())
    query.params = inQuery.params
    query.appendSQL(") GROUP BY fhir_id) f")
    query
  }

  def generateConditionCountSQL(clauses) {
    generatePatientAttributeCountSQL("Condition", clauses)
  }

  def generateEncounterCountSQL(clauses) {
    generatePatientAttributeCountSQL("Encounter", clauses)
  }

  def generateBaseSQL(selectStatement, clauses) {
    def query = new QueryWithParams()
    query.appendSQL("select ")
    query.appendSQL(selectStatement)
    query.appendSQL(" from (")
    query.appendSQL(clauses["patient"].collect { pw ->
      query.appendParam(pw.value)
      if(pw.searchParam == "birthdate") {
        "select fhir_id from resource_index_term where fhir_type = 'Patient' AND search_param = '$pw.searchParam' AND $pw.column $pw.comparator ?::timestamp"
      } else {
        "select fhir_id from resource_index_term where fhir_type = 'Patient' AND search_param = '$pw.searchParam' AND $pw.column $pw.comparator ?"
      }

    }.join("\nINTERSECT\n"))
    query.appendSQL(") p")
    if (clauses["condition"].size() > 0) {
      query.appendSQL(" WHERE\n")
    }
    query.appendSQL(clauses["condition"].collect { qw ->
      query.appendParam(qw.system)
      query.appendParam(qw.value)
      """EXISTS(
          SELECT fhir_id
            from resource_index_term
            where fhir_type = 'Condition' AND search_param = '$qw.searchParam' AND token_namespace = ? AND $qw.column = ?
          INTERSECT

          SELECT fhir_id
            from resource_index_term
            where fhir_type = 'Condition' AND search_param = 'patient' AND reference_id = p.fhir_id)
    """
    }.join("\nAND\n"))
    query
  }

  def groupToWhereClauses(group) {
    def clauses = ["patient": [], "condition": []]
    group.characteristic.each { characteristic ->
      switch(characteristic.code.coding) {
        case {it.any { c -> c.system == "http://loinc.org" && c.code == "21840-4"}}: // Patient Gender
          def firstValueCode = characteristic.valueCodeableConcept.coding[0]
          clauses["patient"] << new WhereClause(searchParam: "gender", column: "token_code", comparator: "=", value: firstValueCode.code)
        break
        case {it.any { c -> c.system == "http://loinc.org" && c.code == "21612-7"}}: // Patient Age
          if (characteristic.valueRange) {
            if (characteristic.valueRange.high) {
              def ageInYears = characteristic.valueRange.high.value.toInteger();
              def d = new Date();
              d[Calendar.YEAR] = (d[Calendar.YEAR] - ageInYears)
              clauses["patient"] << new WhereClause(searchParam: "birthdate", column: "date_max", comparator: ">", value: d.format("YYYY-MM-dd"))
            }
            if (characteristic.valueRange.low) {
              def ageInYears = characteristic.valueRange.low.value.toInteger()
              def d = new Date()
              d[Calendar.YEAR] = d[Calendar.YEAR] - ageInYears
              clauses["patient"] << new WhereClause(searchParam: "birthdate", column: "date_max", comparator: "<", value: d.format("YYYY-MM-dd"))
            }
          }
        break
        case {it.any { c -> c.system == "http://loinc.org" && c.code == "11450-4"}}:
          clauses["condition"] << new WhereClause(searchParam: "code", column: "token_code", comparator: "=", value: characteristic.valueCodeableConcept.coding[0].code,
                                            system: characteristic.valueCodeableConcept.coding[0].system)
        break
      }
    }
    clauses
  }
}