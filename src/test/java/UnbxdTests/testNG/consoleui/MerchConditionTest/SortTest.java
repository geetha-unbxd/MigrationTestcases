package UnbxdTests.testNG.consoleui.MerchConditionTest;

import UnbxdTests.testNG.consoleui.MerchTest.MerchandisingTest;
import UnbxdTests.testNG.dataProvider.ResourceLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.consoleui.actions.CommercePageActions;
import lib.annotation.FileToTest;
import lib.enums.UnbxdEnum;
import lib.compat.Page;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static core.ui.page.UiBase.ThreadWait;

public class SortTest extends MerchandisingTest {

    List<String> queryRules = new ArrayList<>();

    @Page
    CommercePageActions searchPageActions;

    private String query;

    List<String> pageRules = new ArrayList<>();

    @Page
    core.consoleui.page.BrowsePage browsePage;

    public String page;


    @FileToTest(value = "/consoleTestData/Sort.json")
    @Test(description = "SEARCH: Creates and verifies the campaign creation with sort for Search Campaigns", priority = 1, dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile", groups = {"merchandising","sanity"})
    public void sortTest(Object jsonObject) throws InterruptedException
    {
        String conditionType = "Sort By";
        JsonObject sortJsonObject = (JsonObject) jsonObject;
        query = sortJsonObject.get("query").getAsString();

        goTo(searchPage);
        searchPage.threadWait();
        createPromotion(query,false,false);

        JsonArray object = sortJsonObject.get("data").getAsJsonArray();
        Map<String, Object> campaignData = merchandisingActions.getCampaignData("sort.json");

        // goTo(searchPage);
        merchandisingActions.fillCampaignData(campaignData);
        ThreadWait();
        merchandisingActions.goToLandingPage();
        merchandisingActions.goToSectionInMerchandising(UnbxdEnum.SORT);
        fillPinSortMerchandisingData(object, UnbxdEnum.SORT);
        merchandisingActions.clickOnApplyButton();
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        Assert.assertNotNull(searchPage.queryRuleByName(query));
        queryRules.add(query);

        searchPageActions.selectActionType(UnbxdEnum.EDIT, query);
        ThreadWait();
        ThreadWait();
        String condition = searchPageActions.getConditionTitle();
        int group = searchPageActions.getSortPinConditionSize();
        Assert.assertTrue(condition.equalsIgnoreCase(conditionType), "SELECTED CONDITION TYPE IS WRONG!!! SELECTED CONDITION IS : " + conditionType);
        Assert.assertEquals(group, 1, "NUMBER OF CONDITION GROUP IS WRONG!!! SELECTED CONDITION GROOUP IS" + group);


        searchPage.scrollToBottom();
        ThreadWait();
        // Use robust click handling to avoid click interception issues
        merchandisingActions.scrollUntilVisible(merchandisingActions.MerchandisingStrategyEditButton);
        merchandisingActions.waitForElementToBeClickable(merchandisingActions.MerchandisingStrategyEditButton, "Edit");
        merchandisingActions.clickUsingJS(merchandisingActions.MerchandisingStrategyEditButton);
        fillupdatedPinSortMerchandisingData(object, UnbxdEnum.SORT);
        merchandisingActions.clickOnApplyButton();
        searchPage.threadWait();
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();


        goTo(searchPage);
        searchPage.threadWait();
        searchPage.queryRuleByName(query);
        ThreadWait();
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
