package core.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.aventstack.extentreports.reporter.configuration.ChartLocation;
import com.aventstack.extentreports.reporter.configuration.Theme;

public class ExtentManager {

    private static ExtentReports extent;


    public synchronized static ExtentReports getInstance() {
        if (extent == null)
            createInstance("extent.html");
        return extent;
    }


    public synchronized static ExtentReports createInstance(String fileName) {
        ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter(fileName);
        htmlReporter.config().setTestViewChartLocation(ChartLocation.BOTTOM);
        htmlReporter.config().setChartVisibilityOnOpen(true);
        htmlReporter.config().setDocumentTitle("Unbxd-selfServe-UI-ExtentReports");
        htmlReporter.config().setReportName("UNBXD selfServe UI Automation Report");
        htmlReporter.config().setTestViewChartLocation(ChartLocation.BOTTOM);
        htmlReporter.config().setTheme(Theme.DARK);
        htmlReporter.config().setEncoding("utf-8");
        htmlReporter.config().setCSS("img { max-width: 100%; height: auto; }");
        htmlReporter.config().setJS(
            "var panels = document.querySelectorAll('.panel-lead');" +
            "panels.forEach(function(el) {" +
            "  var text = el.textContent.trim();" +
            "  if (text.match(/^[\\d,]+ms$/)) {" +
            "    var ms = parseInt(text.replace(/[^0-9]/g, ''));" +
            "    if (!isNaN(ms)) {" +
            "      var totalSec = Math.floor(ms / 1000);" +
            "      var h = Math.floor(totalSec / 3600);" +
            "      var m = Math.floor((totalSec % 3600) / 60);" +
            "      var s = totalSec % 60;" +
            "      var fmt = '';" +
            "      if (h > 0) fmt += h + 'h ';" +
            "      fmt += m + 'm ' + s + 's';" +
            "      el.textContent = fmt;" +
            "    }" +
            "  }" +
            "});"
        );

        extent = new ExtentReports();
        extent.attachReporter(htmlReporter);
        extent.setReportUsesManualConfiguration(true);
        return extent;
    }
}
