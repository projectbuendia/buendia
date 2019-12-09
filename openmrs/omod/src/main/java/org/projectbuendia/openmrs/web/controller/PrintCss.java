package org.projectbuendia.openmrs.web.controller;

public class PrintCss {
    public static final String CSS =
        "/* Global */\n" +
        "\n" +
        "html {\n" +
        "  font-size: 15px;\n" +
        "}\n" +
        "\n" +
        "body {\n" +
        "  margin: 0;\n" +
        "}\n" +
        "\n" +
        ".columns tr {\n" +
        "  vertical-align: top;\n" +
        "}\n" +
        "\n" +
        "/* Admission */\n" +
        "\n" +
        ".admission-form {\n" +
        "  font-family: verdana;\n" +
        "  font-size: 0.6rem;\n" +
        "  margin: 0;\n" +
        "  page-break-after: always;\n" +
        "}\n" +
        "\n" +
        ".admission-form td {\n" +
        "  font-size: 0.6rem;\n" +
        "}\n" +
        "\n" +
        ".line {\n" +
        "  padding-top: 0.2rem;\n" +
        "}\n" +
        "\n" +
        ".followup .row:first-of-type {\n" +
        "  padding-bottom: 0.2rem;\n" +
        "}\n" +
        "\n" +
        ".line, .subhead, .vspace {\n" +
        "  min-height: 1rem;\n" +
        "  white-space: nowrap;\n" +
        "}\n" +
        "\n" +
        ".admission-form .title .heading {\n" +
        "  padding-left: 0.5rem;\n" +
        "  font-weight: bold;\n" +
        "  font-size: 0.8rem;\n" +
        "}\n" +
        "\n" +
        ".admission-form .title .heading, .admission-form .title .patient-id {\n" +
        "  margin-bottom: 0.5rem;\n" +
        "}\n" +
        "\n" +
        ".admission-form .patient-id {\n" +
        "  float: right;\n" +
        "}\n" +
        "\n" +
        ".admission-form .section {\n" +
        "  clear: both;\n" +
        "}\n" +
        "\n" +
        ".admission-form .section .heading {\n" +
        "  display: block;\n" +
        "  font-size: 0.8rem;\n" +
        "  font-weight: normal;\n" +
        "  text-align: center;\n" +
        "  padding: 0.4rem 0.6rem;\n" +
        "  background: #666;\n" +
        "  color: white;\n" +
        "}\n" +
        "\n" +
        "td .block {\n" +
        "  border-right: 0.1rem solid black;\n" +
        "}\n" +
        "\n" +
        ".block {\n" +
        "  border-top: 0.1rem solid black;\n" +
        "  padding: 0.5rem;\n" +
        "}\n" +
        "\n" +
        "td:last-of-type .block {\n" +
        "  border-right: none;\n" +
        "}\n" +
        "\n" +
        ".blank {\n" +
        "  margin: 0 0.2rem 0 0;\n" +
        "  padding: 0.04rem;\n" +
        "  border-bottom: 0.1rem solid black;\n" +
        "  display: inline-block;\n" +
        "}\n" +
        "\n" +
        ".spaces {\n" +
        "  display: block;\n" +
        "}\n" +
        "\n" +
        ".blank .contents {\n" +
        "  display: block;\n" +
        "  font-family: arial;\n" +
        "  font-size: 0.8rem;\n" +
        "  text-align: center;\n" +
        "  margin: -0.1rem 0;\n" +
        "}\n" +
        "\n" +
        ".space {\n" +
        "  width: 1.5rem;\n" +
        "  display: inline-block;\n" +
        "}\n" +
        "\n" +
        "table.columns {\n" +
        "  width: 100%;\n" +
        "}\n" +
        "\n" +
        ".field .label {\n" +
        "  margin-left: 1rem;\n" +
        "  margin-right: 0.2rem;\n" +
        "}\n" +
        "\n" +
        ".field:first-of-type .label {\n" +
        "  margin-left: 0;\n" +
        "}\n" +
        "\n" +
        ".section {\n" +
        "  border-left: 1px solid black;\n" +
        "  border-right: 1px solid black;\n" +
        "  display: block;\n" +
        "}\n" +
        "\n" +
        ".section:last-of-type {\n" +
        "  border-bottom: 1px solid black;\n" +
        "}\n" +
        "\n" +
        ".ox:first-of-type {\n" +
        "  margin-left: 0;\n" +
        "}\n" +
        "\n" +
        ".checkitem:first-of-type{\n" +
        "  margin-left: 0.2rem;\n" +
        "}\n" +
        "\n" +
        ".checkitem {\n" +
        "  margin-left: 0.6rem;\n" +
        "}\n" +
        "\n" +
        ".checkitem input {\n" +
        "  height: 0.8rem;\n" +
        "  margin: 0 0.2rem 0.2rem 0; \n" +
        "}\n" +
        "\n" +
        ".checkitem .label {\n" +
        "  margin-left: 0.4rem;\n" +
        "}\n" +
        "\n" +
        ".stack {\n" +
        "  display: inline-block;\n" +
        "  vertical-align: top;\n" +
        "}\n" +
        "\n" +
        ".stack .row {\n" +
        "  display: block;\n" +
        "}\n" +
        "\n" +
        ".subhead {\n" +
        "  font-weight: bold;\n" +
        "}\n" +
        "\n" +
        ".agesex .hspace .unit {\n" +
        "  width: 0.5rem;\n" +
        "}\n" +
        "\n" +
        ".residence .hspace .unit {\n" +
        "  width: 3rem;\n" +
        "}\n" +
        "\n" +
        ".pregnancy .subhead {\n" +
        "  font-style: italic;\n" +
        "}\n" +
        "\n" +
        ".pregnancy + .columns td:first-of-type {  \n" +
        "  width: 60%;\n" +
        "}\n" +
        "\n" +
        ".comorbidities td {\n" +
        "  width: 50% !important;\n" +
        "}\n" +
        "\n" +
        ".comorbidities {\n" +
        "  padding: 0;\n" +
        "}\n" +
        "\n" +
        ".comorbidities .subhead {\n" +
        "  background: #ddd;\n" +
        "  padding: 0.3rem 0.5rem 0;\n" +
        "}\n" +
        "\n" +
        ".comorbidities .subhead td {\n" +
        "  font-weight: bold;\n" +
        "}\n" +
        "\n" +
        ".comorbidities .line {\n" +
        "  padding: 0.2rem 0.5rem 0;\n" +
        "  border-top: 0.1rem solid #999;\n" +
        "}\n" +
        "\n" +
        ".followup .stack {\n" +
        "  margin-left: 0.3rem;\n" +
        "}\n" +
        "\n" +
        "\n" +
        "/* History */\n" +
        "\n" +
        ".history {\n" +
        "  font-family: helvetica, arial, sans-serif;\n" +
        "  margin: 1rem;\n" +
        "  page-break-after: always;\n" +
        "}\n" +
        "\n" +
        ".history .heading {\n" +
        "  margin: 2rem 0 1rem;\n" +
        "  font-size: 1.6rem;\n" +
        "  font-weight: bold;\n" +
        "}\n" +
        "\n" +
        ".history .obs,\n" +
        ".history .treatment > div,\n" +
        ".history .schedule,\n" +
        ".history .execution > div {\n" +
        "  padding-left: 2em;\n" +
        "  text-indent: -2em;\n" +
        "}\n" +
        "\n" +
        ".intro {\n" +
        "  font-size: 2rem;\n" +
        "}\n" +
        "\n" +
        ".intro .ident {\n" +
        "  float: right;\n" +
        "  font-size: 3rem;\n" +
        "}\n" +
        "\n" +
        ".intro .name {\n" +
        "  font-weight: bold;\n" +
        "}\n" +
        "\n" +
        ".history, .history td {\n" +
        "  font-size: 0.9rem;\n" +
        "  font-weight: 400;\n" +
        "  line-height: 1.4;\n" +
        "}\n" +
        "\n" +
        ".event {\n" +
        "  margin: 2em 0;\n" +
        "  clear: both;\n" +
        "}\n" +
        "\n" +
        ".history .time {\n" +
        "  font-weight: bold;\n" +
        "  font-size: 1.4rem;\n" +
        "  margin: 0 1rem;\n" +
        "  padding-top: 0.7rem;\n" +
        "  padding-bottom: 0.7rem;\n" +
        "}\n" +
        "\n" +
        ".event .time:first-of-type {\n" +
        "  padding-top: 0;\n" +
        "}\n" +
        "\n" +
        ".c-159393, .c-1642 {  /* diagnosis, final diagnosis */\n" +
        "  font-weight: bold;\n" +
        "}\n" +
        "\n" +
        ".label {\n" +
        "  color: #699;\n" +
        "}\n" +
        "\n" +
        ".observations .form {\n" +
        "  margin: 1em;\n" +
        "}\n" +
        "\n" +
        ".orders h3, .executions h3 {\n" +
        "  font-weight: bold;\n" +
        "  font-size: 0.9rem;\n" +
        "  color: #000;\n" +
        "}\n" +
        "\n" +
        ".order, .execution {\n" +
        "  margin: 1rem 0;\n" +
        "}\n" +
        "\n" +
        ".order > .label, .order .action, .execution > .label {\n" +
        "  display: none;\n" +
        "}\n" +
        "\n" +
        ".drug, .format {\n" +
        "  display: block;\n" +
        "}\n" +
        "\n";
}
