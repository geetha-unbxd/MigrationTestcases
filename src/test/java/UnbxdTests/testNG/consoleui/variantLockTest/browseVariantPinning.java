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

public class browseVariantPinning extends MerchVariantTest {

    public String page;

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


    @FileToTest(value = "/consoleTestData/variantglobalPinning.json")
    @Test(description = "Browse: Creates and verifies the global campaign creation with Variant Pinning for Browse Campaigns", dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile", groups = {"merchandising","sanity"})
    public void browseGlobalVariantPinningTest(Object jsonObject) throws InterruptedException
    {
        JsonObject variantPinningJsonObject = (JsonObject) jsonObject;
        page = variantPinningJsonObject.get("query").getAsString();

        goTo(browsePage);
        createGlobalRulePromotion();
        // In preview: get product count, if > 0, click variant lock on first product, select first variant, and verify
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

        goTo(browsePage);
        searchPage.editGlobalRule();
        // Get the first product to verify UniqueId and VariantId
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

        // Verify status - check for Active first, then Pending Sync
        variantLockAction.verifyActiveOrPendingSyncStatus(page);

    }
    @FileToTest(value = "/consoleTestData/variantPinning.json")
    @Test(description = "Browse: Creates, edits, verifies and deletes the campaign creation with Variant Pinning for Browse Campaigns", dataProviderClass = ResourceLoader.class, dataProvider = "getTestDataFromFile", groups = {"merchandising","sanity"})
    public void browseVariantPinningCreateEditDeleteTest(Object jsonObject) throws InterruptedException
    {
        JsonObject variantPinningJsonObject = (JsonObject) jsonObject;
        page = variantPinningJsonObject.get("query").getAsString()+System.currentTimeMillis();

        goTo(browsePage);
        searchPage.threadWait();
        createPromotion(page,false,false);
        Map<String, Object> campaignData = merchandisingActions.getCampaignData("variantPinning.json");

        searchPageActions.pageRuleDropdown.click();
        searchPageActions.awaitForElementPresence(searchPageActions.BuildPath);
        searchPageActions.BuildPath.click();
        searchPageActions.SelectedCategoryPathDisplay.fill().with(page);
        searchPageActions.categorypathApplyButton.click();
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
        ThreadWait();
        merchandisingActions. publishCampaign();
        merchandisingActions.verifySuccessMessage();
        Assert.assertNotNull(searchPage.queryRuleByName(page));
        pageRules.add(page);

        searchPageActions.selectActionType(UnbxdEnum.EDIT, page);
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

        searchPage.threadWait();
        merchandisingActions.publishCampaign();
        merchandisingActions.verifySuccessMessage();
        goTo(browsePage);
        searchPage.threadWait();
        searchPage.queryRuleByName(page);
        // Verify status in listing page (Active or Pending Sync)
        // Verify status - check for Active first, then Pending Sync
        variantLockAction.verifyActiveOrPendingSyncStatus(page);
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


