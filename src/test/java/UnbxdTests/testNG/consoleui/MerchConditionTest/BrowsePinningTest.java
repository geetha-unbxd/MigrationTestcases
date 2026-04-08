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

public class BrowsePinningTest extends MerchandisingTest {

    List<String> queryRules = new ArrayList<>();

    List<String> pageRules = new ArrayList<>();

    @Page
    CommercePageActions searchPageActions;

    private String query;

    private String pin;
    String conditionType = "Pinned";

    @Page
    BrowsePage browsePage;

    public String page;


//Test cases for browse
@FileToTest(value = "/consoleTestData/browsePinningCED.json")
@Test(description = "BROWSE: This test verifies the creation with Pinning for Search Campaigns", priority = 3, dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile",groups = {"merchandising","sanity"})
public void browsePinningCreateTest(Object jsonObject) throws InterruptedException
{
    JsonObject PinningJsonObject = (JsonObject) jsonObject;
    page = PinningJsonObject.get("page").getAsString();

    goTo(browsePage);
    searchPage.threadWait();
    createBrowsePromotion(page,false,false);

    JsonArray object = PinningJsonObject.get("data").getAsJsonArray();
    String pinningPosition=PinningJsonObject.get("pinningPosition").getAsString();
    Map<String, Object> campaignData = merchandisingActions.getCampaignData("browsePinningCED.json");

    searchPageActions.pageRuleDropdown.click();
    searchPageActions.awaitForElementPresence(searchPageActions.BuildPath);
    searchPageActions.BuildPath.click();
    searchPageActions.SelectedCategoryPathDisplay.fill().with(page);
    searchPageActions.categorypathApplyButton.click();
    merchandisingActions.fillCampaignData(campaignData);
    ThreadWait();
    merchandisingActions.goToLandingPage();
    merchandisingActions.goToSectionInMerchandising(UnbxdEnum.PIN);
    fillPinSortMerchandisingData(object, UnbxdEnum.PIN);
    merchandisingActions.clickOnApplyButton();
    Assert.assertEquals(merchandisingActions.pinnedProductIndex.getText(),pinningPosition,"BROWSE: PRODUCT IS NOT PINNED AT THE GIVEN POSITION");
    Assert.assertTrue(merchandisingActions.pinnedProductText.isDisplayed(),"BROWSE: PINNED TEXT IS NOT PRESENT AT THE GIVEN POSITION");

    merchandisingActions.publishCampaign();
    merchandisingActions.verifySuccessMessage();
    ThreadWait();
    Assert.assertNotNull(searchPage.queryRuleByName(page));
    pageRules.add(page);

    searchPageActions.selectActionType(UnbxdEnum.EDIT, page);
    ThreadWait();
    String condition = searchPageActions.getConditionTitle();
    int group = searchPageActions.getSortPinConditionSize();
    Assert.assertTrue(condition.equalsIgnoreCase(conditionType), "SELECTED CONDITION TYPE IS WRONG!!! SELECTED CONDITION IS : " + conditionType);
    Assert.assertEquals(group, 1, "NUMBER OF CONDITION GROUP IS WRONG!!! SELECTED CONDITION GROOUP IS" + group);


    searchPage.scrollToBottom();
    // Use robust click handling to avoid click interception issues
    merchandisingActions.scrollUntilVisible(merchandisingActions.MerchandisingStrategyEditButton);
    merchandisingActions.waitForElementToBeClickable(merchandisingActions.MerchandisingStrategyEditButton, "Edit");
    merchandisingActions.clickUsingJS(merchandisingActions.MerchandisingStrategyEditButton);
    fillupdatedPinSortMerchandisingData(object, UnbxdEnum.PIN);
    merchandisingActions.clickOnApplyButton();
    searchPage.threadWait();
    merchandisingActions.publishCampaign();
    merchandisingActions.verifySuccessMessage();
    ThreadWait();


    goTo(browsePage);
    searchPage.threadWait();
    searchPage.queryRuleByName(page);
    ThreadWait();
    searchPageActions.deleteQueryRule(page);
    searchPage.awaitTillElementDisplayed(searchPageActions.ToasterSuccess);
    searchPage.threadWait();

}

    @AfterClass(alwaysRun = true, groups = {"sanity"})
    public void deleteCreatedRules() throws InterruptedException {
        for (String p : new ArrayList<>(pageRules)) {
            deleteBrowsePageRuleIfPresent(browsePage, p);
        }
        deleteBrowsePageRuleIfPresent(browsePage, page);
    }

}
