package UnbxdTests.testNG.consoleui.AlgorithmTest;

import java.util.ArrayList;
import java.util.List;

import lib.compat.Page;
import lib.compat.FluentWebElement;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import core.consoleui.actions.ConceptsActions;
import core.consoleui.page.ConceptsPage;
import UnbxdTests.testNG.ui.BaseTest;
import org.testng.Assert;
import lib.constants.UnbxdErrorConstants;
import lib.Helper;
import core.ui.actions.LoginActions;
import core.consoleui.actions.StopWordAction;
import core.consoleui.page.StemmingPage;
import core.consoleui.actions.ContentActions;
import core.consoleui.actions.SynonymActions;
import lib.EnvironmentConfig;
public class StemmingTest extends BaseTest {

    @Page
    LoginActions loginActions;

    @Page
    ConceptsActions conceptsActions;

    @Page
    StemmingPage stemmingPage;

    @Page
    ContentActions contentActions;

    @Page
    SynonymActions synonymActions;
    
    @Page
    StopWordAction stopWordAction;

    @BeforeClass
    public void setUp() {
        super.setUp();
        EnvironmentConfig.unSetContext();
        EnvironmentConfig.setContext(2, 2);
    }


    @Test(priority = 1,description = "This Test Creates and verifies the  stemword",groups = {"stemwordCreation"})
    public void createStemmingTest() throws InterruptedException
    {
        goTo(stemmingPage);
        contentActions.awaitForPageToLoad();
        String stemKeyWord=contentActions.createStemming();
        Assert.assertTrue(contentActions.checkSuccessMessage(), UnbxdErrorConstants.SUCCESS_MESSAGE_FAILURE);


        FluentWebElement stemword= synonymActions.getKeyWordsByName(stemKeyWord);

        String updatedstemKeyword="editstem"+System.currentTimeMillis();
        contentActions.editKeyword(stemword,updatedstemKeyword,null,null);
        conceptsActions.saveChanges();

        Assert.assertTrue(contentActions.checkSuccessMessage(), UnbxdErrorConstants.SUCCESS_MESSAGE_FAILURE);

        synonymActions.deleteCreatedKeyword(updatedstemKeyword);
        Assert.assertTrue(contentActions.checkSuccessMessage(), UnbxdErrorConstants.SUCCESS_MESSAGE_FAILURE);

        Assert.assertNull(synonymActions.getKeyWordsByName(updatedstemKeyword));
    }


    @AfterClass
    public void tearDown()
    {
        driver.close();
        driver.quit();
        Helper.tearDown();

    }
}
