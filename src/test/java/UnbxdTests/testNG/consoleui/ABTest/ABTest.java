package UnbxdTests.testNG.consoleui.ABTest;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lib.compat.Page;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import UnbxdTests.testNG.consoleui.MerchTest.MerchandisingTest;
import UnbxdTests.testNG.dataProvider.ResourceLoader;
import core.consoleui.actions.ABActions;
import core.consoleui.actions.CommercePageActions;
import core.consoleui.page.BrowsePage;
import static core.ui.page.UiBase.ThreadWait;
import lib.annotation.FileToTest;
import lib.enums.UnbxdEnum;

public class ABTest extends MerchandisingTest {

    String query;

    List<String> queryRules = new ArrayList<>();
    List<String> pageRules = new ArrayList<>();

    @Page
    CommercePageActions searchPageActions;

    @Page
    BrowsePage browsePage;

    @Page
    ABActions abActions;

    public String page;


    @FileToTest(value = "/consoleTestData/ABboost.json")
    @Test(description = "BROWSE AB RULE: Creates and verifies the campaign creation with Boost and AB condition ", priority = 1, dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile", groups = {"merchandising","sanity"})
    public void abCreateAndPreviewTest(Object jsonObject) throws InterruptedException
    {
        String conditionType = "boost";
        JsonObject ABData = (JsonObject) jsonObject;
        page = ABData.get("page").getAsString();
        int VariationA=ABData.get("VariationA").getAsInt();
        int VariationB=ABData.get("VariationB").getAsInt();

        goTo(browsePage);
        searchPage.threadWait();
        createBrowsePromotion(page,false,false);
        JsonArray object = ABData.get("data").getAsJsonArray();
        Map<String, Object> campaignData = merchandisingActions.getCampaignData("ABboost.json");


        searchPageActions.pageRuleDropdown.click();
        searchPageActions.awaitForElementPresence(searchPageActions.BuildPath);
        searchPageActions.BuildPath.click();
        searchPageActions.SelectedCategoryPathDisplay.fill().with(page);
        searchPageActions.categorypathApplyButton.click();
        merchandisingActions.fillCampaignData(campaignData);
        ThreadWait();
        merchandisingActions.goToLandingPage();
        ThreadWait();
        abActions.enableABToggle();
        ThreadWait();
        abActions.fillABPercentage(UnbxdEnum.VARIATIONA, VariationA);
        abActions.scrollToBottomOfRuleContent();
        ThreadWait();
        abActions.fillABPercentage(UnbxdEnum.VARIATIONB, VariationB);
        ThreadWait();
        String winnerDecidedBy = ABData.get("winnerDecidedBy").getAsString();
        abActions.selectWinnerDecidedByabData(winnerDecidedBy);
        String winner=abActions.getWinnerDecidedByValue();
        ThreadWait();
        abActions.selectPreferredOptionABData();
        String PreferredOption=abActions.getPreferredOptionABData();
        click(abActions.abConditionApplyButton);

        merchandisingActions.goToSectionInMerchandising(UnbxdEnum.BOOST);
        fillMerchandisingData(object,UnbxdEnum.BOOST,false);
        merchandisingActions.clickOnApplyButton();

        abActions.selectABConfiguration(UnbxdEnum.VARIATIONB);
        merchandisingActions.goToSectionInMerchandising(UnbxdEnum.BOOST);
        fillMerchandisingData(object,UnbxdEnum.BOOST,false);
        merchandisingActions.clickOnApplyButton();

        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        Assert.assertNotNull(searchPage.queryRuleByName(page));
        pageRules.add(page);
        ThreadWait();

        searchPageActions.selectActionType(UnbxdEnum.PREVIEW, page);
        ThreadWait();
        merchandisingActions.scrollUntilVisible(searchPageActions.conditionTitle);
        ThreadWait();
        String condition = searchPageActions.getConditionTitle();
        int group = searchPageActions.getConditionSize();
        Assert.assertTrue(condition.equalsIgnoreCase(conditionType), "BROWSE: SELECTED CONDITION TYPE IS WRONG!!! SELECTED CONDITION IS : " + conditionType);
        Assert.assertEquals(group, object.size(), "BROWSE: NUMBER OF CONDITION GROUP IS WRONG!!! SELECTED CONDITION GROUP IS : " + group);

        abActions.selectABConfiguration(UnbxdEnum.VARIATIONB);
        Assert.assertTrue(condition.equalsIgnoreCase(conditionType), "BROWSE: SELECTED CONDITION TYPE IS WRONG!!! SELECTED CONDITION IS : " + conditionType);
        Assert.assertEquals(group, object.size(), "BROWSE: NUMBER OF CONDITION GROUP IS WRONG!!! SELECTED CONDITION GROUP IS : " + group);

//        click(abActions.viewMore);
//        Assert.assertTrue(abActions.abPreviewSummary.getText().contains(winner));
//        Assert.assertTrue(abActions.abPreviewSummary.getText().contains(PreferredOption));
//
//        Assert.assertTrue(abActions.percentageVariantASummary.getText().contains(String.valueOf(VariationA)));
//        Assert.assertTrue(abActions.percentageVariantBSummary.getText().contains(String.valueOf(VariationB)));
        searchPage.threadWait();
        goTo(browsePage);
        searchPage.threadWait();
        searchPageActions.deleteQueryRule(page);
        searchPage.awaitTillElementDisplayed(searchPageActions.ToasterSuccess);
        searchPage.threadWait();

    }


    //Search test cases
    @FileToTest(value = "/consoleTestData/ABsearchBoost.json")
    @Test(description = "SEARCH PAGE AB RULE: Creates and verifies the campaign creation with Boost and AB condition ", priority = 1, dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile", groups = {"merchandising","sanity"})
    public void abCreateAndPreviewTestForSearch(Object jsonObject) throws InterruptedException
    {
        String conditionType = "boost";
        JsonObject ABData = (JsonObject) jsonObject;
        query = ABData.get("query").getAsString();
        int VariationA=ABData.get("VariationA").getAsInt();
        int VariationB=ABData.get("VariationB").getAsInt();

        goTo(searchPage);
        searchPage.threadWait();
        createPromotion(query,false,false);
        JsonArray object = ABData.get("data").getAsJsonArray();
        Map<String, Object> campaignData = merchandisingActions.getCampaignData("ABsearchBoost.json");

        merchandisingActions.fillCampaignData(campaignData);
        merchandisingActions.goToLandingPage();

        abActions.enableABToggle();
        ThreadWait();
        abActions.fillABPercentage(UnbxdEnum.VARIATIONA, VariationA);
        abActions.scrollToBottomOfRuleContent();
        ThreadWait();
        abActions.fillABPercentage(UnbxdEnum.VARIATIONB, VariationB);
        String winnerDecidedBy = ABData.get("winnerDecidedBy").getAsString();
        abActions.selectWinnerDecidedByabData(winnerDecidedBy);
        ThreadWait();
        String winner=abActions.getWinnerDecidedByValue();
        ThreadWait();
        abActions.selectPreferredOptionABData();
        String PreferredOption=abActions.getPreferredOptionABData();
        click(abActions.abConditionApplyButton);

        merchandisingActions.goToSectionInMerchandising(UnbxdEnum.BOOST);
        fillMerchandisingData(object,UnbxdEnum.BOOST,false);
        ThreadWait();
        merchandisingActions.clickOnApplyButton();

        abActions.selectABConfiguration(UnbxdEnum.VARIATIONB);
        merchandisingActions.goToSectionInMerchandising(UnbxdEnum.BOOST);
        fillMerchandisingData(object,UnbxdEnum.BOOST,false);
        merchandisingActions.clickOnApplyButton();
        ThreadWait();

        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        Assert.assertNotNull(searchPage.queryRuleByName(query));
        queryRules.add(query);

        searchPageActions.selectActionType(UnbxdEnum.PREVIEW, query);
        ThreadWait();
        merchandisingActions.scrollUntilVisible(searchPageActions.conditionTitle);
        ThreadWait();
        String condition = searchPageActions.getConditionTitle();
        int group = searchPageActions.getConditionSize();
        Assert.assertTrue(condition.equalsIgnoreCase(conditionType), "BROWSE: SELECTED CONDITION TYPE IS WRONG!!! SELECTED CONDITION IS : " + conditionType);
        Assert.assertEquals(group, object.size(), "BROWSE: NUMBER OF CONDITION GROUP IS WRONG!!! SELECTED CONDITION GROUP IS : " + group);

        abActions.selectABConfiguration(UnbxdEnum.VARIATIONB);
        Assert.assertTrue(condition.equalsIgnoreCase(conditionType), "BROWSE: SELECTED CONDITION TYPE IS WRONG!!! SELECTED CONDITION IS : " + conditionType);
        Assert.assertEquals(group, object.size(), "BROWSE: NUMBER OF CONDITION GROUP IS WRONG!!! SELECTED CONDITION GROUP IS : " + group);

//        click(abActions.viewMore);
//        Assert.assertTrue(abActions.abPreviewSummary.getText().contains(winner));
//        Assert.assertTrue(abActions.abPreviewSummary.getText().contains(PreferredOption));
//
//        Assert.assertTrue(abActions.percentageVariantASummary.getText().contains(String.valueOf(VariationA)));
//        Assert.assertTrue(abActions.percentageVariantBSummary.getText().contains(String.valueOf(VariationB)));
        searchPage.threadWait();
        goTo(searchPage);
        searchPage.threadWait();
        searchPageActions.deleteQueryRule(query);
        searchPage.awaitTillElementDisplayed(searchPageActions.ToasterSuccess);
        searchPage.threadWait();

    }

    @AfterClass(alwaysRun = true, groups = {"sanity"})
    public void deleteCreatedRules() throws InterruptedException {
        for (String p : new ArrayList<>(pageRules)) {
            deleteBrowsePageRuleIfPresent(browsePage, p);
        }
        for (String q : new ArrayList<>(queryRules)) {
            deleteSearchQueryRuleIfPresent(q);
        }
        deleteBrowsePageRuleIfPresent(browsePage, page);
        deleteSearchQueryRuleIfPresent(query);
    }

}

