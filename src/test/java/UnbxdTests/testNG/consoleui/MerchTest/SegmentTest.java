package UnbxdTests.testNG.consoleui.MerchTest;

import UnbxdTests.testNG.dataProvider.ResourceLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.consoleui.actions.BannerActions;
import core.consoleui.actions.CommercePageActions;
import core.consoleui.actions.MerchandisingActions;
import core.consoleui.actions.SegmentActions;
import core.consoleui.page.BrowsePage;
import core.consoleui.page.BrowseSegmentPage;
import core.consoleui.page.SegmentPage;
import lib.annotation.FileToTest;
import lib.enums.UnbxdEnum;
import lib.compat.Page;
import lib.compat.FluentWebElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static core.ui.page.UiBase.ThreadWait;

public class SegmentTest extends MerchandisingTest {

    List<String> SegmentRules = new ArrayList<>();

    @Page
    MerchandisingActions merchandisingActions;

    @Page
    CommercePageActions searchPageActions;
    @Page
    SegmentPage segmentPage;

    @Page
    BrowseSegmentPage browseSegmentPage;

    @Page
    SegmentActions segmenPageActions;
    @Page
    BannerActions bannerActions;

    @Page
    BrowsePage browsePage;

    public String segmentName;

    List<String> queryRules = new ArrayList<>();
    List<String> pageRules = new ArrayList<>();

    public String page;
    public String query;


    @FileToTest(value = "/consoleTestData/Segment.json")
    @Test(description = "Verifies the creation and editing of a search segment.", priority = 1, dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile",groups = {"sanity"})
    public void createAndEditSearchSegmentTest(Object jsonObject) throws InterruptedException {
        JsonObject segmentData=(JsonObject) jsonObject;
        segmentName=segmentData.get("Segment").getAsString();
        String userTypeNew = segmentData.get("UserTypeNew").getAsString();
        String segmentDeviceType = segmentData.get("SegmentDeviceType").getAsString();
        String userTypeRepeat = segmentData.get("UserTypeRepeat").getAsString();

        ThreadWait();
        searchPageActions.awaitForPageToLoad();
        goTo(segmentPage);
        ThreadWait();

        segmenPageActions.createSegment(segmentName);
        segmenPageActions.selectTypeValues(UnbxdEnum.USERTYPE,userTypeNew);
        segmenPageActions.selectDeviceType(UnbxdEnum.DEVICETYPE,segmentDeviceType);
        segmenPageActions.clickOnSave();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        Assert.assertNotNull(segmenPageActions.segmentRuleByName(segmentName));
        SegmentRules.add(segmentName);

        searchPageActions.selectActionType(UnbxdEnum.EDIT,segmentName);
        ThreadWait();
        segmenPageActions.segmentUserTypeValues.getText().contains(userTypeNew);
        segmenPageActions.segmentDeviceTypeValues.getText().contains(segmentDeviceType);

        segmenPageActions.removeSegmentValues(userTypeNew);
        segmenPageActions.selectTypeValues(UnbxdEnum.USERTYPE,userTypeRepeat);
        segmenPageActions.segmentUserTypeValues.getText().contains(userTypeRepeat);
        segmenPageActions.clickOnSave();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        segmenPageActions.UserTypeinListingPage.getText().contains(userTypeRepeat);

        goTo(segmentPage);
        segmenPageActions.deleteSegmentRule(segmentName);
        searchPage.awaitTillElementDisplayed(searchPageActions.ToasterSuccess);

    }
    @FileToTest(value = "/consoleTestData/BrowseSegment.json")
    @Test(description = "Verifies the creation and editing of a browse segment.", priority = 5, dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile",groups = {"sanity"})
    public void createAndEditBrowseSegmentTest(Object jsonObject) throws InterruptedException {
        JsonObject segmentData=(JsonObject) jsonObject;
        segmentName=segmentData.get("Segment").getAsString();
        String userTypeNew = segmentData.get("UserTypeNew").getAsString();
        String segmentDeviceType = segmentData.get("SegmentDeviceType").getAsString();
        String userTypeRepeat = segmentData.get("UserTypeRepeat").getAsString();


        ThreadWait();
        searchPageActions.awaitForPageToLoad();
        goTo(browseSegmentPage);
        ThreadWait();

        segmenPageActions.createSegment(segmentName);
        segmenPageActions.selectTypeValues(UnbxdEnum.USERTYPE,userTypeNew);
        segmenPageActions.selectDeviceType(UnbxdEnum.DEVICETYPE,segmentDeviceType);
        segmenPageActions.clickOnSave();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        Assert.assertNotNull(segmenPageActions.segmentRuleByName(segmentName));
        SegmentRules.add(segmentName);

        searchPageActions.selectActionType(UnbxdEnum.EDIT,segmentName);
        ThreadWait();
        segmenPageActions.segmentUserTypeValues.getText().contains(userTypeNew);
        segmenPageActions.segmentDeviceTypeValues.getText().contains(segmentDeviceType);

        segmenPageActions.removeSegmentValues(userTypeNew);
        segmenPageActions.selectTypeValues(UnbxdEnum.USERTYPE,userTypeRepeat);
        segmenPageActions.segmentUserTypeValues.getText().contains(userTypeRepeat);
        segmenPageActions.clickOnSave();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        segmenPageActions.UserTypeinListingPage.getText().contains(userTypeRepeat);

        //Delete
        goTo(segmentPage);
        segmenPageActions.deleteSegmentRule(segmentName);
        searchPage.awaitTillElementDisplayed(searchPageActions.ToasterSuccess);
        searchPage.threadWait();

    }


    @AfterClass(alwaysRun = true,groups={"sanity"})
    public void deleteCreatedRules() throws InterruptedException {

        goTo(segmentPage);
        merchandisingActions.goToSection(UnbxdEnum.SEGMENTS);
        for (String SegmentRule : SegmentRules) {
            if (segmenPageActions.segmentRuleByName(SegmentRule) != null) {
                segmenPageActions.deleteSegmentRule(SegmentRule);
                Assert.assertNull(segmenPageActions.segmentRuleByName(SegmentRule), "CREATED SEGMENT RULE IS NOT DELETED");
                getDriver().navigate().refresh();
                ThreadWait();

            }
        }

        goTo(browseSegmentPage);
        merchandisingActions.goToSection(UnbxdEnum.SEGMENTS);
        for (String SegmentRule : SegmentRules) {
            if (segmenPageActions.segmentRuleByName(SegmentRule) != null) {
                segmenPageActions.deleteSegmentRule(SegmentRule);
                Assert.assertNull(segmenPageActions.segmentRuleByName(SegmentRule), "CREATED SEGMENT RULE IS NOT DELETED");
                getDriver().navigate().refresh();
                ThreadWait();

            }
        }

    }



}




