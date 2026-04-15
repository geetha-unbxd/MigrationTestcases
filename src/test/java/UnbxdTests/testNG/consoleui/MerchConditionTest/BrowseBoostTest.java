package UnbxdTests.testNG.consoleui.MerchConditionTest;


import UnbxdTests.testNG.consoleui.MerchTest.MerchandisingTest;
import UnbxdTests.testNG.dataProvider.ResourceLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.consoleui.actions.CommercePageActions;
import core.consoleui.page.BrowsePage;
import lib.annotation.FileToTest;
import lib.enums.UnbxdEnum;
import lib.compat.Page;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static core.ui.page.UiBase.ThreadWait;

public class BrowseBoostTest extends MerchandisingTest {

    String query;

    List<String> queryRules = new ArrayList<>();
    List<String> pageRules = new ArrayList<>();

    @Page
    CommercePageActions searchPageActions;

    @Page
    BrowsePage browsePage;

    public String page;



//TestCases for browse
    @FileToTest(value = "/consoleTestData/browseBoosting.json")
    @Test(description = "BROWSE: Creates and verifies the campaign creation with Boost for Search Campaigns", priority = 1, dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile", groups = {"merchandising","sanity"})
    public void browseBoostingTest(Object jsonObject) throws InterruptedException
    {
        String conditionType = "boost";
        JsonObject boostJsonObject = (JsonObject) jsonObject;
        page = boostJsonObject.get("page").getAsString();

        goTo(browsePage);
        createBrowsePromotion(page,false,false);
        JsonArray object = boostJsonObject.get("data").getAsJsonArray();
        Map<String, Object> campaignData = merchandisingActions.getCampaignData("browseBoosting.json");
        searchPageActions.pageRuleDropdown.click();
        searchPageActions.awaitForElementPresence(searchPageActions.BuildPath);
        searchPageActions.BuildPath.click();
        searchPageActions.SelectedCategoryPathDisplay.fill().with(page);
        searchPageActions.categorypathApplyButton.click();
        merchandisingActions.fillCampaignData(campaignData);
        merchandisingActions.goToLandingPage();
        merchandisingActions.goToSectionInMerchandising(UnbxdEnum.BOOST);

        fillMerchandisingData(object,UnbxdEnum.BOOST,false);
        merchandisingActions.clickOnApplyButton();
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        Assert.assertNotNull(searchPage.queryRuleByName(page));
        pageRules.add(page);

        searchPageActions.selectActionType(UnbxdEnum.EDIT, page);
        String condition = searchPageActions.getConditionTitle();
        int group = searchPageActions.getConditionSize();
        Assert.assertTrue(condition.equalsIgnoreCase(conditionType), "SELECTED CONDITION TYPE IS WRONG!!! SELECTED CONDITION IS : " + conditionType);
        Assert.assertEquals(group, object.size(), "NUMBER OF CONDITION GROUP IS WRONG!!! SELECTED CONDITION GROUP IS : " + group);

        searchPage.scrollToBottom();
        merchandisingActions.scrollUntilVisible(merchandisingActions.MerchandisingStrategyEditButton);
        merchandisingActions.waitForElementToBeClickable(merchandisingActions.MerchandisingStrategyEditButton, "Edit");
        merchandisingActions.clickUsingJS(merchandisingActions.MerchandisingStrategyEditButton);
        fillMerchandisingData(object,UnbxdEnum.BOOST,true);
        merchandisingActions.clickOnApplyButton();
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();


        goTo(browsePage);
        searchPage.queryRuleByName(page);
        searchPageActions.deleteQueryRule(page);
        searchPage.awaitTillElementDisplayed(searchPageActions.ToasterSuccess);

    }


    @AfterClass(alwaysRun = true, groups = {"sanity"})
    public void deleteCreatedRules() throws InterruptedException {
        for (String p : new ArrayList<>(pageRules)) {
            deleteBrowsePageRuleIfPresent(browsePage, p);
        }
        deleteBrowsePageRuleIfPresent(browsePage, page);
    }
}


