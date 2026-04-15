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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static core.ui.page.UiBase.ThreadWait;
import static lib.constants.UnbxdErrorConstants.SUCCESS_MESSAGE_FAILURE;

public class BoostTest extends MerchandisingTest {

    String query;

    List<String> queryRules = new ArrayList<>();
    List<String> pageRules = new ArrayList<>();

    @Page
    CommercePageActions searchPageActions;

    @Page
    BrowsePage browsePage;

    public String page;

    @FileToTest(value = "/consoleTestData/boosting.json")
    @Test(description = "SEARCH: Creates and verifies the campaign creation with Boost for Search Campaigns", priority = 1, dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile", groups = {"merchandising","sanity"})
    public void boostingTest(Object jsonObject) throws InterruptedException
    {
        String conditionType = "boost";
        JsonObject boostJsonObject = (JsonObject) jsonObject;
        query = boostJsonObject.get("query").getAsString();

        goTo(searchPage);
        searchPage.threadWait();
        createPromotion(query,false,false);

        JsonArray object = boostJsonObject.get("data").getAsJsonArray();
        Map<String, Object> campaignData = merchandisingActions.getCampaignData("boosting.json");

        // goTo(searchPage);
        merchandisingActions.fillCampaignData(campaignData);
        ThreadWait();
        merchandisingActions.goToLandingPage();
        merchandisingActions.goToSectionInMerchandising(UnbxdEnum.BOOST);

        fillMerchandisingData(object,UnbxdEnum.BOOST,false);
        merchandisingActions.clickOnApplyButton();
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        Assert.assertNotNull(searchPage.queryRuleByName(query));
        queryRules.add(query);

        searchPageActions.selectActionType(UnbxdEnum.EDIT, query);
        ThreadWait();
        String condition = searchPageActions.getConditionTitle();
        int group = searchPageActions.getConditionSize();
        Assert.assertTrue(condition.equalsIgnoreCase(conditionType), "SELECTED CONDITION TYPE IS WRONG!!! SELECTED CONDITION IS : " + conditionType);
        Assert.assertEquals(group, object.size(), "NUMBER OF CONDITION GROUP IS WRONG!!! SELECTED CONDITION GROUP IS : " + group);

        String summaryForAssertions = searchPageActions.getAggregatedPromotionSummaryText();
        for (String selectedAttributeLabel : merchandisingActions.getSelectedAttributeOptionTexts()) {
            Assert.assertTrue(
                summaryForAssertions.contains(selectedAttributeLabel),
                "Promotion summary should include the attribute label chosen in the dropdown: \"" + selectedAttributeLabel + "\". Summary text: " + summaryForAssertions);
        }

        searchPage.scrollToBottom();
        // Use robust click handling to avoid click interception issues
        merchandisingActions.scrollUntilVisible(merchandisingActions.MerchandisingStrategyEditButton);
        merchandisingActions.waitForElementToBeClickable(merchandisingActions.MerchandisingStrategyEditButton, "Edit");
        merchandisingActions.clickUsingJS(merchandisingActions.MerchandisingStrategyEditButton);
        fillMerchandisingData(object,UnbxdEnum.BOOST,true);
        merchandisingActions.clickOnApplyButton();
        searchPage.threadWait();
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();

        goTo(searchPage);
        searchPage.threadWait();
        searchPage.queryRuleByName(query);
        searchPageActions.deleteQueryRule(query);
        searchPage.awaitTillElementDisplayed(searchPageActions.ToasterSuccess);
        searchPage.threadWait();
    }

    @AfterClass(alwaysRun = true, groups = {"sanity"})
    public void deleteCreatedRules() throws InterruptedException {
        for (String q : new ArrayList<>(queryRules)) {
            deleteSearchQueryRuleIfPresent(q);
        }
        deleteSearchQueryRuleIfPresent(query);
    }

}


