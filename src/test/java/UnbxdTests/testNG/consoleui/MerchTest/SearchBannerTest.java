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
import static lib.constants.UnbxdErrorConstants.SUCCESS_MESSAGE_FAILURE;

public class SearchBannerTest extends MerchandisingTest {

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




    @FileToTest(value = "/consoleTestData/FieldRuleBanner.json")
    @Test(description = "Verifies the creation and editing of a search banner with a field rule and image URL.", priority = 2, dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile",groups={"sanity"})
    public void createAndEditSearchFieldRuleImageBannerTest(Object jsonObject) throws InterruptedException {
        JsonObject bannerData = (JsonObject) jsonObject;

        goTo(searchPage);
        searchPage.threadWait();
        merchandisingActions.goToSection(UnbxdEnum.BANNER);
        searchPageActions.awaitForPageToLoad();

        //create the rule
        String ImgUrl= bannerData.get("data").getAsString();
        String editImgUrl=bannerData.get("editBanner").getAsString();
        searchPageActions.clickOnAddRule(true);
        searchPageActions.goToFieldRuleBasedBanner();
        bannerActions.goToFieldRuleBanner();
        query=bannerActions.selectFieldRuleAttribute();
        ThreadWait();
        bannerActions.selectFieldRuleAttributeValue();
        // Use robust click handling to avoid click interception issues
        searchPageActions.scrollUntilVisible(searchPageActions.nextButton);
        searchPageActions.waitForElementToBeClickable(searchPageActions.nextButton, "Next Button");
        searchPageActions.clickUsingJS(searchPageActions.nextButton);
        bannerActions.addImgBanner(ImgUrl);
        ThreadWait();
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        Assert.assertNotNull(searchPage.queryRuleByName(query));
        queryRules.add(query);

        // Edit the rule
        searchPageActions.selectActionType(UnbxdEnum.EDIT,query);
        ThreadWait();
        Assert.assertEquals(ImgUrl,searchPageActions.bannerInputImgUrl.getValue(),"SEARCH: IMG URL IS NOT SAME AS GIVEN");
        Assert.assertEquals(ImgUrl,searchPageActions.bannerInputRedirectUrl.getValue(),"SEARCH: REDIRECT URL IS NOT SAME AS GIVEN");
        bannerActions.addHtmlBanner(editImgUrl);
        // Use robust click handling to avoid click interception issues
        merchandisingActions.scrollUntilVisible(merchandisingActions.fieldRulePublishBtn);
        merchandisingActions.waitForElementToBeClickable(merchandisingActions.fieldRulePublishBtn, "Publish Button");
        merchandisingActions.clickUsingJS(merchandisingActions.fieldRulePublishBtn);
        merchandisingActions.verifySuccessMessage();
        searchPageActions.selectActionType(UnbxdEnum.EDIT,query);
        ThreadWait();
        Assert.assertEquals(editImgUrl,searchPageActions.htmlPreview.getText(),"SEARCH: IMG URL IS NOT SAME AS GIVEN");

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
