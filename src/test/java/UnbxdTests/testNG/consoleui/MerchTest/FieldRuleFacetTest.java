package UnbxdTests.testNG.consoleui.MerchTest;

import UnbxdTests.testNG.dataProvider.ResourceLoader;
import com.google.gson.JsonObject;
import core.consoleui.actions.BannerActions;
import core.consoleui.actions.CommercePageActions;
import core.consoleui.actions.FacetActions;
import core.consoleui.actions.MerchandisingActions;
import core.consoleui.page.BrowseFacetsPage;
import core.consoleui.page.BrowsePage;
import core.consoleui.page.CommerceSearchPage;
import core.consoleui.page.searchableFieldsAndFacetsPage;
import core.ui.actions.FacetableFieldsActions;
import core.ui.actions.SearchableFieldActions;
import core.ui.components.FacetComponent;
import core.ui.page.FacetableFieldsPage;
import lib.Helper;
import lib.annotation.FileToTest;
import lib.enums.UnbxdEnum;
import lib.compat.Page;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static core.ui.page.UiBase.ThreadWait;

public class FieldRuleFacetTest extends MerchandisingTest {

    @Page
    MerchandisingActions merchandisingActions;

    @Page
    CommercePageActions searchPageActions;
    @Page
    BannerActions bannerActions;

    @Page
    FacetActions facetActions;

    @Page
    FacetableFieldsActions facetableFieldsActions;

    @Page
    SearchableFieldActions searchableFieldActions;
    @Page
    searchableFieldsAndFacetsPage manageSearchFacetAndSearchFieldPage;

    @Page
    FacetComponent facetComponenet;

    @Page
    BrowsePage browsePage;

    @Page
    CommerceSearchPage commerceSearchPage;
    @Page
    FacetableFieldsPage facetableFieldsPage;
    @Page
    BrowseFacetsPage browseFacetsPage;

    List<String> queryRules = new ArrayList<>();
    List<String> pageRules = new ArrayList<>();

    public String page;

    public String query;
    @FileToTest(value = "/consoleTestData/FieldRuleFacetCE.json")
    @Test(description = "Verifies the creation and editing of a search facet using a field rule.", priority = 1, dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile",groups = {"sanity"})
    public void createAndEditSearchFieldRuleFacetTest(Object jsonObject) throws InterruptedException {
        JsonObject facetData = (JsonObject) jsonObject;

        goTo(searchPage);
        searchPage.threadWait();
        merchandisingActions.goToSection(UnbxdEnum.FACETS);
        searchPageActions.awaitForPageToLoad();
       
        //create the rule
        searchPageActions.clickOnAddRule(true);
        ThreadWait();
        query=bannerActions.selectFieldRuleAttribute();
        bannerActions.selectFieldRuleAttributeValue();
        ThreadWait();
        click(searchPageActions.nextButton);
        String facetDisplayName=facetActions.selectFacetAndGetSelectedFacetName();
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        searchPage.queryRuleByName(query);
        queryRules.add(query);

        // Edit the rule
        searchPageActions.selectActionType(UnbxdEnum.EDIT,query);
        ThreadWait();
        facetActions.searchForField(facetDisplayName);
        Assert.assertTrue(facetActions.activeToggle.get(0).isDisplayed());
        facetActions.disableTheToggle();
        click(merchandisingActions.fieldRulePublishBtn);
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        searchPage.threadWait();
        searchPageActions.selectActionType(UnbxdEnum.EDIT,query);
        ThreadWait();
        facetActions.searchForField(facetDisplayName);
        facetActions.verifydTheToggleIsDisabledOrNot();
        click(merchandisingActions.fieldRulePublishBtn);
        merchandisingActions.verifySuccessMessage();

        goTo(searchPage);
        searchPage.threadWait();
        merchandisingActions.goToSection(UnbxdEnum.FACETS);
        searchPage.queryRuleByName(query);
        searchPageActions.deleteQueryRule(query);
        searchPage.awaitTillElementDisplayed(searchPageActions.ToasterSuccess);
        searchPage.threadWait();

    }


    @FileToTest(value = "/consoleTestData/gobalFieldRule.json")
    @Test(description = "Verifies the filtering functionality for global search facets (text/range, enabled/disabled).", priority = 3,dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile")
    public void verifyGlobalSearchFacetFiltersTest(Object jsonObject) throws InterruptedException {

        JsonObject globalData = (JsonObject) jsonObject;
        String Facet_TypeText = globalData.get("Facet_Type").getAsString();
        String FacetStatusEnabled = globalData.get("Facet_Status").getAsString();
        String Facet_TypeRange = globalData.get("FacetType").getAsString();
        String FacetStatusDisabled = globalData.get("FacetStatus").getAsString();

        goTo(searchPage);
        searchPage.threadWait();
        merchandisingActions.goToSection(UnbxdEnum.FACETS);
        searchPageActions.awaitForPageToLoad();
        createGlobalRulePromotion();

        //Text and enabled
        facetActions.facetFilterIcon();
        facetActions.applyTextAndEnabledFilter();
        facetActions.ApplyFilterButton();
        facetActions.verifyAppliedFilter(Facet_TypeText,FacetStatusEnabled);
        facetActions.verifyTextFilterAndEnabledFilter(Facet_TypeText);

        //Disable filter
        facetActions.facetFilterIcon();
        facetActions.applyTextAndEnabledFilter();
        facetActions.ApplyFilterButton();
        facetActions.verifyAppliedFilterIsNotPresent();

        //range and disabled
        facetActions.facetFilterIcon();
        facetActions.applyRangeAndDisabledFilter();
        facetActions.ApplyFilterButton();
        facetActions.verifyAppliedFilter(Facet_TypeRange,FacetStatusDisabled);
        facetActions.verifyRangeFilterAndDisabledFilter(Facet_TypeRange);
        click(merchandisingActions.fieldRulePublishBtn);
    }


//Test case for browse

    @FileToTest(value = "/consoleTestData/FieldRuleBrowseBanner.json")
    @Test(description = "Verifies the creation and editing of a browse facet using a field rule.", priority = 2, dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile",groups = {"sanity"})
    public void createAndEditBrowseFieldRuleFacetTest(Object jsonObject) throws InterruptedException {
        JsonObject facetData = (JsonObject) jsonObject;

        goTo(browsePage);
        searchPage.threadWait();
        merchandisingActions.goToSection(UnbxdEnum.FACETS);
        searchPageActions.awaitForPageToLoad();

        //create the rule
        searchPageActions.clickOnAddRule(true);
        ThreadWait();
        page=bannerActions.selectFieldRuleAttribute();
        bannerActions.selectFieldRuleAttributeValue();
        click(searchPageActions.nextButton);
        String facetDisplayName=facetActions.selectFacetAndGetSelectedFacetName();
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        Assert.assertNotNull(searchPage.queryRuleByName(page));
        pageRules.add(page);

        // Edit the rule
        searchPageActions.selectActionType(UnbxdEnum.EDIT,page);
        ThreadWait();
        facetActions.searchForField(facetDisplayName);
        Assert.assertTrue(facetActions.activeToggle.get(0).isDisplayed());
        facetActions.disableTheToggle();
        click(merchandisingActions.fieldRulePublishBtn);
        merchandisingActions.verifySuccessMessage();
        ThreadWait();
        searchPage.threadWait();
        searchPageActions.selectActionType(UnbxdEnum.EDIT,page);
        ThreadWait();
        facetActions.searchForField(facetDisplayName);
        facetActions.verifydTheToggleIsDisabledOrNot();
        click(merchandisingActions.fieldRulePublishBtn);
        merchandisingActions.verifySuccessMessage();

        //searchPageActions.fillPageName(page);
        // click(bannerActions.browseFieldRuleMatchCondtion);
        goTo(browsePage);
        searchPage.threadWait();
        merchandisingActions.goToSection(UnbxdEnum.FACETS);
        searchPage.queryRuleByName(page);
        searchPageActions.deleteQueryRule(page);
        searchPage.awaitTillElementDisplayed(searchPageActions.ToasterSuccess);
        searchPage.threadWait();


    }




    @FileToTest(value = "/consoleTestData/gobalFieldRule.json")
    @Test(description = "Verifies the filtering functionality for global browse facets (text/range, enabled/disabled).", priority = 9,dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile")
    public void browseGlobalFacetWithFilterTest(Object jsonObject) throws InterruptedException {

        JsonObject globalData = (JsonObject) jsonObject;
        String Facet_TypeText = globalData.get("Facet_Type").getAsString();
        String FacetStatusEnabled = globalData.get("Facet_Status").getAsString();
        String Facet_TypeRange = globalData.get("FacetType").getAsString();
        String FacetStatusDisabled = globalData.get("FacetStatus").getAsString();

        goTo(browsePage);
        searchPage.threadWait();
        merchandisingActions.goToSection(UnbxdEnum.FACETS);
        searchPageActions.awaitForPageToLoad();
        createGlobalRulePromotion();

        //Text and enabled
        facetActions.facetFilterIcon();
        facetActions.applyTextAndEnabledFilter();
        facetActions.ApplyFilterButton();
        facetActions.verifyAppliedFilter(Facet_TypeText,FacetStatusEnabled);
        facetActions.verifyTextFilterAndEnabledFilter(Facet_TypeText);

        //Disable filter
        facetActions.facetFilterIcon();
        facetActions.applyTextAndEnabledFilter();
        facetActions.ApplyFilterButton();
        facetActions.verifyAppliedFilterIsNotPresent();

        //range and disabled
        facetActions.facetFilterIcon();
        facetActions.applyRangeAndDisabledFilter();
        facetActions.ApplyFilterButton();
        facetActions.verifyAppliedFilter(Facet_TypeRange,FacetStatusDisabled);
        facetActions.verifyRangeFilterAndDisabledFilter(Facet_TypeRange);
        click(merchandisingActions.fieldRulePublishBtn);
    }

    @AfterClass(alwaysRun = true, groups = {"sanity"})
    public void deleteCreatedRules() throws InterruptedException {
        for (String q : new ArrayList<>(queryRules)) {
            deleteSearchQueryRuleIfPresent(q, UnbxdEnum.FACETS);
        }
        for (String p : new ArrayList<>(pageRules)) {
            deleteBrowsePageRuleIfPresent(browsePage, p, UnbxdEnum.FACETS);
        }
    }

}