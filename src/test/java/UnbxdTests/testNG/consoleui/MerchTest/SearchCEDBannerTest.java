package UnbxdTests.testNG.consoleui.MerchTest;

import UnbxdTests.testNG.dataProvider.ResourceLoader;
import com.google.gson.JsonObject;
import core.consoleui.actions.BannerActions;
import core.consoleui.actions.CommercePageActions;
import core.consoleui.actions.MerchandisingActions;
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

public class SearchCEDBannerTest extends MerchandisingTest {

    List<String> queryRules = new ArrayList<>();
    List<String> pageRules = new ArrayList<>();

    @Page
    MerchandisingActions merchandisingActions;

    @Page
    CommercePageActions searchPageActions;

    @Page
    BannerActions bannerActions;

    @Page
    BrowsePage browsePage;

    public String query;

    public String page;


    @FileToTest(value = "/consoleTestData/htmlBanner.json")
    @Test(description = "Verifies the creation and editing of a search banner with HTML content.",groups="sanity",priority = 1,dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile")
    public void createAndEditSearchHtmlBannerTest(Object jsonObject) throws InterruptedException {
        JsonObject bannerData=(JsonObject) jsonObject;
        query=bannerData.get("query").getAsString();
        String editImg=bannerData.get("editBanner").getAsString();

        goTo(searchPage);
        searchPage.threadWait();
        merchandisingActions.goToSection(UnbxdEnum.BANNER);
        searchPageActions.awaitForPageToLoad();

        //Create the banner rule
        createPromotion(query,true,true);
        String html= bannerData.get("data").getAsString();
        Map<String, Object> campaignData = merchandisingActions.getCampaignData("htmlBanner.json");
        bannerActions.goToQueryRuleBanner();
        searchPageActions.fillQueryRuleData(query,null);
        merchandisingActions.fillCampaignData(campaignData);
        bannerActions.addHtmlBanner(html);
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        Assert.assertNotNull(searchPage.queryRuleByName(query));
        queryRules.add(query);

        // Edit the rule
        goTo(searchPage);
        searchPage.threadWait();
        merchandisingActions.goToSection(UnbxdEnum.BANNER);
        searchPageActions.awaitForPageToLoad();
        searchPageActions.selectActionType(UnbxdEnum.EDIT,query);
        searchPage.threadWait();
        Assert.assertTrue(searchPageActions.htmlPreview.getText().contains(html),"SEARCH:  HTML URL IS NOT SAME AS GIVEN ");

        bannerActions.scrollToElement(bannerActions.imageUrlRadioButton, "Image URL banner tab");
        bannerActions.addImgBanner(editImg);
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        Assert.assertNotNull(searchPage.queryRuleByName(query));
        ThreadWait();
        

        searchPage.threadWait();
        goTo(searchPage);
        searchPage.threadWait();
        merchandisingActions.goToSection(UnbxdEnum.BANNER);
        searchPageActions.deleteQueryRule(query);
        searchPage.awaitTillElementDisplayed(searchPageActions.ToasterSuccess);
        searchPage.threadWait();

    }

    @AfterClass(alwaysRun = true, groups = {"sanity"})
    public void deleteCreatedRules() throws InterruptedException {
        for (String q : new ArrayList<>(queryRules)) {
            deleteSearchQueryRuleIfPresent(q, UnbxdEnum.BANNER);
        }
        deleteSearchQueryRuleIfPresent(query, UnbxdEnum.BANNER);
    }

}
