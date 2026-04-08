package UnbxdTests.testNG.consoleui.variantLockTest;


import UnbxdTests.testNG.consoleui.MerchTest.MerchVariantTest;
import UnbxdTests.testNG.dataProvider.ResourceLoader;
import com.google.gson.JsonObject;
import core.consoleui.actions.ABActions;
import core.consoleui.actions.CommercePageActions;
import core.consoleui.actions.VariantLockAction;
import core.consoleui.page.BrowsePage;
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

public class searchVariantPinning extends MerchVariantTest {

    String query;

    List<String> queryRules = new ArrayList<>();
    List<String> pageRules = new ArrayList<>();

    @Page
    CommercePageActions searchPageActions;

    @Page
    BrowsePage browsePage;

    @Page
    VariantLockAction variantLockAction;

    @Page
    ABActions abActions;

    public String page;

    @FileToTest(value = "/consoleTestData/variantPinning.json")
    @Test(description = "Search: Creates and verifies the global campaign creation with Variant Pinning for Search Campaigns", dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile", groups = {"merchandising","sanity"})
    public void searchGlobalVariantPinningTest(Object jsonObject) throws InterruptedException
    {
        JsonObject variantPinningJsonObject = (JsonObject) jsonObject;
        query = variantPinningJsonObject.get("query").getAsString();

        goTo(searchPage);
        createGlobalRulePromotion();
        variantLockAction.waitForProductsToLoad();
        int previewProductCount = variantLockAction.getProductCount();
        System.out.println("Product count in preview: " + previewProductCount);

        if (previewProductCount > 0) {
            FluentWebElement firstProduct = variantLockAction.getFirstProduct();
            String savedVariantId = variantLockAction.selectAndLockFirstVariantFromModal(firstProduct);
            searchPage.threadWait();
            variantLockAction.waitForProductsToLoad();
            firstProduct = variantLockAction.getFirstProduct();
            variantLockAction.verifyVariantIdAndLockInPreview(firstProduct, savedVariantId);
        } else {
            Assert.fail("No products found in preview to test variant lock");
        }
    
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
     
        goTo(searchPage);
        searchPage.editGlobalRule();
        variantLockAction.waitForProductsToLoad();
        // Get a fresh first product reference after page has settled (avoids StaleElementReferenceException)
        FluentWebElement firstProduct = variantLockAction.getFirstProduct();

        // Extract UniqueId and VariantId from summary and verify in preview
        String summaryUniqueId = variantLockAction.getUniqueIdFromSummary();
        String summaryVariantId = variantLockAction.getVariantIdFromSummary();
        // Re-fetch first product before reading preview to avoid stale element after summary load
        firstProduct = variantLockAction.getFirstProduct();
        String previewUniqueId = variantLockAction.getUniqueIdFromPreview(firstProduct);
        String previewVariantId = variantLockAction.getVariantIdFromPreview(firstProduct);

        System.out.println("Summary - UniqueId: " + summaryUniqueId + ", VariantId: " + summaryVariantId);
        System.out.println("Preview - UniqueId: " + previewUniqueId + ", VariantId: " + previewVariantId);

        // Verify UniqueId matches
        Assert.assertFalse(summaryUniqueId.isEmpty(), "UniqueId not found in summary");
        Assert.assertFalse(previewUniqueId.isEmpty(), "UniqueId not found in preview");
        Assert.assertEquals(previewUniqueId, summaryUniqueId, "UniqueId mismatch between summary and preview");

        // Verify VariantId matches
        Assert.assertFalse(summaryVariantId.isEmpty(), "VariantId not found in summary");
        Assert.assertFalse(previewVariantId.isEmpty(), "VariantId not found in preview");
        Assert.assertEquals(previewVariantId, summaryVariantId, "VariantId mismatch between summary and preview");

        System.out.println("UniqueId and VariantId verification passed");
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        ThreadWait();

        // Verify status - check for Active first, then Pending Sync
        variantLockAction.verifyActiveOrPendingSyncStatus(query);

    }



    @FileToTest(value = "/consoleTestData/variantPinning.json")
    @Test(description = "Search: Creates, edits, verifies and deletes the campaign creation with Variant Pinning for Search Campaigns", dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile", groups = {"merchandising","sanity"})
    public void searchVariantPinningCreateEditDeleteTest(Object jsonObject) throws InterruptedException
    {
        JsonObject variantPinningJsonObject = (JsonObject) jsonObject;
        query = variantPinningJsonObject.get("query").getAsString()+System.currentTimeMillis();

        goTo(searchPage);
        createPromotion(query,false,false);
        Map<String, Object> campaignData = merchandisingActions.getCampaignData("variantPinning.json");

        // goTo(searchPage);
        merchandisingActions.fillCampaignData(campaignData);
        merchandisingActions.goToLandingPage();
        variantLockAction.scrollToBottomUntilVariantLockVisible();
        // Click on Lock element
        variantLockAction.variantlock();
        variantLockAction.waitAndScrollToPinningDropdown();
        variantLockAction.selectVariantPinningProduct(0);
        variantLockAction.selectVariantPinningPosition(0);
        merchandisingActions.clickOnApplyButton();

        // Wait for products to be visible and verify first variant is locked
        variantLockAction.waitForProductsToLoad();
        // Get product count
        int productCount = variantLockAction.getProductCount();
        System.out.println("Total products found: " + productCount);
        Assert.assertTrue(productCount > 0, "No products found in preview");
        
        // Verify that the first variant in the first product is locked
        boolean isLocked = variantLockAction.verifyFirstVariantIsLocked();
        Assert.assertTrue(isLocked, "First variant in first product is not locked");
        System.out.println("First variant in first product is locked: " + isLocked);
        merchandisingActions. publishCampaign();
        merchandisingActions.verifySuccessMessage();
        Assert.assertNotNull(searchPage.queryRuleByName(query));
        queryRules.add(query);

        searchPageActions.selectActionType(UnbxdEnum.EDIT, query);
        // Scroll to variant strategy summary
        variantLockAction.waitAndScrollToVariantStrategySummary();
        // Get the first product to verify UniqueId and VariantId
        FluentWebElement firstProduct = variantLockAction.getFirstProduct();

        // Extract UniqueId and VariantId from summary and verify in preview
        String summaryUniqueId = variantLockAction.getUniqueIdFromSummary();
        String summaryVariantId = variantLockAction.getVariantIdFromSummary();
        String previewUniqueId = variantLockAction.getUniqueIdFromPreview(firstProduct);
        String previewVariantId = variantLockAction.getVariantIdFromPreview(firstProduct);

        System.out.println("Summary - UniqueId: " + summaryUniqueId + ", VariantId: " + summaryVariantId);
        System.out.println("Preview - UniqueId: " + previewUniqueId + ", VariantId: " + previewVariantId);

        // Verify UniqueId matches
        Assert.assertFalse(summaryUniqueId.isEmpty(), "UniqueId not found in summary");
        Assert.assertFalse(previewUniqueId.isEmpty(), "UniqueId not found in preview");
        Assert.assertEquals(previewUniqueId, summaryUniqueId, "UniqueId mismatch between summary and preview");

        // Verify VariantId matches
        Assert.assertFalse(summaryVariantId.isEmpty(), "VariantId not found in summary");
        Assert.assertFalse(previewVariantId.isEmpty(), "VariantId not found in preview");
        Assert.assertEquals(previewVariantId, summaryVariantId, "VariantId mismatch between summary and preview");

        System.out.println("UniqueId and VariantId verification passed");
        merchandisingActions.clickUsingJS(merchandisingActions.MerchandisingStrategyEditButton);
        // Scroll to footer button area (Apply variant lock button)
        variantLockAction.waitAndScrollToFooterButtonArea();
        variantLockAction.selectVariantPinningProduct(1);
        variantLockAction.selectVariantPinningPosition(1);
        merchandisingActions.clickOnApplyButton();
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();

        // Verify status in listing page (Active or Pending Sync)
        goTo(searchPage);
        searchPage.awaitForPageToLoad();
        searchPage.queryRuleByName(query);

        // Verify status - check for Active first, then Pending Sync
        variantLockAction.verifyActiveOrPendingSyncStatus(query);
        searchPageActions.deleteQueryRule(query);
        searchPage.awaitTillElementDisplayed(searchPageActions.ToasterSuccess);
    }

    @AfterClass(alwaysRun = true, groups = {"sanity"})
    public void deleteCreatedRules() throws InterruptedException {
        for (String q : new ArrayList<>(queryRules)) {
            deleteSearchQueryRuleIfPresent(q);
        }
        deleteSearchQueryRuleIfPresent(query);
    }

}


