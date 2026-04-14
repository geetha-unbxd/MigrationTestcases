package UnbxdTests.testNG.consoleui.Collections;

import UnbxdTests.testNG.consoleui.MerchBulkUpload.uploadTest;
import core.consoleui.actions.CollectionsActions;
import lib.compat.Page;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CollectionsuplodFileBrowse extends uploadTest {

    @Page
    CollectionsActions collectionsActions;

    /**
     * Browse URL: upload collections file, then search and delete the created row (same cleanup as
     * {@link CollectionsuploadFileSearch}).
     */
    @Test(priority = 1, description = "Browse Collections: upload, verify, search, delete")
    public void uploadFileToCollections() {
        System.out.println("=== Test: Browse Collections — Upload & Delete ===");
        
        // Step 1: Navigate to Collections page
        System.out.println("Step 1: Navigating to Collections page");
        collectionsActions.navigateToCollectionsBrowse();
        System.out.println("Step 2: Verifying Collections page is loaded");
        collectionsActions.verifyCollectionsPageLoaded();
        System.out.println("Step 3: Clicking on Upload File button");
        collectionsActions.clickUploadFile();
        System.out.println("Step 4: Entering collection name");
        collectionsActions.enterCollectionName();
        String collectionName = collectionsActions.getLastEnteredCollectionName();
        Assert.assertNotNull(collectionName, "Collection name should be set after entering");
        System.out.println("Step 4b: Collection name captured: " + collectionName);
        System.out.println("Step 5: Uploading collections CSV file");
        collectionsActions.uploadCollectionsFile();
        System.out.println("Step 6: Verifying file upload success");
        collectionsActions.verifyFileUploadSuccess();

        System.out.println("Step 7: Searching for collection — delete only if listed");
        collectionsActions.closeUploadModalIfPresent();
        collectionsActions.searchCollectionByName(collectionName);
        if (collectionsActions.isCollectionListedInResults(collectionName, 25)) {
            System.out.println("Step 8: Collection found — deleting");
            collectionsActions.deleteFirstCollection();
        } else {
            System.out.println("Step 8: Collection not listed after search — skipping delete");
        }

        System.out.println("✓ Collections browse upload and delete test completed successfully");
    }
} 